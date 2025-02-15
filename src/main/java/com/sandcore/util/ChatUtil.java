package com.sandcore.util;

import java.awt.Color;
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

    // New method to translate gradient tags.
    // It processes tags of the form: <gradient:#startHex:#endHex>text</gradient>
    public static String translateGradient(String message) {
        if (message == null) return null;
        Pattern gradientPattern = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");
        Matcher matcher = gradientPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String innerText = matcher.group(3);
            String gradientText = generateGradient(innerText, startHex, endHex);
            matcher.appendReplacement(buffer, gradientText);
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String generateGradient(String text, String startHex, String endHex) {
        Color start = Color.decode(startHex);
        Color end = Color.decode(endHex);
        int length = text.length();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            double ratio = (length > 1) ? (double) i / (length - 1) : 0;
            int red = (int) (start.getRed() + ratio * (end.getRed() - start.getRed()));
            int green = (int) (start.getGreen() + ratio * (end.getGreen() - start.getGreen()));
            int blue = (int) (start.getBlue() + ratio * (end.getBlue() - start.getBlue()));
            // Convert to legacy color code format: §x§R§R§G§G§B§B
            String legacy = "§x"
                    + "§" + Integer.toHexString((red >> 4) & 0xF)
                    + "§" + Integer.toHexString(red & 0xF)
                    + "§" + Integer.toHexString((green >> 4) & 0xF)
                    + "§" + Integer.toHexString(green & 0xF)
                    + "§" + Integer.toHexString((blue >> 4) & 0xF)
                    + "§" + Integer.toHexString(blue & 0xF);
            sb.append(legacy).append(text.charAt(i));
        }
        return sb.toString();
    }

    // Convenience method: process gradient, then hex, then alternate color codes.
    public static String translateColors(String message) {
        if (message == null) return null;
        message = translateGradient(message);
        message = translateHexColorCodes(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
} 