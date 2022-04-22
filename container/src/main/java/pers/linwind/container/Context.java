package pers.linwind.container;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Context {
    private final Map<Class<?>, Builder> container = new HashMap<>();

    public <T> void bind(Class<T> clazz, T instance) {
        container.put(clazz, () -> instance);
    }

    public <T> Optional<T> get(Class<T> type) {
        return (Optional<T>) Optional.ofNullable(container.get(type)).map(Builder::build);
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implement) {
        Constructor<?> constructor = getConstructor(implement);
        container.put(type, new InstanceBuilder(constructor));
    }

    private interface Builder {
        Object build();
    }

    private class InstanceBuilder implements Builder {
        boolean cyclicFlag;

        Constructor<?> constructor;

        InstanceBuilder(Constructor<?> constructor) {
            this.constructor = constructor;
        }

        @Override
        public Object build() {
            if (cyclicFlag) {
                throw new CyclicDependencyException();
            }
            cyclicFlag = true;
            Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                    .map(clazz -> get(clazz).orElseThrow(DependencyNotFoundException::new)).toArray();
            try {
                return constructor.newInstance(parameters);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            } finally {
                cyclicFlag = false;
            }
        }
    }

    private <T> Constructor<?> getConstructor(Class<T> implement) {
        List<Constructor<?>> constructors = Arrays.stream(implement.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toList();
        if (constructors.size() > 1) {
            throw new MultiInjectException();
        }
        return constructors.isEmpty() ? getDefaultConstructor(implement) : constructors.get(0);
    }

    private <T> Constructor<T> getDefaultConstructor(Class<T> implement) {
        try {
            return implement.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new NoAvailableConstructionException();
        }
    }
}
