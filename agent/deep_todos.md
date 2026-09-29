# 專案任務

- [x] TDD 實作 Paper 啟動時自動建立並啟動 Debian VM（2026-09-30）。
- [x] 核對官方映像 SHA-512 並確保磁碟持久化；失敗時保留資料、下次可重試。
- [x] 提供 Docker 部署範例與本機 Java 8 編譯、16 項測試驗證。
- [x] 在 GitHub Ubuntu 24.04 runner 執行 `mvn verify`、`docker compose up --build -d`，確認 QEMU TCG 開機、實際 SSH 登入與重啟後的磁碟持久性（run 36626895799）。
- [x] 新增 Java 8/17/21 測試與打包、Docker 工具檢查、可手動執行的 Debian 開機/SSH/持久化整合驗收；修正 ISO 來源路徑並以 TDD 保護（2026-09-30）。
- [x] 遠端執行 GitHub Actions 並核對 CI（最終 run 36627708405）和手動整合驗收（最終 run 36627725879）成功；修復遠端揭露的 Maven 舊版相依、Bukkit 測試替身型態與 Debian 302 轉址。升級 checkout/setup-java 至 v5 後再次驗證。
