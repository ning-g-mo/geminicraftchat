package cn.ningmo.geminicraftchat.response;

import lombok.Getter;

@Getter
public class GeminiResponse {
    private final boolean success;
    private final String message;
    private final String error;

    private GeminiResponse(boolean success, String message, String error) {
        this.success = success;
        this.message = message;
        this.error = error;
    }

    public static GeminiResponse success(String message) {
        return new GeminiResponse(true, message, null);
    }

    public static GeminiResponse error(String error) {
        return new GeminiResponse(false, null, error);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        if (!success) {
            throw new IllegalStateException("Cannot get message from error response");
        }
        return message;
    }

    public String getError() {
        if (success) {
            throw new IllegalStateException("Cannot get error from success response");
        }
        return error;
    }
} 