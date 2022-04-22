package pers.linwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    @Nested
    class InjectionTest {
        private ContextConfiguration contextConfiguration;

        @BeforeEach
        void setUp() {
            contextConfiguration = new ContextConfiguration();
        }

        @Test
        void should_bind_instance_to_context() {
            Bean instance = new Bean() {
            };
            contextConfiguration.bind(Bean.class, instance);

            Bean bean = new Context(contextConfiguration).get(Bean.class).get();
            assertSame(instance, bean);
        }

        @Nested
        class ConstructionTest {
            @Test
            void should_inject_instance_with_default_construction() {
                contextConfiguration.bind(Bean.class, Instance.class);

                Bean bean = new Context(contextConfiguration).get(Bean.class).get();
                assertTrue(bean instanceof Instance);
            }

            @Test
            void should_inject_instance_with_annotation_construction() {
                Dependency dependency = new DependencyWithConstruction();
                contextConfiguration.bind(Dependency.class, dependency);
                contextConfiguration.bind(Bean.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration).get(Bean.class).get();
                assertSame(dependency, bean.dependency);
            }

            @Test
            void should_inject_instance_with_dependency() {
                contextConfiguration.bind(Dependency.class, DependencyWithDependency.class);
                contextConfiguration.bind(String.class, "dependency");
                contextConfiguration.bind(Bean.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration).get(Bean.class).get();
                assertNotNull(bean.dependency);
                assertEquals("dependency", ((DependencyWithDependency) bean.dependency).dependency);
            }

            @Test
            void should_throw_exception_if_multi_inject_annotation() {
                MultiInjectException exception = assertThrows(
                        MultiInjectException.class,
                        () -> contextConfiguration.bind(Bean.class, MultiInjectConstruction.class));
                assertEquals(MultiInjectConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_no_inject_annotation_nor_default_construction() {
                NoAvailableConstructionException exception = assertThrows(
                        NoAvailableConstructionException.class,
                        () -> contextConfiguration.bind(Bean.class, NoAvailableConstruction.class));
                assertEquals(NoAvailableConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_dependency_not_found() {
                contextConfiguration.bind(Bean.class, InstanceWithInject.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration));
                assertEquals(InstanceWithInject.class, exception.getInstanceType());
                assertEquals(Dependency.class, exception.getDependencyType());
            }

            @Test
            void should_throw_exception_if_cyclic_transitive_dependency() {
                contextConfiguration.bind(Bean.class, InstanceWithInject.class);
                contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
                contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

                CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration));
                Set<Class<?>> dependencies = Set.of(InstanceWithInject.class, DependencyWithAnotherDependency.class, DependencyWithBean.class);
                assertTrue(exception.getDependencies().containsAll(dependencies));
                assertEquals(InstanceWithInject.class, exception.getInstanceType());
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
