package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

public class CrackTimesSeconds extends ZerokitJson {
    private double online_throttling_100_per_hour;
    private double online_no_throttling_10_per_second;
    private double offline_slow_hashing_1e4_per_second;
    private double offline_fast_hashing_1e10_per_second;

    public double getOffline_fast_hashing_1e10_per_second() {
        return offline_fast_hashing_1e10_per_second;
    }

    public double getOffline_slow_hashing_1e4_per_second() {
        return offline_slow_hashing_1e4_per_second;
    }

    public double getOnline_no_throttling_10_per_second() {
        return online_no_throttling_10_per_second;
    }

    public double getOnline_throttling_100_per_hour() {
        return online_throttling_100_per_hour;
    }

    @Override
    public String toString() {
        return String.format("offline_fast_hashing_1e10_per_second: %s, offline_slow_hashing_1e4_per_second: %s, online_no_throttling_10_per_second: %s, online_throttling_100_per_hour: %s", offline_fast_hashing_1e10_per_second, offline_slow_hashing_1e4_per_second, online_no_throttling_10_per_second, online_throttling_100_per_hour);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ZerokitJson> T parse(String json) {
        JSONObject jsonObject = new JSONObject(json);
        online_throttling_100_per_hour = jsonObject.getDouble("online_throttling_100_per_hour");
        online_no_throttling_10_per_second = jsonObject.getDouble("online_no_throttling_10_per_second");
        offline_slow_hashing_1e4_per_second = jsonObject.getDouble("offline_slow_hashing_1e4_per_second");
        offline_fast_hashing_1e10_per_second = jsonObject.getDouble("offline_fast_hashing_1e10_per_second");
        return (T) this;
    }
}
