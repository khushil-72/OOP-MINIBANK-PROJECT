package service;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import model.annotation.Id;

/** Practical 13: one generic repository for any type that has an @Id field. */
public class Repository<T> {
    private final HashMap<String, T> items = new HashMap<>();

    public T save(T entity) {
        items.put(idOf(entity), entity);
        return entity;
    }

    public T findById(String id) {
        return items.get(id);
    }

    public ArrayList<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public boolean delete(String id) {
        return items.remove(id) != null;
    }

    @SafeVarargs
    public final void saveAll(T... entities) {
        for (T entity : entities) {
            save(entity);
        }
    }

    /** Generic method with a bounded type parameter. */
    public static <N extends Number> double average(N[] values) {
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (N value : values) {
            sum += value.doubleValue();
        }
        return sum / values.length;
    }

    /** Finds the field marked @Id by reflection and returns its value as the key. */
    private String idOf(T entity) {
        for (Class<?> c = entity.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    try {
                        field.setAccessible(true);
                        return String.valueOf(field.get(entity));
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException("Cannot read @Id field", e);
                    }
                }
            }
        }
        throw new IllegalArgumentException("No @Id field found in " + entity.getClass().getName());
    }
}
