package pers.lenwind.container;

import java.util.List;
import java.util.ResourceBundle;
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

    public static String getErrorMsg(String key) {
        return bundle.getString(key);
    }
}
