package dev.tr7zw.gradle_compose;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import dev.tr7zw.gradle_compose.ComposeData.Project;

public class App {
    public final String version = "0.0.1";

    public static void main(String[] args) {
        new App().run(args);
    }

    private Yaml yaml = new Yaml(new Constructor(ComposeData.class));
    private Yaml yamlSet = new Yaml(new Constructor(HashSet.class));

    public void run(String[] args) {
        setup();
        printWelcome();
        ComposeData data = loadConfig();
        addAutoReplacements(data);
        SourceProvider provider = getProvider(data);
        processComposition(data, provider);
    }

    private void setup() {
        AnsiConsole.systemInstall();
    }

    private void printWelcome() {
        System.out.println("Loading gradle-compose V" + version + "...");
    }

    private void addAutoReplacements(ComposeData data) {
        StringBuilder includes = new StringBuilder();
        for(String entry : data.subProjects.keySet()) {
            includes.append("include(\"" + entry + "\")\n");
        }
        data.replacements.put("autoincludes", includes.toString());
        StringBuilder githubWorkFlow = new StringBuilder();
        for(String entry : data.subProjects.keySet()) {
            githubWorkFlow.append("            " + entry + "/build/libs/*\n");
        }
        data.replacements.put("autoworkflowfiles", githubWorkFlow.toString());
    }
    
    private void processComposition(ComposeData data, SourceProvider provider) {
        // copy in ./gradle files
        copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.properties");
        copyIfAvailable(provider, "gradle/wrapper/gradle-wrapper.jar");
        updateProject(new File("."), data, provider, data.rootProject);
        for(Entry<String, Project> entry : data.subProjects.entrySet()) {
            updateProject(new File(".", entry.getKey()), data, provider, entry.getValue());
        }
        Set<String> customEntries = readCustomList(provider, "custom.compose");
        for(String name : customEntries) {
            copyIfAvailableWithReplacments(provider, new File("."), data.rootProject.template, name, data.rootProject.replacements,
                    data.replacements);
        }
    }

    private void updateProject(File baseDir, ComposeData data, SourceProvider provider, Project project) {
        copyIfAvailableWithReplacments(provider, baseDir, project.template, ".github/workflows/build.yml", project.replacements,
                data.replacements);
        copyIfAvailableWithReplacments(provider, baseDir, project.template, ".github/workflows/tag.yml", project.replacements,
                data.replacements);
        copyIfAvailableWithReplacments(provider, baseDir, project.template, "build.gradle", project.replacements,
                data.replacements);
        copyIfAvailableWithReplacments(provider, baseDir, project.template, "gradle.properties", project.replacements,
                data.replacements);
        copyIfAvailableWithReplacments(provider, baseDir, project.template, "settings.gradle", project.replacements,
                data.replacements);
    }

    private void copyIfAvailableWithReplacments(SourceProvider provider, File baseDir, String path, String file,
            Map<String, String> targetReplacement, Map<String, String> globalReplacements) {
        if (!provider.hasFile(path + "/" + file)) {
            return;
        }
        File target = new File(baseDir, file);
        if (target.exists()) {
            target.delete();
        }
        target.getParentFile().mkdirs();
        try (InputStream in = provider.getStream(path + "/" + file)) {
            String data = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            data = applyReplacements(data, targetReplacement);
            data = applyReplacements(data, globalReplacements);
            Files.write(target.toPath(), data.getBytes());
            //System.out.println(data);
            System.out.println("Wrote '" + target.getAbsolutePath() + "'...");
        } catch (Exception e) {
            System.out.println(ansi().fgRed().a("Error while copying '" + file + "'!").reset());
            e.printStackTrace();
            System.exit(1);
            return;
        }
    }

    private String applyReplacements(String text, Map<String, String> replacements) {
        for (Entry<String, String> entry : replacements.entrySet()) {
            String toReplace = "$" + entry.getKey() + "$";
            while (text.indexOf(toReplace) >= 0) {
                text = text.replace(toReplace, entry.getValue());
            }
        }
        return text;
    }

    private void copyIfAvailable(SourceProvider provider, String file) {
        if (!provider.hasFile(file)) {
            return;
        }
        File target = new File(file);
        if (target.exists()) {
            target.delete();
        }
        target.getParentFile().mkdirs();
        try (InputStream in = provider.getStream(file)) {
            Files.copy(in, target.toPath());
            System.out.println("Copied '" + target.getAbsolutePath() + "'...");
        } catch (Exception e) {
            System.out.println(ansi().fgRed().a("Error while copying '" + file + "'!").reset());
            e.printStackTrace();
            System.exit(1);
            return;
        }
    }

    private SourceProvider getProvider(ComposeData data) {
        if (data.source == null) {
            System.out.println(ansi().fgRed().a("No source defined!").reset());
            System.exit(1);
        }
        if(data.source.startsWith("https://github.com/")) {
            String url = data.source.replace("https://github.com", "https://raw.githubusercontent.com/");
            if(!url.endsWith("/")) {
                url = url + "/";
            }
            url = url.replace("/tree/", "/");
            return new GithubProvider(url);
        }
        File folder = new File(data.source);
        if (!folder.exists()) {
            System.out.println(ansi().fgRed().a("Folder '" + folder.getAbsolutePath() + "' not found!").reset());
            System.exit(1);
        }
        return new LocalSourceProvider(folder);
    }

    private ComposeData loadConfig() {
        File settingsFile = new File("gradle-compose.yml");
        if (!settingsFile.exists()) {
            System.out.println(ansi().fgRed().a("'" + settingsFile.getAbsolutePath() + "' not found!").reset());
            System.exit(1);
            return null;
        }
        ComposeData compose = null;
        try {
            compose = yaml.load(new FileInputStream(settingsFile));
        } catch (FileNotFoundException e) {
            System.out
                    .println(ansi().fgRed().a("Error while loading '" + settingsFile.getAbsolutePath() + "'!").reset());
            e.printStackTrace();
            System.exit(1);
            return null;
        }
        if (compose == null) {
            System.out.println(
                    ansi().fgRed().a("'" + settingsFile.getAbsolutePath() + "' wasn't parsed successfully!").reset());
            System.exit(1);
        }
        if (!"0.0.1".equals(compose.version)) {
            System.out.println(ansi().fgRed().a("Incompatible version defined! Please update gradle-compose!").reset());
            System.exit(1);
        }
        return compose;
    }
    
    private Set<String> readCustomList(SourceProvider provider, String file) {
        if (!provider.hasFile(file)) {
            return new HashSet<String>();
        }

        Set<String> set = null;

        try (InputStream in = provider.getStream(file)) {
            set = yamlSet.load(in);
        } catch (FileNotFoundException e) {
            // ignore
        } catch (IOException e) {
            System.out.println(ansi().fgRed().a("Error while reading '" + file + "'!").reset());
            e.printStackTrace();
            System.exit(1);
        }
        if(set == null) {
            return new HashSet<String>();
        }
        return set;
    }

}
