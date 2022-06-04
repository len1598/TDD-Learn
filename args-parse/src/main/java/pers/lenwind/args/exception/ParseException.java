package pers.lenwind.args.exception;

public class ParseException extends RuntimeException {
    private String flag;
    private Class<?> type;

    public ParseException(String flag) {
        this.flag = flag;
        this.type = type;
    }
}
