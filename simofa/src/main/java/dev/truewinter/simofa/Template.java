package dev.truewinter.simofa;

public class Template {
    private final int id;
    private final String name;
    private final String template;

    public Template(int id, String name, String template) {
        this.id = id;
        this.name = name;
        this.template = template;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }
}
