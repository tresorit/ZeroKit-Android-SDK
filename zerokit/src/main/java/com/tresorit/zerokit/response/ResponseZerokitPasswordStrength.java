package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

public class ResponseZerokitPasswordStrength extends ZerokitJson {
    private CrackTimesSeconds crack_times_seconds;
    private Feedback feedback;
    private double guesses_log10;
    private int length;
    private int score;

    public CrackTimesSeconds getCrack_times_seconds() {
        return crack_times_seconds;
    }

    public double getGuesses_log10() {
        return guesses_log10;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public int getLength() {
        return length;
    }

    public int getScore() {
        return score;
    }

    @Override
    public String toString() {
        return String.format("crack_times_seconds: %s, feedback: %s, guesses_log10: %s, length: %s, score: %s", crack_times_seconds, feedback, guesses_log10, length, score);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ZerokitJson> T parse(String json) {
        JSONObject jsonObject = new JSONObject(json);
        crack_times_seconds = new CrackTimesSeconds().parse(jsonObject.getJSONObject("crack_times_seconds").toString());
        feedback = new Feedback().parse(jsonObject.getJSONObject("feedback").toString());
        guesses_log10 = jsonObject.getDouble("guesses_log10");
        length = jsonObject.getInt("length");
        score = jsonObject.getInt("score");
        return (T) this;
    }
}
