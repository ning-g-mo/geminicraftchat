package cn.ningmo.geminicraftchat.persona;

public class Persona {
    private String id;
    private String name;
    private String description;
    private String prompt;
    private String greeting;

    public Persona(String id, String name, String description, String prompt, String greeting) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prompt = prompt;
        this.greeting = greeting;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getPrompt() { return prompt; }
    public String getGreeting() { return greeting; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    @Override
    public String toString() {
        return name;
    }
}