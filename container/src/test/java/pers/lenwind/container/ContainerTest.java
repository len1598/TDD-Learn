package pers.lenwind.container;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pers.lenwind.container.exception.CyclicDependencyException;
import pers.lenwind.container.exception.DependencyNotFoundException;
import pers.lenwind.container.exception.MultiInjectException;
import pers.lenwind.container.exception.NoAvailableConstructionException;

import java.util.Optional;
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

            Component component = new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
            assertSame(instance, component);
        }

        @Test
        void should_get_empty_if_not_bind_to_context() {
            Optional<Component> component = new Context(contextConfiguration.getComponentProviders()).get(Component.class);

            assertTrue(component.isEmpty());
        }

        @Nested
        class ConstructionInjection {
            @Test
            void should_inject_instance_with_default_construction() {
                contextConfiguration.bind(Component.class, Instance.class);

                Component component = new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
                assertTrue(component instanceof Instance);
            }

            @Test
            void should_inject_instance_with_annotation_construction() {
                Dependency dependency = new DependencyWithConstruction();
                contextConfiguration.bind(Dependency.class, dependency);
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
                assertSame(dependency, bean.dependency);
            }

            @Test
            void should_inject_instance_with_dependency() {
                contextConfiguration.bind(Dependency.class, DependencyWithDependency.class);
                contextConfiguration.bind(String.class, "dependency");
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                InstanceWithInject bean = (InstanceWithInject) new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
                assertNotNull(bean.dependency);
                assertEquals("dependency", ((DependencyWithDependency) bean.dependency).dependency);
            }

            @Test
            void should_throw_exception_if_multi_inject_annotation() {
                MultiInjectException exception = assertThrows(
                    MultiInjectException.class,
                    () -> contextConfiguration.bind(Component.class, MultiInjectConstruction.class));
                assertEquals(MultiInjectConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_no_inject_annotation_nor_default_construction() {
                NoAvailableConstructionException exception = assertThrows(
                    NoAvailableConstructionException.class,
                    () -> contextConfiguration.bind(Component.class, NoAvailableConstruction.class));
                assertEquals(NoAvailableConstruction.class, exception.getInstanceType());
            }

            @Test
            void should_throw_exception_if_dependency_not_found() {
                contextConfiguration.bind(Component.class, InstanceWithInject.class);

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> new Context(contextConfiguration.getComponentProviders()));
                assertEquals(InstanceWithInject.class, exception.getInstanceType());
                assertEquals(Dependency.class, exception.getDependencyType());
            }

            @Test
            void should_throw_exception_if_cyclic_transitive_dependency() {
                contextConfiguration.bind(Component.class, InstanceWithInject.class);
                contextConfiguration.bind(Dependency.class, DependencyWithAnotherDependency.class);
                contextConfiguration.bind(AnotherDependency.class, DependencyWithBean.class);

                CyclicDependencyException exception = assertThrows(CyclicDependencyException.class, () -> new Context(contextConfiguration.getComponentProviders()));
                Set<Class<?>> dependencies = Set.of(InstanceWithInject.class, DependencyWithAnotherDependency.class, DependencyWithBean.class);
                assertTrue(exception.getDependencies().containsAll(dependencies));
            }
        }

        @Nested
        class FieldInjection {
            @Test
            void should_inject_field_dependencies() {
                contextConfiguration.bind(Component.class, ComponentWithFieldDependency.class);
                contextConfiguration.bind(Dependency.class, DependencyWithConstruction.class);

                Context context = new Context(contextConfiguration.getComponentProviders());
                Component component = context.get(Component.class).get();
                assertEquals(DependencyWithConstruction.class, ((ComponentWithFieldDependency) component).dependency.getClass());
            }

            @Test
            void should_inject_super_class_field_dependencies() {
                contextConfiguration.bind(Component.class, ComponentWithSuperFieldDependency.class);
                contextConfiguration.bind(Dependency.class, DependencyWithConstruction.class);

                Context context = new Context(contextConfiguration.getComponentProviders());
                Component component = context.get(Component.class).get();
                assertEquals(DependencyWithConstruction.class, ((ComponentWithSuperFieldDependency) component).dependency.getClass());
            }

            static class ComponentWithFinalField implements Component {
                @Inject
                final Dependency dependency = null;
            }


            @Test
            void should_inject_failed_if_final_field() {
//                ContextConfiguration.ComponentProvider componentProvider = new ContextConfiguration.ComponentProvider(ComponentWithFinalField.class);
            }
        }

        @Nested
        class MethodInjection {
            @Test
            void should_inject_dependency_if_method_tag_annotation() {
                Dependency dependency = new Dependency() {
                };
                contextConfiguration.bind(Component.class, ComponentWithMethodInject.class);
                contextConfiguration.bind(Dependency.class, dependency);

                ComponentWithMethodInject component =
                    (ComponentWithMethodInject) new Context(contextConfiguration.getComponentProviders()).get(Component.class).get();
                assertSame(dependency, component.dependency);
            }

            static class SuperComponentWithMethodInject {
                protected int superCount;

                @Inject
                void setSuperCount() {
                    superCount++;
                }
            }

            static class SubComponent extends SuperComponentWithMethodInject {
            }


            static class SubComponentWithMethodInject extends SuperComponentWithMethodInject {
                int subCount;

                @Inject
                public void setSubCount() {
                    this.subCount = superCount + 1;
                }
            }

            @Test
            void should_inject_super_method() {
                contextConfiguration.bind(SubComponent.class, SubComponent.class);

                SubComponent component = new Context(contextConfiguration.getComponentProviders()).get(SubComponent.class).get();
                assertEquals(1, component.superCount);
            }

            @Test
            void should_inject_super_method_first_sub_method_second() {
                contextConfiguration.bind(SubComponentWithMethodInject.class, SubComponentWithMethodInject.class);

                Context context = new Context(contextConfiguration.getComponentProviders());
                SubComponentWithMethodInject component = context.get(SubComponentWithMethodInject.class).get();
                assertEquals(1, component.superCount);
                assertEquals(2, component.subCount);
            }

            static class OverrideMethodInject extends SuperComponentWithMethodInject {
                @Override
                @Inject
                void setSuperCount() {
                    superCount = superCount + 2;
                }
            }

            @Test
            void should_only_inject_subclass_method_if_override_method_tag_annotation_both() {
                contextConfiguration.bind(OverrideMethodInject.class, OverrideMethodInject.class);

                OverrideMethodInject component = new Context(contextConfiguration.getComponentProviders()).get(OverrideMethodInject.class).get();
                assertEquals(2, component.superCount);
            }

            static class NoInjectComponent extends SuperComponentWithMethodInject {
                @Override
                void setSuperCount() {
                    super.setSuperCount();
                }
            }

            @Test
            void should_not_inject_override_method_if_super_tag_annotation_but_sub_not_tag() {
                contextConfiguration.bind(NoInjectComponent.class, NoInjectComponent.class);

                NoInjectComponent component = new Context(contextConfiguration.getComponentProviders()).get(NoInjectComponent.class).get();
                assertEquals(0, component.superCount);
            }
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
    private NoAvailableConstruction(String noDefault) {
    }
}

class ComponentWithFieldDependency implements Component {
    @Inject
    Dependency dependency;
}

class ComponentWithSuperFieldDependency extends ComponentWithFieldDependency {
}

class ComponentWithMethodInject implements Component {
    Dependency dependency;

    @Inject
    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }
}