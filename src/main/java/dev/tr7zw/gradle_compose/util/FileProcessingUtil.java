package dev.tr7zw.gradle_compose.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public final class FileProcessingUtil {

    private FileProcessingUtil() {
        // private
    }
    
    public static String applyReplacements(String text, Map<String, String> replacements) {
        for (Entry<String, String> entry : replacements.entrySet()) {
            String toReplace = "$" + entry.getKey() + "$";
            while (text.indexOf(toReplace) >= 0) {
                text = text.replace(toReplace, entry.getValue());
            }
        }
        return text;
    }
    
    /**
     * Merges the maps from left to right. Left has priority
     * 
     * @param mappings
     * @return
     */
    @SafeVarargs
    public static Map<String, String> mergeReplacements(Map<String, String>... mappings){
        Map<String, String> output = new HashMap<>();
        for(int i = 0; i < mappings.length; i++) {
            for(Entry<String, String> entry : mappings[i].entrySet()) {
                output.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }
        return output;
    }
    
}
