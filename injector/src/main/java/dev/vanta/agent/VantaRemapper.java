package dev.vanta.agent;

import org.objectweb.asm.commons.Remapper;

/**
 * Rewrites client bytecode from the names it was compiled against (Yarn) to the names
 * the running game actually has, which is what a mod loader normally does for mods.
 */
public final class VantaRemapper extends Remapper {

    private final Mappings mappings;

    public VantaRemapper(Mappings mappings) {
        this.mappings = mappings;
    }

    @Override
    public String map(String internalName) {
        return mappings.officialClass(internalName);
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (name.startsWith("<")) {
            return name;
        }
        return mappings.method(owner, name, descriptor);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        return mappings.field(owner, name, descriptor);
    }
}
