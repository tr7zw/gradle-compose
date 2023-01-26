package dev.tr7zw.gradle_compose.util;

import static org.fusesource.jansi.Ansi.ansi;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import dev.tr7zw.gradle_compose.ComposeData;
import dev.tr7zw.gradle_compose.provider.GithubProvider;
import dev.tr7zw.gradle_compose.provider.LocalSourceProvider;
import dev.tr7zw.gradle_compose.provider.SourceProvider;

public final class FileUtil {

    private FileUtil() {
        // private
    }
    
    public static void delete(File file) {
        if(file.isDirectory()) {
            for(File f : file.listFiles())
                delete(f);
        }
        file.delete();
    }
    
    public static SourceProvider getProvider(ComposeData data) {
        if (data.source == null) {
            System.out.println(ansi().fgRed().a("No source defined!").reset());
            System.exit(1);
        }
        if (data.source.toLowerCase().startsWith("https://github.com/")) {
            String url = data.source.toLowerCase().replace("https://github.com", "https://raw.githubusercontent.com");
            if (!url.endsWith("/")) {
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
    
    public static void copyIfAvailable(SourceProvider provider, String file) {
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
    
    public static void copyIfAvailableWithReplacments(SourceProvider provider, File baseDir, String path, String file,
            Map<String, String> replacement, Set<String> availableTags, Set<String> enabledTags) {
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
            data = FileProcessingUtil.applyReplacements(data, replacement);
            data = FileProcessingUtil.processTags(data, availableTags, enabledTags);
            data = FileProcessingUtil.cleanFile(target.getName(), data);
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
    
}
