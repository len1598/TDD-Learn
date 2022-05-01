package pers.lenwind.container;

import java.util.ResourceBundle;
import java.util.stream.Stream;

public class CommonUtils {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("error-message");

    public static <T> Stream<T> concatStream(Stream<T>... streams) {
        Stream<T> stream = Stream.empty();
        for (Stream<T> t : streams) {
            stream = Stream.concat(stream, t);
        }
        return stream;
    }

    public static String getErrorMsg(String key) {
        return bundle.getString(key);
    }
}
