package dev.tr7zw.gradle_compose.util;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import dev.tr7zw.gradle_compose.ComposeData;
import dev.tr7zw.gradle_compose.TemplateData;
import dev.tr7zw.gradle_compose.provider.SourceProvider;

public final class ConfigUtil {

    private static final Yaml yamlComposeData = new Yaml(new Constructor(ComposeData.class));
    private static final Yaml yamlTemplateData = new Yaml(new Constructor(TemplateData.class));
    private static final Yaml yamlSet = new Yaml(new Constructor(HashSet.class));
    
    private ConfigUtil() {
        // private
    }
    
    public static TemplateData getTemplateData(SourceProvider provider, ComposeData compose) {
        boolean hasFile = provider.hasFile("compose-template.yml");
        if ("0.0.1".equals(compose.version) && !hasFile) {
            return TemplateData.legacyFallback();
        } else if("0.0.1".equals(compose.version) && hasFile) {
            System.out.println(ansi().fgRed().a("The template is version >= 0.0.2, but the current repo is using 0.0.1!").reset());
            System.exit(1);
            return null;
        }
        TemplateData template = null;
        try {
            template = yamlTemplateData.load(provider.getStream("compose-template.yml"));
        } catch(IOException ex) {
            System.out
            .println(ansi().fgRed().a("Error while loading 'compose-template.yml' from the template repository!").reset());
            ex.printStackTrace();
            System.exit(1);
            return null;
        }
        if (!"0.0.2".equals(compose.version)) {
            System.out.println(ansi().fgRed().a("Incompatible template version defined! Please update gradle-compose!").reset());
            System.exit(1);
        }
        return template;
    }

    public static ComposeData loadLocalConfig() {
        File settingsFile = new File("gradle-compose.yml");
        if (!settingsFile.exists()) {
            System.out.println(ansi().fgRed().a("'" + settingsFile.getAbsolutePath() + "' not found!").reset());
            System.exit(1);
            return null;
        }
        ComposeData compose = null;
        try {
            compose = yamlComposeData.load(new FileInputStream(settingsFile));
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
            return null;
        }
        if (!("0.0.1".equals(compose.version) || "0.0.2".equals(compose.version))) {
            System.out.println(ansi().fgRed().a("Incompatible version defined! Please update gradle-compose!").reset());
            System.exit(1);
        }
        return compose;
    }
    
    public static Set<String> readCustomList(SourceProvider provider, String file) {
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
