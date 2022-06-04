package pers.lenwind.args;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public abstract class ParseOption<T> {
    public abstract T parse(List<String> args, Option option);

    protected Optional<List<String>> values(List<String> args, Option option) {
        int index = args.indexOf(option.value());
        if (index == -1) {
            return Optional.empty();
        }
        return Optional.of(args.subList(index + 1, getFlowingFlagIndex(index, args)));
    }

    private int getFlowingFlagIndex(int index, List<String> args) {
        return IntStream.range(index + 1, args.size())
            .filter(i -> args.get(i).matches("-[dglp]"))
            .findFirst().orElse(args.size());
    }
}
