package pers.lenwind.container.exception;

import lombok.ToString;

import java.lang.reflect.Type;

@ToString(callSuper = true)
public class DependencyNotFoundException extends BaseException {
    private Class<?> dependency;

    public DependencyNotFoundException(Type instanceType, Class<?> dependency) {
        super(instanceType);
        this.dependency = dependency;
    }

    public Class<?> getDependencyType() {
        return dependency;
    }
}
