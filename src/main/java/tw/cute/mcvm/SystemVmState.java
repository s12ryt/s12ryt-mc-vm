package tw.cute.mcvm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SystemVmState implements VmProvisioner.State {
    public boolean isRunning(Path pidFile, Path disk) throws IOException {
        if (!Files.exists(pidFile)) return false;
        String pid = new String(Files.readAllBytes(pidFile), StandardCharsets.US_ASCII).trim();
        if (!pid.matches("[1-9][0-9]{0,9}")) return false;
        Path cmdline = Paths.get("/proc", pid, "cmdline");
        if (!Files.exists(cmdline)) return false;
        String[] args = new String(Files.readAllBytes(cmdline), StandardCharsets.UTF_8).split("\u0000");
        if (args.length == 0 || !args[0].endsWith("qemu-system-x86_64")) return false;
        for (String arg : args) {
            if (arg.contains("file=" + disk.toAbsolutePath() + ",format=qcow2")) return true;
        }
        return false;
    }
}
