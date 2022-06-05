package pers.lenwind.container.exception;

import lombok.ToString;

import java.lang.reflect.Type;

@ToString(callSuper = true)
public class NoAvailableConstructionException extends BaseException {
    public NoAvailableConstructionException(Type instanceType) {
        super(instanceType);
    }
}
