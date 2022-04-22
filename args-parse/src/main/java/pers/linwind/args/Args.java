package pers.linwind.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class Args {
    public static <T> T parse(Class<T> optionsClass, String... args) {

        try {
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            List<String> argList = Arrays.asList(args);
            Object[] values = Arrays.stream(constructor.getParameters())
                    .map(parameter -> getValue(argList, parameter))
                    .toList().toArray();
            return (T) constructor.newInstance(values);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getValue(List<String> args, Parameter parameter) {
        Option option = parameter.getAnnotation(Option.class);
        Object value = null;
        int index = args.indexOf(option.value());
        if (parameter.getType() == boolean.class) {
            value = index != -1;
        }
        if (parameter.getType() == int.class) {
            value = index != -1 ? Integer.parseInt(args.get(index + 1)) : null;
        }
        if (parameter.getType() == String.class) {
            value = index != -1 ? args.get(index + 1) : null;
        }
        return value;
    }
}
