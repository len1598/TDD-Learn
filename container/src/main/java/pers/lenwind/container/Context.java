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

    public Optional get(Type type) {
        if (type instanceof ParameterizedType parameterizedType) {
            return getType(parameterizedType);
        } else if (type instanceof Class clazz) {
            return getType(clazz);
        }
        return Optional.empty();
    }

    private <T> Optional<T> getType(Class<T> type) {
        return Optional.ofNullable(container.get(type)).map(provider -> (T) provider.get(this));
    }

    private Optional<ContextConfiguration.Provider<?>> getType(ParameterizedType type) {
        return Optional.ofNullable(container.get(type));
    }

    private void checkDependencies(ComponentProvider<?> provider, Stack<Type> dependencyStack) {
        if (dependencyStack.contains(provider.componentType)) {
            throw new CyclicDependencyException(dependencyStack.stream().toList());
        }
        this.getClass().isAssignableFrom()
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
}
