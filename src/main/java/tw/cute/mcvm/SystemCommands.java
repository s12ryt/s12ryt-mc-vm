package tw.cute.mcvm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class SystemCommands implements VmProvisioner.Commands {
    private final Path directory;

    public SystemCommands(Path directory) {
        this.directory = directory;
    }

    public void run(List<String> command, int timeoutSeconds) throws IOException {
        Files.createDirectories(directory);
        Path log = directory.resolve("commands.log");
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log.toFile()))
                .start();
        try {
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException(command.get(0) + " timed out; see " + log);
            }
            if (process.exitValue() != 0) {
                throw new IOException(command.get(0) + " exited " + process.exitValue() + "; see " + log);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException(command.get(0) + " interrupted", e);
        }
    }
}
