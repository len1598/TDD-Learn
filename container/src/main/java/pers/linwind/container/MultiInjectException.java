package pers.linwind.container;

public class MultiInjectException extends BaseException {
    public MultiInjectException(Class<?> instanceType) {
        super(instanceType);
    }
}
