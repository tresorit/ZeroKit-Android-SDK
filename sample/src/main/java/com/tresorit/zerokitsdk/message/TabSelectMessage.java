package com.tresorit.zerokitsdk.message;

public class TabSelectMessage {
    private final int tabId;

    public TabSelectMessage(int tabId) {
        this.tabId = tabId;
    }

    public int getTabId() {
        return tabId;
    }
}
