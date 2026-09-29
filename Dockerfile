FROM itzg/minecraft-server:java21

RUN apt-get update && apt-get install -y --no-install-recommends \
    qemu-system-x86 qemu-utils genisoimage ca-certificates \
    && rm -rf /var/lib/apt/lists/*

COPY target/mc-vm-1.0.0.jar /plugins/mc-vm.jar
