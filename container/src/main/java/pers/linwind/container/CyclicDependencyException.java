package pers.linwind.container;

import java.util.HashSet;
import java.util.Set;

public class CyclicDependencyException extends BaseException {
    private final Set<Class<?>> dependencies = new HashSet<>();
    public CyclicDependencyException(Class<?> instanceType, CyclicDependencyException cause) {
        this(instanceType);
        this.dependencies.addAll(cause.getDependencies());
    }

    public CyclicDependencyException(Class<?> instanceType) {
        super(instanceType);
        dependencies.add(instanceType);
    }

    public Set<Class<?>> getDependencies() {
        return dependencies;
    }
}
