# 操作紀錄

- 2026-09-30：讀取工作區，原本為空；建立需求確認、待辦及項目表。查閱 Paper plugin.yml/onEnable、Debian 官方 cloud image 與 SHA512SUMS、cloud-init NoCloud、QEMU 與 Docker Minecraft image 文件。確認本機 Java 25/javac 可用，Docker、Maven、Gradle 不可用。
- 2026-09-30：新增 Java 8 核心、Paper 入口、plugin.yml/config.yml、Maven、Dockerfile/Compose、README 與測試替身。TDD RED：設定未拒絕、公用流程未實作（7 項）、短 checksum 錯誤型別、SystemCommands/SystemVmState 未實作、Paper 入口未實作、無效 SSH blob 被接受。各輪 GREEN：`javac --release 8` 與 `java -cp target tw.cute.mcvm.CoreTests` 最終通過 13 項。
- 2026-09-30：LSP 因本機沒有 jdtls 無法執行；`Get-Command mvn,docker` 無結果，無法產製正式 JAR 或真實啟動 Docker/QEMU/SSH，已列入剩餘驗收。
- 2026-09-30：用戶確認公開儲存庫 `s12ryt/s12ryt-mc-vm` 及手動 Debian VM 開機/SSH 驗收。新增 `agent/question.md` 驗收條件。TDD RED：新增 ISO 輸入絕對路徑測試，13 通過 1 失敗；GREEN：`VmProvisioner` 改以 `-graft-points` 指定絕對路徑，14 項全通過。新增 GitHub Java/Docker 與手動整合 workflow、忽略 `data/`，`actionlint` 無錯誤。本機無 Maven/Docker，等待遠端實測。
