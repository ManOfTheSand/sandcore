package com.sandcore.casting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComboTrie {
    private final TrieNode root = new TrieNode();
    
    class TrieNode {
        Map<String, TrieNode> children = new HashMap<>();
        boolean isCombo;
    }
    
    public void insertCombo(String combo) {
        TrieNode node = root;
        String[] clicks = combo.split("");
        for (String click : clicks) {
            node = node.children.computeIfAbsent(click, k -> new TrieNode());
        }
        node.isCombo = true;
    }
    
    public boolean isPotentialCombo(List<String> current) {
        TrieNode node = root;
        for (String click : current) {
            node = node.children.get(click);
            if (node == null) return false;
        }
        return true;
    }
    
    public boolean isExactMatch(List<String> current) {
        TrieNode node = root;
        for (String click : current) {
            node = node.children.get(click);
            if (node == null) return false;
        }
        return node.isCombo;
    }
} 