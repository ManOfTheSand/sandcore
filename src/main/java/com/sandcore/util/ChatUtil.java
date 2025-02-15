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
            // Use Minecraft's API to get the proper color code (requires 1.16+)
            String replacement = ChatColor.of(hexCode).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);
        // Also translate alternate color codes (e.g., &a, &b) if present
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
} 