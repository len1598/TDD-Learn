package pers.lenwind.args;

import pers.lenwind.args.exception.ParseException;

import java.util.List;

public class ParseDecimalsOption extends ParseOption<int[]> {
    @Override
    public int[] parse(List<String> args, Option option) {
        return values(args, option)
            .map(l -> l.stream().mapToInt(Integer::parseInt).toArray())
            .orElseThrow(() -> new ParseException(option.value(), int[].class));
    }
}
