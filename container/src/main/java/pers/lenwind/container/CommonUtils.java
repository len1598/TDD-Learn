package pers.lenwind.container;

import java.util.stream.Stream;

public class CommonUtils {
    public static <T> Stream<T> concatStream(Stream<T>... streams) {
        Stream<T> stream = Stream.empty();
        for (Stream<T> t : streams) {
            stream = Stream.concat(stream, t);
        }
        return stream;
    }
}
