# 專案任務

- [x] TDD 實作 Paper 啟動時自動建立並啟動 Debian VM（2026-09-30）。
- [x] 核對官方映像 SHA-512 並確保磁碟持久化；失敗時保留資料、下次可重試。
- [x] 提供 Docker 部署範例與本機 Java 8 編譯、13 項測試驗證。
- [ ] 在有 Maven、Docker 的 Linux amd64 主機執行 `mvn test package` 和 `docker compose up --build -d`，確認 QEMU 開機與實際 SSH 登入。
- [x] 新增 Java 8/17/21 測試與打包、Docker 工具檢查、可手動執行的 Debian 開機/SSH/持久化整合驗收；修正 ISO 來源路徑並以 TDD 保護（2026-09-30）。
- [ ] 遠端執行 GitHub Actions 並核對 CI 和手動整合驗收的結果。
