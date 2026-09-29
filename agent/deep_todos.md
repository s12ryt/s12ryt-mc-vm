# 專案任務

- [x] TDD 實作 Paper 啟動時自動建立並啟動 Debian VM（2026-09-30）。
- [x] 核對官方映像 SHA-512 並確保磁碟持久化；失敗時保留資料、下次可重試。
- [x] 提供 Docker 部署範例與本機 Java 8 編譯、16 項測試驗證。
- [x] 在 GitHub Ubuntu 24.04 runner 執行 `mvn verify`、`docker compose up --build -d`，確認 QEMU TCG 開機、實際 SSH 登入與重啟後的磁碟持久性（run 36626895799）。
- [x] 新增 Java 8/17/21 測試與打包、Docker 工具檢查、可手動執行的 Debian 開機/SSH/持久化整合驗收；修正 ISO 來源路徑並以 TDD 保護（2026-09-30）。
- [x] 遠端執行 GitHub Actions 並核對 CI（最終 run 36627708405）和手動整合驗收（最終 run 36627725879）成功；修復遠端揭露的 Maven 舊版相依、Bukkit 測試替身型態與 Debian 302 轉址。升級 checkout/setup-java 至 v5 後再次驗證。
- [x] CI 抽測 Paper 1.8.8、1.12.2、1.16.5、1.18.2、1.21.4 的伺服器及插件啟動；第一次矩陣僅 1.21.4 因新版日誌格式而失敗，TDD 修正後 run 36638398201 五版本與 Java/Docker 工作皆成功。保留獨立的 Debian VM/SSH 整合驗收。
- [x] CI 增測 Paper 1.21.8、1.21.11、26.1.2、26.2，在 Java 21/25 驗證插件啟動；TDD 矩陣測試由缺少新版本而 RED，補齊九個官方穩定 build 後 GREEN；遠端 run 36639611277 的九個 Paper 工作、Java 8/17/21 與 Docker 工作全數成功。
