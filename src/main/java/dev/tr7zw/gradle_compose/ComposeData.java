package dev.tr7zw.gradle_compose;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.ToString;

@ToString
public class ComposeData {

    public String version = "invalid";
    public String source;
    public Map<String, String> replacements = new HashMap<String, String>();
    public Map<String, Project> subProjects = new HashMap<String, ComposeData.Project>();
    public Project rootProject;
    public Set<String> enabledFlags = new HashSet<>();

    @ToString
    public static class Project {
        public String template;
        public Map<String, String> replacements = new HashMap<String, String>();
    }
    
}
