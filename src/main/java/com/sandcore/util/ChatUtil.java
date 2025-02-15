package com.sandcore.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;

public class ChatUtil {
    private static final Pattern HEX_PATTERN = Pattern.compile("<hex:(#[A-Fa-f0-9]{6})>");

    public static String translateHexColorCodes(String message) {
        if (message == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            // Convert hex to legacy color code format, e.g., §x§R§R§G§G§B§B
            String replacement = toLegacy(hexCode);
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        // Also translate alternate color codes (e.g., &a, &b) if present.
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    private static String toLegacy(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != 7) return hex;
        StringBuilder sb = new StringBuilder("§x");
        // Convert each hex digit into the legacy format.
        for (int i = 1; i < hex.length(); i++) {
            sb.append("§").append(hex.charAt(i));
        }
        return sb.toString();
    }
} 