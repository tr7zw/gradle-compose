package dev.tr7zw.gradle_compose.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
    
    public static String processTags(String text, Set<String> availableTags, Set<String> enabledTags) {
        for(String tag : availableTags) {
            if(enabledTags.contains(tag)) {
                text = text.replace("<" + tag + ">", "");
                text = text.replace("</" + tag + ">", "");
            } else {
                text = text.replaceAll("(?s)<" + tag + ">.*?</" + tag + ">", "");
            }
        }
        return text;
    }
    
    public static String cleanFile(String filename, String text) {
        if(filename.toLowerCase().endsWith(".yaml") || filename.toLowerCase().endsWith(".yml")) {
            String nText = text.replace("\n\n", "\n");
            while(!nText.equals(text)) {
                text = nText;
                nText = text.replace("\n\n", "\n");
            }
            text = nText;
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
