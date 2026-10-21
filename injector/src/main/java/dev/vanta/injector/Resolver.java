package dev.vanta.injector;

import java.lang.reflect.Method;

/**
 * Thin wrapper over fabric-loader's MappingResolver.
 *
 * <p>Mod hooks are written with Yarn names because that is what the project compiles
 * against, but at runtime the game runs in intermediary. The resolver converts one to
 * the other so nothing here has to hardcode intermediary identifiers.
 */
final class Resolver {

    private static final String NAMESPACE = "named";

    private final Object handle;
    private final Method mapClassName;
    private final Method mapMethodName;

    private Resolver(Object handle, Method mapClassName, Method mapMethodName) {
        this.handle = handle;
        this.mapClassName = mapClassName;
        this.mapMethodName = mapMethodName;
    }

    static Resolver create(ClassLoader gameLoader) throws Exception {
        Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader", true, gameLoader);
        Object loader = loaderClass.getMethod("getInstance").invoke(null);
        Object resolver = loaderClass.getMethod("getMappingResolver").invoke(loader);
        Method mapClass = resolver.getClass().getMethod("mapClassName", String.class, String.class);
        Method mapMethod = resolver.getClass().getMethod("mapMethodName", String.class, String.class, String.class, String.class);
        Log.info("mappings: runtime namespace " + resolver.getClass().getMethod("getCurrentRuntimeNamespace").invoke(resolver));
        return new Resolver(resolver, mapClass, mapMethod);
    }

    /** Yarn binary class name to the runtime class name. */
    String className(String yarnName) {
        try {
            return (String) mapClassName.invoke(handle, NAMESPACE, yarnName);
        } catch (Exception exception) {
            Log.warn("class mapping failed for " + yarnName + ", keeping the given name");
            return yarnName;
        }
    }

    /** Yarn internal name, ready for ASM. */
    String internal(String yarnName) {
        return className(yarnName).replace('.', '/');
    }

    String methodName(String yarnOwner, String yarnMethod, String yarnDescriptor) {
        try {
            return (String) mapMethodName.invoke(handle, NAMESPACE, yarnOwner, yarnMethod, yarnDescriptor);
        } catch (Exception exception) {
            Log.warn("method mapping failed for " + yarnOwner + "#" + yarnMethod);
            return yarnMethod;
        }
    }
}
