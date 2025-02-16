package com.sandcore.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.md_5.bungee.api.ChatColor;

public class ColorParser {
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<g:#([0-9a-fA-F]{6})>(.*?)</g:#([0-9a-fA-F]{6})>");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([0-9a-fA-F]{6})");

    public static String parseGradient(String text) {
        text = parseHexCodes(text);
        
        Matcher matcher = GRADIENT_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String startColor = matcher.group(1);
            String content = matcher.group(2);
            String endColor = matcher.group(3);
            
            String gradient = applyGradient(content, startColor, endColor);
            matcher.appendReplacement(sb, gradient);
        }
        matcher.appendTail(sb);
        
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    private static String parseHexCodes(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(sb, ChatColor.of("#" + hex).toString());
        }
        matcher.appendTail(sb);
        
        return sb.toString();
    }

    private static String applyGradient(String text, String startHex, String endHex) {
        StringBuilder result = new StringBuilder();
        int length = text.length();
        java.awt.Color startColor = java.awt.Color.decode("#" + startHex);
        java.awt.Color endColor = java.awt.Color.decode("#" + endHex);
        
        float[] hsbStart = java.awt.Color.RGBtoHSB(
            startColor.getRed(), 
            startColor.getGreen(), 
            startColor.getBlue(), 
            null
        );
        
        float[] hsbEnd = java.awt.Color.RGBtoHSB(
            endColor.getRed(),
            endColor.getGreen(),
            endColor.getBlue(),
            null
        );
        
        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) (length - 1);
            float hue = hsbStart[0] + (hsbEnd[0] - hsbStart[0]) * ratio;
            float saturation = hsbStart[1] + (hsbEnd[1] - hsbStart[1]) * ratio;
            float brightness = hsbStart[2] + (hsbEnd[2] - hsbStart[2]) * ratio;
            
            java.awt.Color color = java.awt.Color.getHSBColor(hue, saturation, brightness);
            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            result.append(ChatColor.of("#" + hex)).append(text.charAt(i));
        }
        
        return result.toString();
    }
} 