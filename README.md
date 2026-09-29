# MC VM

Paper 插件在伺服器載入後，於背景下載 Debian 13 amd64 官方 genericcloud 映像，核對 `SHA512SUMS`，建立 cloud-init SSH 公鑰設定，並以 QEMU `tcg` 在無 KVM 的 Linux Docker 容器內啟動 VM。後續啟動沿用 `/data/plugins/McVm/vm/disk.qcow2`，不會重建已有磁碟。

## 部署

1. 在有 Maven、Docker Compose 的 Linux amd64 主機執行 `mvn test package`，再執行 `docker compose up --build -d`。範例的 Paper 是 1.21.4，Java 21；插件本身編譯為 Java 8 bytecode，未宣稱每一個 Paper 版本都已實測。
2. 初次開服會產生 `data/plugins/McVm/config.yml`；將 `ssh-public-key` 改為單一 `ssh-ed25519` 或 `ssh-rsa` 公鑰，重啟 Minecraft 服務。不要填私鑰。
3. 查閱 Minecraft 容器日誌，首次會下載數百 MB 映像；成功後在 Docker 主機上執行 `ssh -p 2222 debian@127.0.0.1`，使用對應私鑰。雲端初始化及 SSH 啟動可能還需數分鐘。QEMU 序列輸出存於 `data/plugins/McVm/vm/serial.log`，工具輸出存於同目錄 `commands.log`。

SSH 在 Docker 容器內監聽 2222，Compose 僅將其公開到主機回環介面；如需遠端連入，建議先透過 SSH tunnel。請持續備份 `data`，尤其 `disk.qcow2`。變更公鑰不會覆寫已初始化的 VM；要輪換密鑰請在 Debian 客體內更新 `authorized_keys`。`vm/user-data` 含公鑰及免密碼 sudo 使用者設定，避免對外公開。

此 Docker 範例不需要 `privileged` 或 `/dev/kvm`，但 TCG 效能有限，與 Minecraft 共用 CPU/記憶體。VM 執行中停止容器會終止 QEMU；正式環境應評估突然停止造成的檔案系統風險並確保可恢復備份。下載完整性仰賴官方站 TLS 及其 SHA-512 清單（未驗證 Debian 簽章）；`latest` 來源會隨 Debian 發行更新，僅影響全新磁碟。

## 持續整合

推送與 pull request 會在 Java 8、17、21 上執行 `mvn verify`，確認插件 JAR 是 Java 8 bytecode、含 `plugin.yml`、不含 Bukkit 測試替身，再建置 Docker 映像並檢查 QEMU 與 ISO 工具。

GitHub Actions 的 **Debian VM integration** 可手動執行。它在臨時 Linux runner 上產生專用 SSH 密鑰、建立 Paper 容器、下載並校驗 Debian 映像、透過 SSH 確認 VM 已開機，然後重啟 Minecraft 容器並確認客體內的檔案仍在。此工作可能需要數十分鐘，依賴 Debian 與 Paper 的上游下載；失敗時 Actions 日誌會輸出容器、QEMU 診斷。不會提交私鑰或 VM 資料，亦不會將 Docker 映像自動發佈到 registry。
