package tw.cute.mcvm;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.nio.charset.StandardCharsets;

public final class DebianImageSource implements VmProvisioner.Image {
    public static final String BASE = "https://cloud.debian.org/images/cloud/trixie/latest/";
    public static final String IMAGE = "debian-13-genericcloud-amd64.qcow2";

    public interface DownloadClient {
        InputStream open(URL url) throws IOException;
    }

    interface Connections {
        HttpURLConnection open(URL url) throws IOException;
    }

    private final DownloadClient client;

    public DebianImageSource() {
        this(DebianImageSource::openOfficial);
    }

    public DebianImageSource(DownloadClient client) {
        this.client = client;
    }

    public void download(Path destination) throws IOException {
        String expected = checksum();
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-512 unavailable", e);
        }
        if (Files.exists(destination)) throw new IOException("Image destination already exists");
        try (InputStream input = client.open(new URL(BASE + IMAGE));
             OutputStream output = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[65536];
            long bytes = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                bytes += read;
                if (bytes > 1024L * 1024 * 1024) throw new IOException("Image exceeds 1 GiB limit");
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
            if (bytes == 0 || !expected.equals(hex(digest.digest()))) {
                throw new IOException("Debian image SHA-512 mismatch");
            }
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(destination);
            throw e;
        }
    }

    private String checksum() throws IOException {
        try (InputStream input = client.open(new URL(BASE + "SHA512SUMS"));
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII))) {
            int total = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                total += line.length();
                if (total > 131072) throw new IOException("Checksum listing too large");
                if (line.endsWith("  " + IMAGE) || line.endsWith(" *" + IMAGE)) {
                    if (line.length() == 130 + IMAGE.length()) {
                        String hash = line.substring(0, 128);
                        if (hash.matches("[0-9a-fA-F]{128}")) return hash.toLowerCase(Locale.ROOT);
                    }
                }
            }
        }
        throw new IOException("Debian image checksum not found");
    }

    private static String hex(byte[] data) {
        char[] alphabet = "0123456789abcdef".toCharArray();
        char[] result = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            result[2 * i] = alphabet[(data[i] & 255) >>> 4];
            result[2 * i + 1] = alphabet[data[i] & 15];
        }
        return new String(result);
    }

    private static InputStream openOfficial(URL url) throws IOException {
        if (!"https".equals(url.getProtocol()) || !"cloud.debian.org".equals(url.getHost())) {
            throw new IOException("Only official Debian HTTPS downloads are supported");
        }
        return openOfficial(url, target -> (HttpURLConnection) target.openConnection());
    }

    static InputStream openOfficial(URL url, Connections connections) throws IOException {
        if (!"https".equals(url.getProtocol()) || !"cloud.debian.org".equals(url.getHost())) {
            throw new IOException("Only official Debian HTTPS downloads are supported");
        }
        for (int redirects = 0; redirects <= 5; redirects++) {
            HttpURLConnection connection = connections.open(url);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(false);
            int status = connection.getResponseCode();
            if (status == 200) return connection.getInputStream();
            if (status != 301 && status != 302 && status != 303 && status != 307 && status != 308) {
                connection.disconnect();
                throw new IOException("Debian download HTTP " + status);
            }
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || redirects == 5) throw new IOException("Debian image redirect invalid or exceeded limit");
            url = new URL(url, location);
            if (!"https".equals(url.getProtocol()) || url.getUserInfo() != null
                    || (url.getPort() != -1 && url.getPort() != 443)) {
                throw new IOException("Debian image redirect must use HTTPS");
            }
        }
        throw new IOException("Debian image redirect exceeded limit");
    }
}
