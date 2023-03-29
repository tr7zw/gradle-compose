package dev.tr7zw.gradle_compose;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.fusesource.jansi.AnsiConsole;

import dev.tr7zw.gradle_compose.ComposeData.Project;
import dev.tr7zw.gradle_compose.provider.SourceProvider;
import dev.tr7zw.gradle_compose.util.ConfigUtil;
import dev.tr7zw.gradle_compose.util.FileProcessingUtil;
import dev.tr7zw.gradle_compose.util.FileUtil;

public class App {
    public final String version = "0.0.2";

    public static void main(String[] args) {
        new App().run(args);
    }

    public void run(String[] args) {
        setup();
        printWelcome();
        ComposeData data = ConfigUtil.loadLocalConfig();
        addAutoReplacements(data);
        SourceProvider provider = FileUtil.getProvider(data);
        TemplateData template = ConfigUtil.getTemplateData(provider, data);
        processComposition(data, template, provider);
        provider.markAsDone();
    }

    private void setup() {
        AnsiConsole.systemInstall();
    }

    private void printWelcome() {
        System.out.println("Loading gradle-compose V" + version + "...");
    }

    private void addAutoReplacements(ComposeData data) {
        StringBuilder includes = new StringBuilder();
        for (String entry : data.subProjects.keySet()) {
            includes.append("include(\"" + entry + "\")\n");
        }
        data.replacements.put("autoincludes", includes.toString());
        StringBuilder githubWorkFlow = new StringBuilder();
        for (String entry : data.subProjects.keySet()) {
            githubWorkFlow.append("            " + entry + "/build/libs/*\n");
        }
        data.replacements.put("autoworkflowfiles", githubWorkFlow.toString());
        StringBuilder forkGithubWorkFlow = new StringBuilder();
        for (String entry : data.subProjects.keySet()) {
            forkGithubWorkFlow.append("            workspace/" + entry + "/build/libs/*\n");
        }
        data.replacements.put("githubautoworkflowfiles", forkGithubWorkFlow.toString());
    }

    private void processComposition(ComposeData data, TemplateData template, SourceProvider provider) {
        // copy in ./gradle files
        FileUtil.copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.properties");
        FileUtil.copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.jar");
        Map<String, String> baseReplacements = FileProcessingUtil.mergeReplacements(data.replacements,
                template.defaultReplacements);
        updateProject(new File("."), baseReplacements, provider, data.rootProject, template.availableFlags,
                data.enabledFlags);
        for (Entry<String, Project> entry : data.subProjects.entrySet()) {
            updateProject(new File(".", entry.getKey()), baseReplacements, provider, entry.getValue(),
                    template.availableFlags, data.enabledFlags);
        }
        if (data.version.equals("0.0.1")) {
            Set<String> customEntries = ConfigUtil.readCustomList(provider, "custom.compose");
            for (String name : customEntries) {
                FileUtil.copyIfAvailableWithReplacments(provider, new File("."), data.rootProject.template, name,
                        FileProcessingUtil.mergeReplacements(data.rootProject.replacements, baseReplacements),
                        template.availableFlags, data.enabledFlags);
            }
        } else {
            for (String name : template.customEntries) {
                FileUtil.copyIfAvailableWithReplacments(provider, new File("."), data.rootProject.template, name,
                        FileProcessingUtil.mergeReplacements(data.rootProject.replacements, baseReplacements),
                        template.availableFlags, data.enabledFlags);
            }
        }
        processFork(baseReplacements, provider, data.rootProject, template.availableFlags, data.enabledFlags);
    }

    private void updateProject(File baseDir, Map<String, String> replacements, SourceProvider provider, Project project,
            Set<String> availableTags, Set<String> enabledTags) {
        Map<String, String> projectReplacements = FileProcessingUtil.mergeReplacements(project.replacements,
                replacements);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/workflows/build.yml",
                ".github/workflows/build.yml", projectReplacements, availableTags, enabledTags);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/workflows/tag.yml",
                ".github/workflows/tag.yml", projectReplacements, availableTags, enabledTags);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "github/FUNDING.yml",
                ".github/FUNDING.yml", projectReplacements, availableTags, enabledTags);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "build.gradle",
                projectReplacements, availableTags, enabledTags);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "gradle.properties",
                projectReplacements, availableTags, enabledTags);
        FileUtil.copyIfAvailableWithReplacments(provider, baseDir, project.template, "settings.gradle",
                projectReplacements, availableTags, enabledTags);
    }

    private void processFork(Map<String, String> replacements, SourceProvider provider, Project project,
            Set<String> availableTags, Set<String> enabledTags) {
        File parentDir = new File("..");
        if (new File(parentDir, "patches").exists() && new File(parentDir, "upstream").exists()
                && new File(parentDir, "workspace").exists() && new File(parentDir, "repo").exists()
                && new File(parentDir, "sha").exists()) {
            System.out.println("Updating fork .github folder...");
            Map<String, String> projectReplacements = FileProcessingUtil.mergeReplacements(project.replacements,
                    replacements);
            FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template, "github/forkworkflows/build.yml",
                    ".github/workflows/build.yml", projectReplacements, availableTags, enabledTags);
            FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template, "github/forkworkflows/tag.yml",
                    ".github/workflows/tag.yml", projectReplacements, availableTags, enabledTags); 
            FileUtil.copyIfAvailableWithReplacments(provider, parentDir, project.template, "github/FUNDING.yml",
                    ".github/FUNDING.yml", projectReplacements, availableTags, enabledTags);
        }
    }

}
