package pers.linwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    @Nested
    class InjectionTest {
        private Context context;

        @BeforeEach
        void setUp() {
            context = new Context();
        }

        @Test
        void should_bind_instance_to_context() {
            Bean instance = new Bean() {
            };
            context.bind(Bean.class, instance);

            Bean bean = context.get(Bean.class).get();
            assertSame(instance, bean);
        }

        @Nested
        class ConstructionTest {
            @Test
            void should_inject_instance_with_default_construction() {
                context.bind(Bean.class, Instance.class);

                Bean bean = context.get(Bean.class).get();
                assertTrue(bean instanceof Instance);
            }

            @Test
            void should_inject_instance_with_annotation_construction() {
                Dependency dependency = new DependencyWithConstruction();
                context.bind(Dependency.class, dependency);
                context.bind(Bean.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) context.get(Bean.class).get();
                assertSame(dependency, bean.dependency);
            }

            @Test
            void should_inject_instance_with_dependency() {
                context.bind(Dependency.class, DependencyWithDependency.class);
                context.bind(String.class, "dependency");
                context.bind(Bean.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) context.get(Bean.class).get();
                assertNotNull(bean.dependency);
                assertEquals("dependency", ((DependencyWithDependency) bean.dependency).dependency);
            }

            @Test
            void should_throw_exception_if_multi_inject_annotation() {
                assertThrows(
                        MultiInjectException.class,
                        () -> context.bind(Bean.class, MultiInjectConstruction.class));
            }

            @Test
            void should_throw_exception_if_no_inject_annotation_nor_default_construction() {
                assertThrows(
                        NoAvailableConstructionException.class,
                        () -> context.bind(Bean.class, NoAvailableConstruction.class));
            }

            @Test
            void should_throw_exception_if_dependency_not_found() {
                context.bind(Bean.class, InstanceWithInject.class);

                assertThrows(DependencyNotFoundException.class, () -> context.get(Bean.class));
            }

            @Test
            void should_throw_exception_if_cyclic_transitive_dependency() {
                context.bind(Bean.class, InstanceWithInject.class);
                context.bind(Dependency.class, DependencyWithAnotherDependency.class);
                context.bind(AnotherDependency.class, DependencyWithBean.class);

                assertThrows(CyclicDependencyException.class, () -> context.get(Bean.class));
            }
        }

        @Nested
        class FieldTest {
        }

        @Nested
        class MethodTest {
        }
    }

    @Nested
    class DependencyWithConstructionTest {
    }

    @Nested
    class LifeCycleTest {
    }
}

interface Bean {
}

interface Dependency {
}

interface AnotherDependency {
}

class Instance implements Bean {
    public Instance() {
    }
}

class DependencyWithConstruction implements Dependency {
}

class DependencyWithDependency implements Dependency {
    String dependency;

    @Inject
    public DependencyWithDependency(String dependency) {
        this.dependency = dependency;
    }
}

class InstanceWithInject implements Bean {
    Dependency dependency;

    @Inject
    public InstanceWithInject(Dependency dependency) {
        this.dependency = dependency;
    }
}

class MultiInjectConstruction implements Bean {

    String obj;

    @Inject
    public MultiInjectConstruction() {
    }

    @Inject
    public MultiInjectConstruction(String obj) {
        this.obj = obj;
    }
}

class DependencyWithAnotherDependency implements Dependency {
    AnotherDependency anotherDependency;

    @Inject
    public DependencyWithAnotherDependency(AnotherDependency anotherDependency) {
        this.anotherDependency = anotherDependency;
    }
}

class DependencyWithBean implements AnotherDependency {
    Bean bean;

    @Inject
    public DependencyWithBean(Bean bean) {
        this.bean = bean;
    }
}

class NoAvailableConstruction implements Bean {
    private NoAvailableConstruction() {
    }
}
