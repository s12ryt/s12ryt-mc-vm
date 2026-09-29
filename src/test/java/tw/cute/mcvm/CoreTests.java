package tw.cute.mcvm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CoreTests {
    private static final String KEY = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPCareQcJ2YijeytR5Vhi4a4IgXA3gldP1038wu9xSq admin@host";
    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        check("settings reject missing key and invalid resources", CoreTests::settingsRejectInvalid);
        check("image verifies Debian checksum", CoreTests::imageVerifiesChecksum);
        check("image refuses mismatched hash and partial data", CoreTests::imageRejectsInvalid);
        check("malformed checksum is reported as IO failure", CoreTests::malformedChecksum);
        check("first boot creates seed, disk and starts TCG", CoreTests::firstBoot);
        check("seed uses absolute files independent of process directory", CoreTests::seedPaths);
        check("second boot preserves disk and seed", CoreTests::secondBoot);
        check("already running VM is not started twice", CoreTests::alreadyRunning);
        check("failed seed command preserves disk for retry", CoreTests::seedFailure);
        check("failed QEMU start preserves disk for retry", CoreTests::startFailure);
        check("system command reports exit and missing tool", CoreTests::systemCommands);
        check("missing or stale VM PID is not running", CoreTests::systemVmState);
        check("Paper entry rejects missing key", CoreTests::pluginRejectsMissingKey);
        check("Paper entry schedules background install", CoreTests::pluginSchedules);
        if (failed > 0) throw new AssertionError(failed + " tests failed");
        System.out.println("Passed " + passed + " tests");
    }

    private static void settingsRejectInvalid() throws Exception {
        expect(IllegalArgumentException.class, () -> new VmSettings("", 1024, 1, 2222));
        expect(IllegalArgumentException.class, () -> new VmSettings(KEY + "\nssh-rsa evil", 1024, 1, 2222));
        expect(IllegalArgumentException.class, () -> new VmSettings("ssh-ed25519 " + zeros(48), 1024, 1, 2222));
        expect(IllegalArgumentException.class, () -> new VmSettings(KEY, 0, 1, 2222));
        expect(IllegalArgumentException.class, () -> new VmSettings(KEY, 1024, 0, 2222));
        expect(IllegalArgumentException.class, () -> new VmSettings(KEY, 1024, 1, 22));
        new VmSettings(KEY, 1024, 1, 2222);
    }

    private static void imageVerifiesChecksum() throws Exception {
        byte[] content = "fake qcow2 bytes".getBytes(StandardCharsets.UTF_8);
        Path path = Files.createTempDirectory("mcvm-hash").resolve("image.qcow2");
        new DebianImageSource(client(content, hash(content) + "  " + DebianImageSource.IMAGE + "\n")).download(path);
        equal("verified image content", content, Files.readAllBytes(path));
    }

    private static void imageRejectsInvalid() throws Exception {
        byte[] content = "bad image".getBytes(StandardCharsets.UTF_8);
        Path path = Files.createTempDirectory("mcvm-bad").resolve("image.qcow2");
        expect(IOException.class, () -> new DebianImageSource(client(content, zeros(128) + "  " + DebianImageSource.IMAGE + "\n")).download(path));
        truth("mismatched image removed", !Files.exists(path));
        expect(IOException.class, () -> new DebianImageSource(client(content, hash(content) + "  other.qcow2\n")).download(path));
        truth("missing checksum image removed", !Files.exists(path));
    }

    private static void malformedChecksum() throws Exception {
        Path path = Files.createTempDirectory("mcvm-short-hash").resolve("image.qcow2");
        expect(IOException.class, () -> new DebianImageSource(client("image".getBytes(StandardCharsets.UTF_8),
                "abc  " + DebianImageSource.IMAGE + "\n")).download(path));
        truth("no malformed image", !Files.exists(path));
    }

    private static void firstBoot() throws Exception {
        Fixture f = new Fixture();
        f.provision();
        equal("one image download", 1, f.downloads);
        equal("seed and VM commands", 2, f.commands.size());
        truth("seed ISO generation", f.commands.get(0).contains("genisoimage"));
        truth("TCG acceleration", f.commands.get(1).contains("tcg"));
        truth("daemonized VM", f.commands.get(1).contains("-daemonize"));
        truth("disk exists", Files.exists(f.dir.resolve("disk.qcow2")));
        String seed = new String(Files.readAllBytes(f.dir.resolve("user-data")), StandardCharsets.UTF_8);
        truth("public key in cloud-init", seed.contains(KEY));
        truth("password SSH disabled", seed.contains("ssh_pwauth: false"));
    }

    private static void secondBoot() throws Exception {
        Fixture f = new Fixture();
        f.provision();
        Files.write(f.dir.resolve("disk.qcow2"), "user files".getBytes(StandardCharsets.UTF_8));
        f.provision();
        equal("no re-download", 1, f.downloads);
        equal("no new ISO", 3, f.commands.size());
        equal("disk unchanged", "user files", new String(Files.readAllBytes(f.dir.resolve("disk.qcow2")), StandardCharsets.UTF_8));
    }

    private static void seedPaths() throws Exception {
        Fixture f = new Fixture();
        f.provision();
        List<String> seed = f.commands.get(0);
        truth("user-data explicitly sourced from VM directory",
                seed.contains("-graft-points") && seed.contains("user-data=" + f.dir.resolve("user-data")));
        truth("meta-data explicitly sourced from VM directory",
                seed.contains("meta-data=" + f.dir.resolve("meta-data")));
    }

    private static void alreadyRunning() throws Exception {
        Fixture f = new Fixture();
        f.provision();
        f.running = true;
        f.provision();
        equal("no second QEMU", 2, f.commands.size());
    }

    private static void seedFailure() throws Exception {
        Fixture f = new Fixture();
        f.failSeed = true;
        expect(IOException.class, f::provision);
        truth("disk retained", Files.exists(f.dir.resolve("disk.qcow2")));
        truth("no incomplete seed", !Files.exists(f.dir.resolve("seed.iso")));
        f.failSeed = false;
        f.provision();
        equal("reuse downloaded disk", 1, f.downloads);
    }

    private static void startFailure() throws Exception {
        Fixture f = new Fixture();
        f.failStart = true;
        expect(IOException.class, f::provision);
        truth("disk preserved on start failure", Files.exists(f.dir.resolve("disk.qcow2")));
        f.failStart = false;
        f.provision();
        equal("no repeated download after start failure", 1, f.downloads);
        equal("only one ISO generation", 3, f.commands.size());
    }

    private static void systemCommands() throws Exception {
        Path dir = Files.createTempDirectory("mcvm-command");
        SystemCommands commands = new SystemCommands(dir);
        String executable = java.nio.file.Paths.get(System.getProperty("java.home"), "bin", "java").toString();
        commands.run(Arrays.asList(executable, "-version"), 10);
        truth("command log created", Files.exists(dir.resolve("commands.log")));
        expect(IOException.class, () -> commands.run(Arrays.asList(executable, "-invalid-mcvm-option"), 10));
        expect(IOException.class, () -> commands.run(Arrays.asList("not-a-real-mcvm-executable-123"), 10));
    }

    private static void systemVmState() throws Exception {
        Path dir = Files.createTempDirectory("mcvm-state");
        Path pid = dir.resolve("vm.pid");
        Path disk = dir.resolve("disk.qcow2");
        SystemVmState state = new SystemVmState();
        truth("no PID is not running", !state.isRunning(pid, disk));
        Files.write(pid, "not a PID\n".getBytes(StandardCharsets.UTF_8));
        truth("malformed PID is not running", !state.isRunning(pid, disk));
    }

    private static void pluginRejectsMissingKey() {
        McVmPlugin plugin = new McVmPlugin();
        plugin.onEnable();
        truth("no job without key", plugin.pendingTask() == null);
    }

    private static void pluginSchedules() {
        McVmPlugin plugin = new McVmPlugin();
        plugin.getConfig().key = KEY;
        plugin.onEnable();
        truth("async job scheduled", plugin.pendingTask() != null);
    }

    private static DebianImageSource.DownloadClient client(byte[] image, String sums) {
        return new DebianImageSource.DownloadClient() {
            public InputStream open(URL url) {
                return new ByteArrayInputStream(url.toString().endsWith("SHA512SUMS")
                        ? sums.getBytes(StandardCharsets.US_ASCII) : image);
            }
        };
    }

    private static String hash(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        StringBuilder hex = new StringBuilder();
        for (byte b : digest.digest(bytes)) {
            hex.append(String.format("%02x", b & 255));
        }
        return hex.toString();
    }

    private static String zeros(int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, '0');
        return new String(chars);
    }

    private static final class Fixture {
        final Path dir;
        final List<List<String>> commands = new ArrayList<List<String>>();
        int downloads;
        boolean running;
        boolean failSeed;
        boolean failStart;

        Fixture() throws IOException {
            dir = Files.createTempDirectory("mcvm-provision");
        }

        void provision() throws IOException {
            VmProvisioner vm = new VmProvisioner(dir, new VmSettings(KEY, 1024, 1, 2222), destination -> {
                downloads++;
                Files.write(destination, "pristine".getBytes(StandardCharsets.UTF_8));
            }, (command, timeout) -> {
                commands.add(new ArrayList<String>(command));
                if (command.contains("genisoimage")) {
                    if (failSeed) throw new IOException("ISO generation failed");
                    Files.write(dir.resolve("seed.iso.tmp"), "iso".getBytes(StandardCharsets.UTF_8));
                }
                if (command.contains("qemu-system-x86_64") && failStart) {
                    throw new IOException("QEMU failed");
                }
            }, (pid, disk) -> running);
            vm.provision();
        }
    }

    private interface Throwing {
        void run() throws Exception;
    }

    private static void check(String name, Throwing test) throws Exception {
        try {
            test.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Throwable failure) {
            System.err.println("FAIL " + name + ": " + failure);
            failed++;
        }
    }

    private static void expect(Class<? extends Throwable> type, Throwing call) throws Exception {
        try {
            call.run();
        } catch (Throwable thrown) {
            if (type.isInstance(thrown)) return;
            throw new AssertionError("Expected " + type + ", got " + thrown, thrown);
        }
        throw new AssertionError("Expected " + type.getName());
    }

    private static void equal(String message, Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError(message + ": " + expected + " != " + actual);
    }

    private static void equal(String message, byte[] expected, byte[] actual) {
        if (!Arrays.equals(expected, actual)) throw new AssertionError(message);
    }

    private static void truth(String message, boolean condition) {
        if (!condition) throw new AssertionError(message);
    }
}
