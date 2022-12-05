package pers.lenwind.container;

import pers.lenwind.container.exception.InternalException;

import java.util.*;
import java.util.stream.Stream;

public class CommonUtils {
    private static final ResourceBundle bundle = ResourceBundle.getBundle("error-message");

    public static <T> List<T> concatStreamToList(Stream<T>... streams) {
        Stream<T> stream = Stream.empty();
        for (Stream<T> t : streams) {
            stream = Stream.concat(stream, t);
        }
        return stream.toList();
    }

    public static <T, R> List<Map.Entry<T, R>> zipStream(Stream<T> stream1, Stream<R> stream2) {
        List<Map.Entry<T, R>> result = new ArrayList<>();
        Iterator<T> iterator1 = stream1.iterator();
        Iterator<R> iterator2 = stream2.iterator();
        while (iterator1.hasNext() && iterator2.hasNext()) {
            result.add(Map.entry(iterator1.next(), iterator2.next()));
        }
        if (iterator1.hasNext() || iterator2.hasNext()) {
            throw new InternalException("stream length not equal");
        }
        return result;
    }

    public static String getErrorMsg(String key) {
        return bundle.getString(key);
    }
}
