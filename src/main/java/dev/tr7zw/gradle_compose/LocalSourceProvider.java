package dev.tr7zw.gradle_compose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LocalSourceProvider implements SourceProvider{

    private final File folder;

    public boolean hasFile(String path) {
        return new File(folder, path).exists();
    }

    public InputStream getStream(String path) throws FileNotFoundException {
        return new FileInputStream(new File(folder, path));
    }
    
}
