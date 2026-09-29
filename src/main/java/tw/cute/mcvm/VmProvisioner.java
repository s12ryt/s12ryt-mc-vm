package tw.cute.mcvm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Arrays;

public final class VmProvisioner {
    public interface Image {
        void download(Path destination) throws IOException;
    }

    public interface Commands {
        void run(List<String> command, int timeoutSeconds) throws IOException;
    }

    public interface State {
        boolean isRunning(Path pidFile, Path disk) throws IOException;
    }

    private final Path directory;
    private final VmSettings settings;
    private final Image image;
    private final Commands commands;
    private final State state;

    public VmProvisioner(Path directory, VmSettings settings, Image image, Commands commands, State state) {
        this.directory = directory.toAbsolutePath();
        this.settings = settings;
        this.image = image;
        this.commands = commands;
        this.state = state;
    }

    public void provision() throws IOException {
        Files.createDirectories(directory);
        Path disk = directory.resolve("disk.qcow2");
        Path pidFile = directory.resolve("vm.pid");
        if (state.isRunning(pidFile, disk)) return;
        if (Files.isSymbolicLink(disk)) throw new IOException("VM disk must not be a symbolic link");
        if (!Files.exists(disk)) {
            Path partial = directory.resolve("disk.qcow2.tmp");
            Files.deleteIfExists(partial);
            try {
                image.download(partial);
                Files.move(partial, disk, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(partial);
            }
        }
        if (Files.size(disk) == 0) throw new IOException("VM disk is empty");

        Path seed = directory.resolve("seed.iso");
        if (!Files.exists(seed)) createSeed(seed);
        if (Files.size(seed) == 0) throw new IOException("Cloud-init ISO is empty");
        Files.deleteIfExists(pidFile);
        commands.run(Arrays.asList("qemu-system-x86_64", "-name", "mcvm-debian",
                "-accel", "tcg", "-m", String.valueOf(settings.memoryMb),
                "-smp", String.valueOf(settings.cpus), "-display", "none",
                "-serial", "file:" + directory.resolve("serial.log"),
                "-monitor", "none", "-drive", "file=" + disk + ",format=qcow2,if=virtio",
                "-drive", "file=" + seed + ",media=cdrom,readonly=on",
                "-nic", "user,model=virtio-net-pci,hostfwd=tcp:0.0.0.0:" + settings.sshPort + "-:22",
                "-pidfile", pidFile.toString(), "-daemonize"), 120);
    }

    private void createSeed(Path seed) throws IOException {
        Path userData = directory.resolve("user-data");
        Path metaData = directory.resolve("meta-data");
        Path partial = directory.resolve("seed.iso.tmp");
        Files.write(userData, ("#cloud-config\nssh_pwauth: false\ndisable_root: true\nusers:\n"
                + "  - name: debian\n    groups: sudo\n    sudo: ALL=(ALL) NOPASSWD:ALL\n"
                + "    shell: /bin/bash\n    ssh_authorized_keys:\n      - '"
                + settings.sshKey + "'\n").getBytes(StandardCharsets.UTF_8));
        Files.write(metaData, "instance-id: mcvm-debian-1\nlocal-hostname: mcvm-debian\n"
                .getBytes(StandardCharsets.UTF_8));
        Files.deleteIfExists(partial);
        try {
            commands.run(Arrays.asList("genisoimage", "-quiet", "-output", partial.toString(),
                    "-volid", "cidata", "-joliet", "-rock", "-graft-points",
                    "user-data=" + userData, "meta-data=" + metaData), 120);
            if (!Files.exists(partial) || Files.size(partial) == 0) {
                throw new IOException("genisoimage did not create a seed ISO");
            }
            Files.move(partial, seed, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(partial);
        }
    }
}
