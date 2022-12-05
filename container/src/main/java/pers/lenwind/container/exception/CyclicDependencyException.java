package pers.lenwind.container.exception;

import lombok.Getter;
import lombok.ToString;
import pers.lenwind.container.Descriptor;

import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter
public class CyclicDependencyException extends RuntimeException {
    private List<Descriptor> descriptors;

    public CyclicDependencyException(List<Descriptor> classes) {
        descriptors = classes;
    }

    public List<Class<?>> getDependencies() {
        return descriptors.stream().map(Descriptor::type).collect(Collectors.toList());
    }
}
