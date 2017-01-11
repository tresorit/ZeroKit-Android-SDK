package com.tresorit.zerokitsdk.message;


public class CreateTresorFinishedMessage {
    private final String tresorId;

    public CreateTresorFinishedMessage(String tresorId) {
        this.tresorId = tresorId;
    }

    public String getTresorId() {
        return tresorId;
    }
}
