package pers.lenwind.container;

public class NoAvailableConstructionException extends BaseException {
    public NoAvailableConstructionException(Class<?> instanceType) {
        super(instanceType);
    }
}