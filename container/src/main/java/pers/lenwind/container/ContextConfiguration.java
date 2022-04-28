package pers.lenwind.container;

import jakarta.inject.Inject;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ContextConfiguration {
    private final Map<Class<?>, Provider> componentProviders = new HashMap<>();

    public <T> void bind(Class<T> componentType, T instance) {
        componentProviders.put(componentType, () -> instance);
    }

    public <ComponentType, InstanceType extends ComponentType>
    void bind(Class<ComponentType> componentType, Class<InstanceType> instanceType) {
        componentProviders.put(componentType, new ComponentProvider(instanceType));
    }

    public Map<Class<?>, Provider> initContainer() {
        initDependencies();
        checkDependencies();
        return componentProviders;
    }

    private void checkDependencies() {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider componentProvider) {
                componentProvider.checkDependencies(componentProvider, new Stack<>());
            }
        });
    }

    private void initDependencies() {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ComponentProvider componentProvider) {
                componentProvider.initDependencies();
            }
        });
    }

    public interface Provider {
        Object get();
    }

    class ComponentProvider implements Provider {
        Class<?> componentType;

        List<Class<?>> dependencies;
        private Constructor<?> constructor;
        private List<Field> injectFields;
        private List<Method> injectMethods;

        ComponentProvider(Class<?> componentType) {
            this.componentType = componentType;
        }

        @Override
        public Object get() {
            try {
                Object[] parameters = Arrays.stream(constructor.getParameterTypes())
                    .map(dependency -> componentProviders.get(dependency).get())
                    .toArray();
                Object instance = constructor.newInstance(parameters);
                for (Field field : injectFields) {
                    field.setAccessible(true);
                    field.set(instance, componentProviders.get(field.getType()).get());
                }
                for (Method method : injectMethods) {
                    method.setAccessible(true);
                    Object[] dependencies = Arrays.stream(method.getParameterTypes())
                        .map(type -> componentProviders.get(type).get()).toArray();
                    method.invoke(instance, dependencies);
                }
                return instance;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        public void initDependencies() {
            constructor = ComponentUtils.getConstructor(componentType);
            injectFields = ComponentUtils.getInjectFields(componentType);
            injectMethods = ComponentUtils.getInjectMethods(componentType);
            dependencies = CommonUtils.concatStream(
                Arrays.stream(constructor.getParameterTypes()),
                injectFields.stream().map(Field::getType),
                injectMethods.stream().flatMap(method1 -> Arrays.stream(method1.getParameterTypes()))).toList();
        }

        private void checkDependencies(ComponentProvider provider, Stack<Class<?>> dependencyStack) {
            if (dependencyStack.contains(provider.componentType)) {
                throw new CyclicDependencyException(dependencyStack.stream().toList());
            }
            dependencyStack.push(provider.componentType);
            provider.dependencies.forEach(dependencyType -> {
                Provider dependency = componentProviders.get(dependencyType);
                if (dependency == null) {
                    throw new DependencyNotFoundException(provider.componentType, dependencyType);
                }
                if (dependency instanceof ComponentProvider componentProvider) {
                    checkDependencies(componentProvider, dependencyStack);
                }
            });
            dependencyStack.pop();
        }
    }

    private static class ComponentUtils {
        static <T> Constructor<?> getConstructor(Class<T> implementation) {
            List<Constructor<?>> constructors = Arrays.stream(implementation.getConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toList();
            if (constructors.size() > 1) {
                throw new MultiInjectException(implementation);
            }
            return constructors.isEmpty() ? getDefaultConstructor(implementation) : constructors.get(0);
        }

        static <T> Constructor<T> getDefaultConstructor(Class<T> implementation) {
            try {
                return implementation.getDeclaredConstructor();
            } catch (NoSuchMethodException e) {
                throw new NoAvailableConstructionException(implementation);
            }
        }

        static List<Field> getInjectFields(Class<?> componentType) {
            List<Field> fields = new ArrayList<>();
            Class<?> currentClass = componentType;
            while (currentClass != Object.class) {
                fields.addAll(Arrays.stream(currentClass.getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(Inject.class)).toList());
                currentClass = currentClass.getSuperclass();
            }
            return fields;
        }

        static List<Method> getInjectMethods(Class<?> componentType) {
            return Arrays.stream(componentType.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Inject.class)).toList();
        }
    }
}
