# ETF NAV Tracker v1.0.1

Android App，可自行輸入 ETF 代碼，透過你的後端 API 查詢淨值、市價、折溢價，並在本機保存自選清單。

## GitHub Actions 編譯重點

GitHub 只會偵測 repo 根目錄底下的：

```text
.github/workflows/android-build.yml
```

正確根目錄應該長這樣：

```text
ETF-app/
├── .github/workflows/android-build.yml
├── app/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

不要變成這樣：

```text
ETF-app/
└── etf_nav_app_github_v1_0_1/
    ├── .github/workflows/android-build.yml
    ├── app/
    └── build.gradle.kts
```

如果 Actions 頁面顯示「Get started with GitHub Actions」，代表 `.github/workflows/android-build.yml` 不在 repo 根目錄，或還沒有 commit 到目前分支。

## API 格式

App 會呼叫：

```text
GET /api/etf/latest?code=0056
```

期待回傳：

```json
{
  "success": true,
  "code": "0056",
  "name": "元大高股息",
  "market_price": "38.20",
  "nav": "38.05",
  "premium_discount": "+0.39%",
  "previous_nav": "37.98",
  "data_date": "20260701",
  "data_time": "14:35:00",
  "source": "backend"
}
```
