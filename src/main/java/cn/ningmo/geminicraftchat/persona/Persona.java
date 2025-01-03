package cn.ningmo.geminicraftchat.persona;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class Persona {
    private final String id;
    private final String name;
    private final String description;
    private final String context;

    public String getPrompt(String message) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(context).append("\n\n");
        prompt.append("用户: ").append(message).append("\n");
        prompt.append("助手: ");
        return prompt.toString();
    }
} 