package pers.lenwind.container;

import jakarta.inject.Inject;
import pers.lenwind.container.exception.BaseException;
import pers.lenwind.container.exception.IllegalInjectionException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ComponentProvider<T> implements ContextConfiguration.Provider<T> {
    final Class<T> componentType;

    private final Constructor<T> constructor;
    private final List<Field> injectFields;
    private final List<Method> injectMethods;

    public ComponentProvider(Class<T> componentType) {
        this.componentType = componentType;
        constructor = getConstructor(componentType);
        injectFields = getInjectFields(componentType);
        injectMethods = getInjectMethods(componentType);
    }

    @Override
    public T get(Context context) {
        try {
            T instance = constructor.newInstance(toDependencies(context, constructor));
            for (Field field : injectFields) {
                field.set(instance, context.get(field.getType()).get());
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Class<?>> getDependencies() {
        return CommonUtils.concatStreamToList(
            Arrays.stream(constructor.getParameterTypes()),
            injectFields.stream().map(Field::getType),
            injectMethods.stream().flatMap(method1 -> Arrays.stream(method1.getParameterTypes())));
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getParameterTypes())
            .map(type -> context.get(type).get()).toArray();
    }

    private static <T> Constructor<T> getConstructor(Class<T> implementation) {
        if (Modifier.isAbstract(implementation.getModifiers()) || Modifier.isInterface(implementation.getModifiers())) {
            throw new BaseException(implementation, "instantiation.illegal");
        }
        List<Constructor<?>> constructors = Arrays.stream(implementation.getConstructors())
            .filter(constructor -> constructor.isAnnotationPresent(Inject.class)).toList();
        if (constructors.size() > 1) {
            throw new MultiInjectException(implementation);
        }
        return constructors.isEmpty() ? getDefaultConstruction(implementation) : (Constructor<T>) constructors.get(0);
    }

    private static <T> Constructor<T> getDefaultConstruction(Class<T> implementation) {
        try {
            return implementation.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new NoAvailableConstructionException(implementation);
        }
    }

    private static List<Field> getInjectFields(Class<?> componentType) {
        List<Field> fields = getMembers(componentType,
            (superComponentType, noUseFields) -> Arrays.stream(superComponentType.getDeclaredFields())
                .filter(ComponentProvider::isInjectAnnotationPresent).toList());
        if (fields.stream().anyMatch(field -> Modifier.isFinal(field.getModifiers()))) {
            throw new IllegalInjectionException(componentType, "inject.field.final");
        }
        return fields;
    }


    private static List<Method> getInjectMethods(Class<?> componentType) {
        List<Method> methods = getMembers(componentType,
            (superComponentType, resultMethods) -> Arrays.stream(superComponentType.getDeclaredMethods())
                .filter(ComponentProvider::isInjectAnnotationPresent)
                .filter(method -> isOverrideMethodInStream(method, resultMethods.stream()))
                .filter(method -> isOverrideMethodInStream(method, noInjectMethods(componentType)))
                .toList());
        if (methods.stream().anyMatch(method -> method.getTypeParameters().length > 0)) {
            throw new IllegalInjectionException(componentType, "inject.method.type-parameter");
        }
        Collections.reverse(methods);
        return methods;
    }

    private static <Member> List<Member> getMembers(Class<?> componentType, BiFunction<Class<?>, List<Member>, List<Member>> getMembersBySuperClass) {
        Class<?> currentType = componentType;
        List<Member> members = new ArrayList<>();
        while (currentType != Object.class) {
            members.addAll(getMembersBySuperClass.apply(currentType, members));
            currentType = currentType.getSuperclass();
        }
        return members;
    }

    private static Stream<Method> noInjectMethods(Class<?> componentType) {
        return Arrays.stream(componentType.getDeclaredMethods()).filter(m -> !isInjectAnnotationPresent(m));
    }

    private static boolean isOverrideMethodInStream(Method method, Stream<Method> stream) {
        return stream.noneMatch(m ->
            Arrays.equals(m.getParameterTypes(), method.getParameterTypes()) && m.getName().equals(method.getName()));
    }

    private static boolean isInjectAnnotationPresent(AccessibleObject accessibleObject) {
        return accessibleObject.isAnnotationPresent(Inject.class);
    }
}
