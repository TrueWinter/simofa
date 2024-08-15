package dev.truewinter.simofa;

public class Template {
    private String id;
    private String name;
    private String template;

    public Template() {}

    public Template(String id, String name, String template) {
        this.id = id;
        this.name = name;
        this.template = template;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }
}
