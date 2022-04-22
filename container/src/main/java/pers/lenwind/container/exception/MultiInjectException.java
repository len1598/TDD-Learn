package pers.lenwind.container.exception;

public class MultiInjectException extends BaseException {
    public MultiInjectException(Class<?> instanceType) {
        super(instanceType);
    }
}
