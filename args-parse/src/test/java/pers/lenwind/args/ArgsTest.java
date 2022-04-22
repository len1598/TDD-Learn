package pers.lenwind.args;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import pers.lenwind.args.exception.ParseException;

import static org.junit.jupiter.api.Assertions.*;

public class ArgsTest {
    // Single Option
    @Test
    void should_parse_boolean_true_if_flag_exist() {
        BooleanOption options = Args.parse(BooleanOption.class, "-l");

        assertTrue(options.logging());
    }

    record BooleanOption(@Option("-l") boolean logging) {
    }

    @Test
    void should_parse_integer_if_flag_exist() {
        IntegerOption options = Args.parse(IntegerOption.class, "-p", "8080");

        assertEquals(8080, options.port());
    }

    record IntegerOption(@Option("-p") int port) {
    }

    @Test
    void should_parse_string_if_flag_exist() {
        StringOption options = Args.parse(StringOption.class, "-d", "/usr/logs");

        assertEquals("/usr/logs", options.directory());
    }

    record StringOption(@Option("-d") String directory) {
    }

    // bad path
    @Test
    @Disabled
    void should_throw_exception_while_boolean_flag_contain_parameter() {
        assertThrows(ParseException.class, () -> Args.parse(BooleanOption.class, "-l", "f"));
    }

    @Test
    @Disabled
    void should_throw_exception_while_integer_flag_contain_invalid_parameter() {
        assertThrows(ParseException.class, () -> {
            String notInteger = "str";
            Args.parse(IntegerOption.class, "-p", notInteger);
        });
    }

    @Test
    @Disabled
    void should_throw_exception_while_string_flag_contain_invalid_parameter() {
        assertThrows(ParseException.class, () -> {
            String notDirectory = "";
            Args.parse(StringOption.class, "-d", notDirectory);
        });
    }

    // default value
    @Test
    void should_parse_boolean_false_if_flag_not_exist() {
        BooleanOption options = Args.parse(BooleanOption.class, "");

        assertFalse(options.logging());
    }

    // TODO -d ""


    // multi options
    // -l -p 8080 -d /usr/logs

    @Test
    void should_parse_multi_args() {
        Options options = Args.parse(Options.class, "-l -p 8080 -d /usr/logs".split(" "));

        assertTrue(options.logging());
        assertEquals(8080, options.port());
        assertEquals("/usr/logs", options.directory());
    }

    // -g this is a list -d 1 2 -3 5
    // TODO -g this is a list
    // TODO -d 1 2 -3 5

    static record Options(@Option("-l") boolean logging, @Option("-p") int port, @Option("-d") String directory) {
    }

    @Test
    @Disabled
    void should_parse_list_args() {
        ListOptions listOptions = Args.parse(ListOptions.class, "-g this is a list -d 1 2 -3 5".split(" "));
        assertArrayEquals(new String[]{"this", "is", "a", "list"}, listOptions.group());
        assertArrayEquals(new int[]{1, 2, -3, 5}, listOptions.decimals());
    }

    static record ListOptions(@Option("g") String[] group, @Option("d") int[] decimals) {
    }
}
