package pers.lenwind.container;

import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Context {
    private Map<Ref, Provider<?>> container;


    public Context(Map<Ref, Provider<?>> initialCache) {
        this.container = initialCache;
        this.container.values().forEach(provider -> {
            if (provider instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(componentProvider, new Stack<>());
            }
        });
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

    Optional get(Type type, Annotation qualifier) {
        if (type instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getRawType() != Provider.class) {
                return Optional.empty();
            }
            return getProvider((Class<?>)parameterizedType.getActualTypeArguments()[0], qualifier);
        } else if (type instanceof Class clazz) {
            return getInstance(clazz, qualifier);
        }
        return Optional.empty();
    }

    private void checkDependencies(ComponentProvider<?> provider, Stack<Type> dependencyStack) {
        Type componentType = provider.getComponentType();
        if (componentType instanceof ParameterizedType) {
            return;
        }
        if (dependencyStack.contains(componentType)) {
            throw new CyclicDependencyException(dependencyStack.stream().toList());
        }
        dependencyStack.push(componentType);
        provider.getDependencies().forEach(dependencyType -> {
            Class<?> dependencyClazz = (Class<?>) (dependencyType instanceof ParameterizedType parameterizedType ? parameterizedType.getActualTypeArguments()[0] : dependencyType);
            Provider<?> dependency = container.get(Ref.of(dependencyClazz));
            if (dependency == null) {
                throw new DependencyNotFoundException(componentType, dependencyClazz);
            }
            if (dependencyType instanceof ParameterizedType) {
                return;
            }
            if (dependency instanceof ComponentProvider<?> componentProvider) {
                checkDependencies(componentProvider, dependencyStack);
            }
        });
        dependencyStack.pop();
    }
}
