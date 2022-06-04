package pers.lenwind.args;

import java.util.List;

public class ParseDecimalsOption extends ParseOption<int[]> {
    @Override
    public int[] parse(List<String> args, Option option) {
        return values(args, option).stream().mapToInt(Integer::parseInt).toArray();
    }
}
