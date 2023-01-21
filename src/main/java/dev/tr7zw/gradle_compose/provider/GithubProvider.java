package dev.tr7zw.gradle_compose.provider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import dev.tr7zw.gradle_compose.util.FileUtil;
import dev.tr7zw.gradle_compose.util.GithubUtil;
import dev.tr7zw.gradle_compose.util.SaveOnWriteHandler;


public class GithubProvider implements SourceProvider {

    private final String repoURL;
    private final File repoCache, cacheFile;
    private final LocalSourceProvider cacheProvider;
    private SaveOnWriteHandler<CacheContent> cache;

    public GithubProvider(String repoURL) {
        this.repoURL = repoURL;
        this.repoCache = new File(new File(System.getProperty("user.home") + "/.compose/cache"), repoURL.replace("https://raw.githubusercontent.com/", ""));
        this.cacheFile = new File(repoCache, ".composecache");
        this.cacheProvider = new LocalSourceProvider(repoCache);
        this.cache = new SaveOnWriteHandler<>(cacheFile, CacheContent.class);
        String version = GithubUtil.getSha(repoURL);
        if(!Objects.equals(version, cache.getHandle().lastVersion) || !cache.getHandle().done) {
            System.out.println("Cache outdated. Redownloading...");
            FileUtil.delete(repoCache);
            this.cache = new SaveOnWriteHandler<>(cacheFile, CacheContent.class);
            this.cache.getHandle().lastVersion = version;
        } else {
            System.out.println("Using local cache...");
        }
    }

    @Override
    public boolean hasFile(String path) {
        if(cacheProvider.hasFile(path)) {
            return true;
        }
        if(cache.getHandle().invalidFiles.contains(path)) {
            return false;
        }
        try {
            readFromURL(repoURL + path).close();
            return true;
        }catch(IOException ex) {
            cache.getHandle().invalidFiles.add(path);
            cache.save();
            return false;
        }
    }

    @Override
    public InputStream getStream(String path) throws FileNotFoundException {
        if(cacheProvider.hasFile(path)) {
            return cacheProvider.getStream(path);
        }
        try {
            File cacheTarget = new File(repoCache, path);
            cacheTarget.getParentFile().mkdirs();
            try(InputStream stream = readFromURL(repoURL + path)) {
                try(OutputStream out = new FileOutputStream(cacheTarget)){
                    copy(stream, out);
                }
            }
            return cacheProvider.getStream(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream readFromURL(String requestURL) throws IOException {
        return new URL(requestURL).openStream();
    }

    @Override
    public void markAsDone() {
        cache.getHandle().done = true;
        cache.save();
    }

    private void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) != -1) {
            target.write(buf, 0, length);
        }
    }
    
    public static class CacheContent {
        private String lastVersion = "";
        private Set<String> invalidFiles = new HashSet<>();
        private boolean done = false;
    }
    
}
