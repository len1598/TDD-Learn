package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.stream.IntStream;

public abstract class ParseOption<T> {
    public abstract T parse(List<String> args, Option option);

    protected List<String> values(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        if (index == -1) {
            Class<?> parameterizeClazz = (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            throw new ParseException(option.value(), parameterizeClazz);
        }
        return args.subList(index + 1, getFlowingFlagIndex(index, args));
    }

    private int getFlowingFlagIndex(int index, List<String> args) {
        return IntStream.range(index + 1, args.size())
            .filter(i -> args.get(i).matches("-[dglp]"))
            .findFirst().orElse(args.size());
    }
}
