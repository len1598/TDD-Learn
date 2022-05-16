package pers.lenwind.container;

import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Context {
    private Map<Type, ContextConfiguration.Provider<?>> container;

    public Context(Map<Type, ContextConfiguration.Provider<?>> componentProviders) {
        container = componentProviders;
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(componentProvider, new Stack<>());
            }
        });
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable(container.get(type)).map(provider -> (T) provider.get(this));
    }

    private void checkDependencies(ComponentProvider<?> provider, Stack<Type> dependencyStack) {
        if (dependencyStack.contains(provider.componentType)) {
            throw new CyclicDependencyException(dependencyStack.stream().toList());
        }
        dependencyStack.push(provider.componentType);
        provider.getDependencies().forEach(dependencyType -> {
            ContextConfiguration.Provider<?> dependency = container.get(dependencyType);
            if (dependency == null) {
                throw new DependencyNotFoundException(provider.componentType, dependencyType);
            }
            if (dependency instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(componentProvider, dependencyStack);
            }
        });
        dependencyStack.pop();
    }

    public Optional<ContextConfiguration.Provider<?>> get(ParameterizedType type) {
        return Optional.ofNullable(container.get(type));
    }
}
