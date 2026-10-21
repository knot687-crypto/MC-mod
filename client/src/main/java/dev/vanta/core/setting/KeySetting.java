package dev.vanta.core.setting;

import org.lwjgl.glfw.GLFW;

public final class KeySetting extends Setting<Integer> {

    public KeySetting(String name, int def) {
        super(name, def);
    }

    public int get() {
        return value;
    }

    public void setKey(int key) {
        value = key;
    }

    public static String nameOf(int key) {
        if (key <= 0) {
            return "none";
        }
        String name = GLFW.glfwGetKeyName(key, 0);
        if (name != null) {
            return name.toUpperCase();
        }
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LSHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RSHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LCTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RCTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ESCAPE -> "ESC";
            default -> "K" + key;
        };
    }

    @Override
    public String display() {
        return nameOf(value);
    }
}
