package pers.lenwind.container.exception;

import lombok.ToString;

import java.lang.reflect.Type;
import java.util.List;

@ToString
public class CyclicDependencyException extends RuntimeException {
    private final List<Type> dependencies;

    public CyclicDependencyException(List<Type> classes) {
        dependencies = classes;
    }

    public List<Type> getDependencies() {
        return dependencies;
    }
}
