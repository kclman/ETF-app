package com.deepblue.etfnav;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.List;

public class MainActivity extends Activity {
    private EtfStore store;
    private LinearLayout listBox;
    private EditText codeInput;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new EtfStore(this);
        Scheduler.scheduleDaily(this);
        requestNotificationPermissionIfNeeded();
        buildUi();
        refreshList(false);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.rgb(246, 247, 249));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("ETF 淨值追蹤");
        title.setTextSize(26);
        title.setTextColor(Color.rgb(32, 33, 36));
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(0, dp(6), 0, dp(14));
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText("輸入 ETF 代碼後加入自選清單。App 會直接連 TWSE 即時報價與 ETF 淨值揭露頁，不再需要 API URL。每天 18:30 會嘗試更新一次。");
        hint.setTextSize(14);
        hint.setTextColor(Color.rgb(95, 99, 104));
        hint.setPadding(0, 0, 0, dp(12));
        root.addView(hint);

        LinearLayout addRow = new LinearLayout(this);
        addRow.setOrientation(LinearLayout.HORIZONTAL);
        addRow.setPadding(0, dp(12), 0, dp(8));
        root.addView(addRow);

        codeInput = new EditText(this);
        codeInput.setSingleLine(true);
        codeInput.setHint("0056 / 00878 / 00981A");
        codeInput.setTextSize(16);
        addRow.addView(codeInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button addButton = new Button(this);
        addButton.setText("加入");
        addButton.setOnClickListener(v -> addCode());
        addRow.addView(addButton, new LinearLayout.LayoutParams(dp(92), LinearLayout.LayoutParams.WRAP_CONTENT));

        Button refreshAll = new Button(this);
        refreshAll.setText("立即更新所有 ETF");
        refreshAll.setOnClickListener(v -> refreshAllFromApi());
        root.addView(refreshAll, matchWrap());

        statusText = new TextView(this);
        statusText.setTextSize(14);
        statusText.setTextColor(Color.rgb(95, 99, 104));
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText);

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        setContentView(scroll);
    }

    private void addCode() {
        String code = EtfApi.cleanCode(codeInput.getText().toString());
        if (!EtfApi.isValidCode(code)) {
            toast("ETF 代碼格式錯誤");
            return;
        }
        store.addCode(code);
        codeInput.setText("");
        refreshList(false);
        fetchOne(code, true);
    }

    private void refreshList(boolean loading) {
        listBox.removeAllViews();
        List<String> codes = store.getCodes();
        statusText.setText(loading ? "更新中..." : "自選 ETF：" + codes.size() + " 檔");
        if (codes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("目前還沒有自選 ETF。輸入代碼加進來，不然這 App 只能安靜地凝視你。");
            empty.setTextSize(15);
            empty.setTextColor(Color.rgb(95, 99, 104));
            empty.setPadding(0, dp(22), 0, dp(22));
            listBox.addView(empty);
            return;
        }
        for (String code : codes) {
            EtfItem item = store.getItem(code);
            listBox.addView(cardFor(code, item));
        }
    }

    private View cardFor(String code, EtfItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(lp);

        TextView head = new TextView(this);
        String name = item == null || item.name == null || item.name.isEmpty() ? "尚無名稱" : item.name;
        head.setText(code + "  " + name);
        head.setTextSize(19);
        head.setTextColor(Color.rgb(32, 33, 36));
        card.addView(head);

        TextView body = new TextView(this);
        body.setTextSize(15);
        body.setTextColor(Color.rgb(60, 64, 67));
        body.setPadding(0, dp(8), 0, dp(8));
        if (item == null) {
            body.setText("尚未更新。按更新，讓它自己去 TWSE 找資料，而不是當桌面裝飾。");
        } else if (item.hasError()) {
            body.setText("錯誤：" + item.error);
        } else {
            body.setText(
                    "市價：" + blank(item.marketPrice) + "\n" +
                    "預估淨值：" + blank(item.nav) + "\n" +
                    "折溢價：" + blank(item.premiumDiscount) + "\n" +
                    "前日淨值：" + blank(item.previousNav) + "\n" +
                    "更新：" + blank(item.dataDate) + " " + blank(item.dataTime) + "\n" +
                    "來源：" + blank(item.source)
            );
        }
        card.addView(body);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        Button update = new Button(this);
        update.setText("更新");
        update.setOnClickListener(v -> fetchOne(code, true));
        row.addView(update, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button remove = new Button(this);
        remove.setText("刪除");
        remove.setOnClickListener(v -> {
            store.removeCode(code);
            refreshList(false);
        });
        row.addView(remove, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(row);
        return card;
    }

    private void fetchOne(String code, boolean showToast) {
        statusText.setText("更新 " + code + " 中...");
        new Thread(() -> {
            EtfItem item = EtfApi.fetchDirect(code);
            runOnUiThread(() -> {
                if (!item.hasError() && (item.nav == null || item.nav.isEmpty())) {
                    statusText.setText("更新 " + code + " 中...嘗試讀取 TWSE 淨值揭露頁");
                    fetchNavViaWebView(code, item, showToast, 0);
                } else {
                    finishFetch(code, item, showToast);
                }
            });
        }).start();
    }

    private void finishFetch(String code, EtfItem item, boolean showToast) {
        if (!item.hasError()) store.saveItem(item);
        refreshList(false);
        if (showToast) toast(item.hasError() ? item.error : code + " 更新完成");
    }

    private void fetchNavViaWebView(String code, EtfItem base, boolean showToast, int step) {
        final String[] urls = new String[] {
                "https://mis.twse.com.tw/stock/various-areas/etf-price/indicator-disclosure-etf?lang=zhHant",
                "https://mis.twse.com.tw/stock/various-areas/etf-price/value-disclosure-etf?lang=zhHant"
        };
        if (step >= urls.length) {
            base.source = base.source + "；預估淨值頁未解析";
            finishFetch(code, base, showToast);
            return;
        }

        WebView webView = new WebView(this);
        webView.setVisibility(View.GONE);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString("Mozilla/5.0 (Linux; Android) ETFNavTracker/1.0.2");

        webView.setWebViewClient(new WebViewClient() {
            private boolean handled = false;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (handled) return;
                handled = true;
                view.postDelayed(() -> view.evaluateJavascript(
                        "(function(){return document.body ? document.body.innerText : '';})()",
                        value -> {
                            EtfItem parsed = null;
                            try {
                                String text = new org.json.JSONArray("[" + value + "]").getString(0);
                                parsed = EtfApi.parseTextForNav(text, code, base);
                            } catch (Exception ignored) {}

                            try { view.destroy(); } catch (Exception ignored) {}

                            if (parsed != null && parsed.nav != null && !parsed.nav.isEmpty()) {
                                parsed.source = "TWSE 淨值揭露專區 WebView";
                                finishFetch(code, parsed, showToast);
                            } else {
                                fetchNavViaWebView(code, base, showToast, step + 1);
                            }
                        }), 5000);
            }
        });
        webView.loadUrl(urls[step]);
    }

    private void refreshAllFromApi() {
        List<String> codes = store.getCodes();
        if (codes.isEmpty()) {
            toast("沒有自選 ETF");
            return;
        }
        refreshList(true);
        new Thread(() -> {
            int ok = 0;
            int fail = 0;
            for (String code : codes) {
                EtfItem item = EtfApi.fetchDirect(code);
                if (item.hasError()) fail++; else { ok++; store.saveItem(item); }
            }
            int finalOk = ok;
            int finalFail = fail;
            runOnUiThread(() -> {
                refreshList(false);
                toast("完成：成功 " + finalOk + "，失敗 " + finalFail);
            });
        }).start();
    }

    private String blank(String s) {
        return s == null || s.isEmpty() ? "--" : s;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 33);
        }
    }
}
