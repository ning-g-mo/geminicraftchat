package cn.ningmo.geminicraftchat.api;

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
        return message;
    }
    
    public String getError() {
        return error;
    }
} 