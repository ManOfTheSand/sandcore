package com.sandcore.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

/**
 * Utility class for converting custom color and gradient tags into Minecraft color codes.
 * 
 * Supported tags:
 *   - Hex colors in the format: <hex:#RRGGBB>
 *   - Gradients in the format: <gradient:#RRGGBB:#RRGGBB>Text</gradient>
 */
public class ColorUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("<hex:(#[A-Fa-f0-9]{6})>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");

    public static String translate(String message) {
        if (message == null) return "";
        message = replaceHexColors(message);
        message = replaceGradientColors(message);
        return message;
    }

    private static String replaceHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            String replacement = ChatColor.of(hex).toString();
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String replaceGradientColors(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String startHex = matcher.group(1);
            String endHex = matcher.group(2);
            String text = matcher.group(3);
            String replacement = createGradient(text, startHex, endHex);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String createGradient(String text, String startHex, String endHex) {
        int length = text.length();
        if (length == 0) return "";
        int startR = Integer.parseInt(startHex.substring(1, 3), 16);
        int startG = Integer.parseInt(startHex.substring(3, 5), 16);
        int startB = Integer.parseInt(startHex.substring(5, 7), 16);

        int endR = Integer.parseInt(endHex.substring(1, 3), 16);
        int endG = Integer.parseInt(endHex.substring(3, 5), 16);
        int endB = Integer.parseInt(endHex.substring(5, 7), 16);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            double ratio = (length == 1) ? 0 : (double)i / (length - 1);
            int r = (int)Math.round(startR + (endR - startR) * ratio);
            int g = (int)Math.round(startG + (endG - startG) * ratio);
            int b = (int)Math.round(startB + (endB - startB) * ratio);
            String hexColor = String.format("#%02X%02X%02X", r, g, b);
            builder.append(ChatColor.of(hexColor)).append(text.charAt(i));
        }
        return builder.toString();
    }
} 