package pers.lenwind.container;

import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class Context {
    private Map<Class<?>, ContextConfiguration.Provider> container;

    public Context(Map<Class<?>, ContextConfiguration.Provider> componentProviders) {
        componentProviders.values().forEach(provider -> {
            if (provider instanceof ContextConfiguration.ComponentProvider componentProvider) {
                componentProvider.checkDependencies(componentProvider, new Stack<>());
            }
        });
        container = componentProviders;
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) container.get(type).get());
    }
}
