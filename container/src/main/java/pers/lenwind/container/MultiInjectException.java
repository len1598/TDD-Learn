package pers.lenwind.container;

public class MultiInjectException extends BaseException {
    public MultiInjectException(Class<?> instanceType) {
        super(instanceType);
    }
}
