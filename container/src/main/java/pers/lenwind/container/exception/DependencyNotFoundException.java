package pers.lenwind.container.exception;

import lombok.ToString;

import java.lang.reflect.Type;

@ToString(callSuper = true)
public class DependencyNotFoundException extends BaseException {
    private Type dependency;

    public DependencyNotFoundException(Type instanceType, Type dependency) {
        super(instanceType);
        this.dependency = dependency;
    }

    public Type getDependencyType() {
        return dependency;
    }
}
