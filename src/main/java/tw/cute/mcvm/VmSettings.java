package tw.cute.mcvm;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;

public final class VmSettings {
    private static final Pattern PUBLIC_KEY = Pattern.compile(
            "^ssh-(?:ed25519|rsa) [A-Za-z0-9+/]{40,}={0,3}(?: [A-Za-z0-9@._-]+)?$");
    public final String sshKey;
    public final int memoryMb;
    public final int cpus;
    public final int sshPort;

    public VmSettings(String sshKey, int memoryMb, int cpus, int sshPort) {
        if (sshKey == null || !PUBLIC_KEY.matcher(sshKey.trim()).matches()) {
            throw new IllegalArgumentException("A single ssh-ed25519 or ssh-rsa public key is required");
        }
        String[] fields = sshKey.trim().split(" ", 3);
        try {
            byte[] blob = Base64.getDecoder().decode(fields[1]);
            if (blob.length < 16) throw new IllegalArgumentException("Invalid SSH public key data");
            int length = ((blob[0] & 255) << 24) | ((blob[1] & 255) << 16)
                    | ((blob[2] & 255) << 8) | (blob[3] & 255);
            if (length < 1 || length > blob.length - 4
                    || !fields[0].equals(new String(blob, 4, length, StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("Invalid SSH public key data");
            }
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid SSH public key data", invalid);
        }
        if (memoryMb < 512 || memoryMb > 65536 || cpus < 1 || cpus > 16
                || sshPort < 1024 || sshPort > 65535) {
            throw new IllegalArgumentException("Invalid VM memory, CPU count or SSH port");
        }
        this.sshKey = sshKey.trim();
        this.memoryMb = memoryMb;
        this.cpus = cpus;
        this.sshPort = sshPort;
    }
}
