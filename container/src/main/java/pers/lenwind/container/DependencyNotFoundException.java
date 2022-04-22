package pers.lenwind.container;

public class DependencyNotFoundException extends BaseException {
    private Class<?> dependency;

    public DependencyNotFoundException(Class<?> instanceType, Class<?> dependency) {
        super(instanceType);
        this.dependency = dependency;
    }

    public Class<?> getDependencyType() {
        return dependency;
    }
}
