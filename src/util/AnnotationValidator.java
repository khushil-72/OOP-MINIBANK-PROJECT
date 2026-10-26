package util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import model.annotation.MaxLength;
import model.annotation.Positive;

/** Practical 7: validates any object by reading its annotations with reflection. */
public class AnnotationValidator {
    private AnnotationValidator() {
    }

    public static String[] validate(Object obj) {
        List<String> errors = new ArrayList<>();
        // walk up the class hierarchy so inherited fields (e.g. Account.balance) are checked too
        for (Class<?> c = obj.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                try {
                    if (field.isAnnotationPresent(Positive.class)) {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        if (value instanceof Number n && n.longValue() <= 0) {
                            errors.add(field.getAnnotation(Positive.class).message());
                        }
                    }
                    if (field.isAnnotationPresent(MaxLength.class)) {
                        field.setAccessible(true);
                        Object value = field.get(obj);
                        int max = field.getAnnotation(MaxLength.class).value();
                        if (value instanceof String s && s.length() > max) {
                            errors.add("length must be <= " + max);
                        }
                    }
                } catch (IllegalAccessException e) {
                    errors.add("cannot read field " + field.getName());
                }
            }
        }
        return errors.toArray(new String[0]);
    }
}
