package pers.lenwind.container;

import java.util.Map;
import java.util.Optional;

public class Context {
    private Map<Class<?>, ContextConfiguration.Provider> container;

    public Context(ContextConfiguration configuration) {
        container = configuration.initContainer();
    }

    public <T> Optional<T> get(Class<T> type) {
        return Optional.ofNullable((T) container.get(type).get());
    }
}
