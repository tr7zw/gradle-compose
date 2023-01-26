package dev.tr7zw.gradle_compose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.ToString;

@ToString
public class TemplateData {

    public String version = "invalid";
    public Map<String, String> defaultReplacements = new HashMap<String, String>();
    public Set<String> availableFlags = new HashSet<>();
    public Set<String> customEntries = new HashSet<>();
    
    public static TemplateData legacyFallback() {
        return new TemplateData();
    }
    
}
