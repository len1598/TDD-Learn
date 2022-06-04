package pers.lenwind.args;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public class Args {
    public static <T> T parse(Class<T> optionsClass, String... args) {
        try {
            Constructor<?> constructor = optionsClass.getDeclaredConstructors()[0];
            List<String> argList = Arrays.asList(args);
            Object[] values = Arrays.stream(constructor.getParameters())
                .map(parameter -> parseOption(argList, parameter))
                .toArray();
            return (T) constructor.newInstance(values);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object parseOption(List<String> args, Parameter parameter) {
        return ParseOptionFactory.createOption(parameter.getType()).parse(args, parameter.getAnnotation(Option.class));
    }
}
