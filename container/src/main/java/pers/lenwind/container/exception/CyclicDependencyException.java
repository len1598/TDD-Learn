package pers.lenwind.container.exception;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyException extends BaseException {
    private final Set<Class<?>> dependencies;

    public CyclicDependencyException(Class<?> instanceType, CyclicDependencyException cause) {
        super(cause.instanceType);
        this.dependencies = cause.dependencies;
        this.dependencies.add(instanceType);
    }

    public CyclicDependencyException(Class<?> instanceType) {
        super(instanceType);
        dependencies = new HashSet<>();
        dependencies.add(instanceType);
    }

    public Set<Class<?>> getDependencies() {
        return dependencies;
    }
}
