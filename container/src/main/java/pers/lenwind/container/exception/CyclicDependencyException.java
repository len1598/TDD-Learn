package pers.lenwind.container.exception;

import lombok.ToString;

import java.util.List;

@ToString
public class CyclicDependencyException extends RuntimeException {
    private final List<Class<?>> dependencies;

    public CyclicDependencyException(List<Class<?>> classes) {
        dependencies = classes;
    }

    public List<Class<?>> getDependencies() {
        return dependencies;
    }
}
