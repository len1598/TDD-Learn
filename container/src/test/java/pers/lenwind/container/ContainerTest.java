package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    @Nested
    class ComponentConstruction {
        private ContextConfiguration contextConfiguration;

        @BeforeEach
        void setUp() {
            contextConfiguration = new ContextConfiguration();
        }

        @Test
        void should_bind_instance_to_context() {
            Component instance = new Component() {
            };
            contextConfiguration.bind(Component.class, instance);

            Component component = new Context(contextConfiguration).get(Component.class).get();
            assertSame(instance, component);
        }

        @Nested
        class ComponentInjection {
            @Test
            void should_inject_instance_with_default_construction() {
                contextConfiguration.bind(Component.class, Instance.class);

                Component component = new Context(contextConfiguration).get(Component.class).get();
                assertTrue(component instanceof Instance);
            }

            @Test
            void should_inject_instance_with_annotation_construction() {
                Dependency dependency = new DependencyWithConstruction();
                contextConfiguration.bind(Dependency.class, dependency);
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration).get(Component.class).get();
                assertSame(dependency, bean.dependency);
            }

            @Test
            void should_inject_instance_with_dependency() {
                contextConfiguration.bind(Dependency.class, DependencyWithDependency.class);
                contextConfiguration.bind(String.class, "dependency");
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration).get(Component.class).get();
                assertNotNull(bean.dependency);
                assertEquals("dependency", ((DependencyWithDependency) bean.dependency).dependency);
            }

            @Test
            void should_throw_exception_if_multi_inject_annotation() {
                contextConfiguration.bind(Component.class, MultiInjectConstruction.class);

                MultiInjectException exception = assertThrows(
                        MultiInjectException.class,
                        () -> new Context(contextConfiguration));
                assertEquals(MultiInjectConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_no_inject_annotation_nor_default_construction() {
                contextConfiguration.bind(Component.class, NoAvailableConstruction.class);

                NoAvailableConstructionException exception = assertThrows(
                        NoAvailableConstructionException.class,
                        () -> new Context(contextConfiguration));
                assertEquals(NoAvailableConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_dependency_not_found() {
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration));
                assertEquals(InstanceWithInject.class, exception.getInstanceType());
                assertEquals(Dependency.class, exception.getDependencyType());
            }

            @Test
            void should_throw_exception_if_cyclic_transitive_dependency() {
                contextConfiguration.bind(Component.class, InstanceWithInject.class);
                contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
                contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

                CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration));
                Set<Class<?>> dependencies = Set.of(InstanceWithInject.class, DependencyWithAnotherDependency.class, DependencyWithBean.class);
                assertTrue(exception.getDependencies().containsAll(dependencies));
            }
        }

        @Nested
        class FieldTest {
            @Test
            void should_inject_field_dependencies() {
                class ComponentWIthFieldDependency implements Component {
                    @Inject
                    private Dependency dependency;
                }
                contextConfiguration.bind(Component.class, ComponentWIthFieldDependency.class);
                contextConfiguration.bind(Dependency.class, DependencyWithConstruction.class);

                Context context = new Context(contextConfiguration);
                Component component = context.get(Component.class).get();
                assertEquals(DependencyWithDependency.class, ((ComponentWIthFieldDependency) component).dependency.getClass());
            }

            @Test
            void should_throw_exception_if_cannot_inject_field_dependencies() {

            }
        }

        @Nested
        class MethodTest {
        }
    }

    @Nested
    class DependencySelection {
    }

    @Nested
    class LifeCycleManagement {
    }
}

interface Component {
}

interface Dependency {
}

interface AnotherDependency {
}

class Instance implements Component {
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

class InstanceWithInject implements Component {
    Dependency dependency;

    @Inject
    public InstanceWithInject(Dependency dependency) {
        this.dependency = dependency;
    }
}

class MultiInjectConstruction implements Component {

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
    Component component;

    @Inject
    public DependencyWithBean(Component component) {
        this.component = component;
    }
}

class NoAvailableConstruction implements Component {
    private NoAvailableConstruction() {
    }
}
