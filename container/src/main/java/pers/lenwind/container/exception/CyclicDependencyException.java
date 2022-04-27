package pers.lenwind.container.exception;

import java.util.List;

public class CyclicDependencyException extends RuntimeException {
    private final List<Class<?>> dependencies;

    public CyclicDependencyException(List<Class<?>> classes) {
        dependencies = classes;
    }

    public List<Class<?>> getDependencies() {
        return dependencies;
    }
}
