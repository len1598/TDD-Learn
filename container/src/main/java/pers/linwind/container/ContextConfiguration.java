package pers.linwind.container;

import jakarta.inject.Inject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ContextConfiguration {
    private final Map<Class<?>, Builder> contextBuilder = new HashMap<>();

    public <T> void bind(Class<T> clazz, T instance) {
        contextBuilder.put(clazz, () -> instance);
    }

    public <T, R extends T> void bind(Class<T> type, Class<R> implement) {
        Constructor<?> constructor = getConstructor(implement);
        contextBuilder.put(type, new InstanceBuilder(constructor));
    }

    public Map<Class<?>, Object> initContainer() {
        return contextBuilder.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().build()));
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
                throw new CyclicDependencyException(constructor.getDeclaringClass());
            }
            cyclicFlag = true;
            try {
                Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                        .map(dependency -> {
                            Builder builder = contextBuilder.get(dependency);
                            if (builder == null) {
                                throw new DependencyNotFoundException(constructor.getDeclaringClass(), dependency);
                            }
                            return builder.build();
                        })
                        .toArray();
                return constructor.newInstance(parameters);
            } catch (CyclicDependencyException e) {
                throw new CyclicDependencyException(constructor.getDeclaringClass(), e);
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
            throw new MultiInjectException(implement);
        }
        return constructors.isEmpty() ? getDefaultConstructor(implement) : constructors.get(0);
    }

    private <T> Constructor<T> getDefaultConstructor(Class<T> implement) {
        try {
            return implement.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new NoAvailableConstructionException(implement);
        }
    }
}
