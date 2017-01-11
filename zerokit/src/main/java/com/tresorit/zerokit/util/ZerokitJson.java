package com.tresorit.zerokit.util;

public abstract class ZerokitJson {

    public abstract <T extends ZerokitJson> T parse(String json);

}
