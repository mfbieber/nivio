package de.bonndan.nivio.util;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SafeAssign {

    public static void assignSafe(String s, Consumer<String> c) {
        if (s != null) c.accept(s);
    }

    /**
     * Passes the first arg to the consumer if it is not null and the second arg is null.
     *
     * @param s value
     * @param absent null val
     * @param c value consumer
     */
    public static void assignSafeIfAbsent(String s, String absent, Consumer<String> c) {
        if (s != null && absent == null) c.accept(s);
    }

    public static <T> void assignSafe(Set<T> s, Consumer<Set<T>> c) {
        if (s != null) c.accept(s);
    }

    public static <T> void assignSafe(List<T> s, Consumer<List<T>> c) {
        if (s != null) c.accept(s);
    }
}
