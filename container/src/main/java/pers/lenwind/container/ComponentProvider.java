package pers.lenwind.container;

import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import pers.lenwind.container.exception.BaseException;
import pers.lenwind.container.exception.IllegalInjectionException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public class ComponentProvider<T> implements Provider<T> {
    final private Class<T> componentType;

    private Constructor<T> constructor;

    private List<Field> injectFields;
    private List<Method> injectMethods;
    public ComponentProvider(Class<T> componentType) {
        this.componentType = componentType;
        init(componentType);
    }

    public Class<T> getComponentType() {
        return componentType;
    }

    private void init(Class<T> clazz) {
        constructor = getConstructor(clazz);
        injectFields = getInjectFields(clazz);
        injectMethods = getInjectMethods(clazz);
    }

    @Override
    public T get(Context context) {
        try {
            constructor.trySetAccessible();
            T instance = constructor.newInstance(toDependencies(context, constructor));
            for (Field field : injectFields) {
                field.trySetAccessible();
                field.set(instance, context.get(Descriptor.of(field.getGenericType(), getQualifier(field))).get());
            }
            for (Method method : injectMethods) {
                method.invoke(instance, toDependencies(context, method));
            }
            return instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    // TODO should throw exception if multiple qualifier annotation found
    private static Annotation getQualifier(AnnotatedElement executable) {
        return Arrays.stream(executable.getAnnotations()).filter(f -> f.annotationType().isAnnotationPresent(Qualifier.class)).findFirst().orElse(null);
    }

    public List<Descriptor> getDependencies() {
        Stream<Descriptor> constructorDependencies = toDescriptors(Arrays.stream(constructor.getGenericParameterTypes()), Arrays.stream(constructor.getParameterAnnotations()));
        Stream<Descriptor> fieldDependencies = toDescriptors(injectFields.stream().map(Field::getGenericType), injectFields.stream().map(Field::getAnnotations));
        Stream<Descriptor> methodDependencies = toDescriptors(injectMethods.stream().flatMap(m -> Arrays.stream(m.getGenericParameterTypes())),
            injectMethods.stream().flatMap(m -> Arrays.stream(m.getParameterAnnotations())));
        return CommonUtils.concatStreamToList(constructorDependencies, fieldDependencies, methodDependencies);
    }

    private Stream<Descriptor> toDescriptors(Stream<Type> types, Stream<Annotation[]> qualifiers) {
        return CommonUtils.zipStream(types, qualifiers).stream().map(this::toDescriptor);
    }

    private Descriptor toDescriptor(Map.Entry<Type, Annotation[]> entry) {
        Type type = entry.getKey();
        Annotation qualifier = Arrays.stream(entry.getValue())
            .filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class))
            .findFirst()
            .orElse(null);
        return Descriptor.of(type, qualifier);
    }

    private static Object[] toDependencies(Context context, Executable executable) {
        return Arrays.stream(executable.getGenericParameterTypes())
            .map(type -> context.get(Descriptor.of(type, getQualifier(executable))).get()).toArray();
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
