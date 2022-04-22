package pers.lenwind.container.exception;

public class NoAvailableConstructionException extends BaseException {
    public NoAvailableConstructionException(Class<?> instanceType) {
        super(instanceType);
    }
}
