package com.deepblue.etfnav;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public class DailyUpdateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        final PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                EtfStore store = new EtfStore(context);
                List<String> codes = store.getCodes();
                int ok = 0;
                int fail = 0;
                StringBuilder failed = new StringBuilder();
                for (String code : codes) {
                    EtfItem item = EtfApi.fetchDirect(code);
                    if (item.hasError()) {
                        fail++;
                        if (failed.length() < 60) failed.append(code).append(" ");
                    } else {
                        ok++;
                        store.saveItem(item);
                    }
                }
                if (!codes.isEmpty()) {
                    String msg = "成功 " + ok + " 檔，失敗 " + fail + " 檔";
                    if (fail > 0) msg += "：" + failed.toString().trim();
                    NotificationHelper.notify(context, "ETF 淨值每日更新", msg);
                }
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
