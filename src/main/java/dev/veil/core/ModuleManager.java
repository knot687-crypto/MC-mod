package dev.veil.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
    private final Map<Class<? extends Module>, Module> byClass = new LinkedHashMap<>();
    private final List<Module> modules = new ArrayList<>();

    public void register(Module module) {
        modules.add(module);
        byClass.put(module.getClass(), module);
    }

    public <T extends Module> T get(Class<T> type) {
        Module module = byClass.get(type);
        return type.isInstance(module) ? type.cast(module) : null;
    }

    public List<Module> all() {
        return modules;
    }

    public List<Module> byCategory(Category category) {
        List<Module> result = new ArrayList<>();
        for (Module module : modules) {
            if (module.category() == category) {
                result.add(module);
            }
        }
        return result;
    }

    public void tick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onTick();
            }
        }
    }
}
