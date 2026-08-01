package fr.corehost.proxy.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Centralized prefix for all proxy messages.
 * Equivalent to Constants.PREFIX on the Lobby side.
 */
public class ProxyPrefix {

    /**
     * Returns the standard CoreHost prefix as a Component.
     * Format: [CoreHost] (DARK_GRAY brackets, GOLD text, followed by GRAY space)
     */
    public static Component get() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("CoreHost", NamedTextColor.GOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    /**
     * Returns a prefixed message Component.
     */
    public static Component message(String text, NamedTextColor color) {
        return get().append(Component.text(text, color));
    }

    /**
     * Returns a prefixed message Component with GRAY color (default).
     */
    public static Component message(String text) {
        return get().append(Component.text(text, NamedTextColor.GRAY));
    }
}
