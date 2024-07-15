package dev.truewinter.simofa.docker;

public class Image {
    private final String name;
    private final String size;

    public Image(String name, String size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getSize() {
        return size;
    }
}
