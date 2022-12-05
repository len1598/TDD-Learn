package pers.lenwind.container;

import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Context {
    private Map<Ref, Provider<?>> container;


    public Context(Map<Ref, Provider<?>> initialCache) {
        this.container = initialCache;
        this.container.forEach(((ref, provider) -> {
            if (provider instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(new Descriptor(componentProvider.getComponentType(), false, ref.getQualifier()),
                    componentProvider.getDependencies(), new Stack<>());
            }
        }));
    }

    public <T> Optional<T> getInstance(Class<T> type, Annotation qualifier) {
        return Optional.ofNullable(container.get(Ref.of(type, qualifier))).map(provider -> (T) provider.get(this));
    }

    public <T> Optional<T> getInstance(Class<T> type) {
        return getInstance(type, null);
    }

    public <T> Optional<Provider<T>> getProvider(Class<T> type) {
        return getProvider(type, null);
    }

    public <T> Optional<Provider<T>> getProvider(Class<T> type, Annotation qualifier) {
        return Optional.ofNullable(container.get(Ref.of(type, qualifier))).map(p -> (Provider<T>) p);
    }

    Optional get(Descriptor descriptor) {
        return descriptor.isProvider() ? getProvider(descriptor.type(), descriptor.qualifier())
            : getInstance(descriptor.type(), descriptor.qualifier());
    }

    private void checkDependencies(Descriptor descriptor, List<Descriptor> dependencies, Stack<Descriptor> dependencyStack) {
        if (dependencyStack.contains(descriptor)) {
            throw new CyclicDependencyException(dependencyStack.stream().toList());
        }
        dependencyStack.push(descriptor);
        dependencies.forEach(dependency -> {
            Provider<?> dependencyProvider = container.get(dependency.toRef());
            if (dependencyProvider == null) {
                throw new DependencyNotFoundException(descriptor.type(), dependency.type());
            }
            if (dependency.isProvider()) {
                return;
            }
            // TODO singleton type provider
            if (dependencyProvider instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(new Descriptor(componentProvider.getComponentType(), false, dependency.qualifier()),
                    componentProvider.getDependencies(), dependencyStack);
            }
        });
        dependencyStack.pop();
    }
}
