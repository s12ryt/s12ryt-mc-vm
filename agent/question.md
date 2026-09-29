# 需求確認與驗收

- VM 指的是虛擬機，由 Minecraft Paper 插件作為入口，在伺服器載入後自動建立並啟動。
- 目標是盡量相容不同版本 Paper；只使用傳統 Bukkit API，編譯為 Java 8 bytecode；不承諾未驗證的所有歷史及未來版本。
- Minecraft 在 Linux Docker 容器中執行，無 `/dev/kvm`；接受 QEMU TCG 軟體模擬及其效能代價。
- 從 Debian 官方站下載 Debian 13 amd64 cloud image，驗證官方 SHA-512 清單後才可使用。
- 管理員設定 SSH 公鑰，透過 cloud-init 啟用 debian 使用者；密碼登入停用。
- 首次啟動建立磁碟與 VM，後續重啟沿用原磁碟，已在執行時不重複啟動。
- 安裝在背景進行；設定缺失、校驗失敗、工具不存在或啟動失敗均留下明確錯誤，不得抹掉已有 VM 資料。
- Docker 的 QEMU、ISO 工具由映像預先安裝；插件不要求容器特權、不安裝宿主機套件。

## 驗收方式

1. 測試涵蓋必要 SSH 公鑰、無效資源設定、SHA-512 成功及失敗、暫存檔處置。
2. 測試涵蓋初始化種子、第一次建立/啟動、既有磁碟與種子重用、已執行 VM 不重複啟動、工具失敗不丟資料。
3. 可用本機 Java 工具執行核心測試與 Java 8 編譯；Paper/Docker 真機整合在可用環境再驗證。

## GitHub 與 CI（2026-09-30）

- 建立公開儲存庫 `s12ryt/s12ryt-mc-vm`，將專案推送到 GitHub。
- 一般 push / pull request：Java 多版本測試、Java 8 bytecode JAR 打包、Docker 映像建置與 QEMU 工具煙霧測試。
- 手動觸發整合驗收：在 Linux Docker 裡啟動 Paper 插件，下載官方 Debian 映像、啟動 TCG VM、透過暫時產生的 SSH 密鑰登入，重啟容器後確認磁碟資料保留。
- CI 不提交私鑰或產生的 VM 資料；持續整合失敗要保留診斷資訊，報告未完成的遠端驗收。
