package util;

/** Practical 8: a small AutoCloseable class for the try-with-resources example. */
public class SampleResource implements AutoCloseable {
    private final String name;

    public SampleResource(String name) {
        this.name = name;
        System.out.println("Resource opened: " + name);
    }

    public void use() {
        System.out.println("Using resource: " + name);
    }

    @Override
    public void close() {
        System.out.println("Resource closed: " + name);
    }
}
