package com.deepblue.etfnav;

import org.json.JSONException;
import org.json.JSONObject;

public class EtfItem {
    public String code = "";
    public String name = "";
    public String marketPrice = "";
    public String nav = "";
    public String premiumDiscount = "";
    public String previousNav = "";
    public String dataDate = "";
    public String dataTime = "";
    public String source = "";
    public String error = "";

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("code", code);
        o.put("name", name);
        o.put("market_price", marketPrice);
        o.put("nav", nav);
        o.put("premium_discount", premiumDiscount);
        o.put("previous_nav", previousNav);
        o.put("data_date", dataDate);
        o.put("data_time", dataTime);
        o.put("source", source);
        o.put("error", error);
        return o;
    }

    public static EtfItem fromJson(JSONObject o) {
        EtfItem item = new EtfItem();
        item.code = opt(o, "code", opt(o, "a", ""));
        item.name = opt(o, "name", opt(o, "b", ""));
        item.marketPrice = opt(o, "market_price", opt(o, "marketPrice", opt(o, "e", "")));
        item.nav = opt(o, "nav", opt(o, "f", ""));
        item.premiumDiscount = opt(o, "premium_discount", opt(o, "premiumDiscount", opt(o, "g", "")));
        item.previousNav = opt(o, "previous_nav", opt(o, "previousNav", opt(o, "h", "")));
        item.dataDate = opt(o, "data_date", opt(o, "dataDate", opt(o, "i", "")));
        item.dataTime = opt(o, "data_time", opt(o, "dataTime", opt(o, "j", "")));
        item.source = opt(o, "source", "api");
        item.error = opt(o, "error", "");
        return item;
    }

    private static String opt(JSONObject o, String key, String fallback) {
        if (o == null || !o.has(key) || o.isNull(key)) return fallback;
        return String.valueOf(o.opt(key));
    }
}
