package dev.tr7zw.gradle_compose;

import java.util.HashMap;
import java.util.Map;

import lombok.ToString;

@ToString
public class ComposeData {

    public String version = "1.0";
    public String source;
    public Map<String, String> replacements = new HashMap<String, String>();
    public Map<String, Project> subProjects = new HashMap<String, ComposeData.Project>();
    public Project rootProject;

    @ToString
    public static class Project {
        public String template;
        public Map<String, String> replacements = new HashMap<String, String>();
    }
    
}
