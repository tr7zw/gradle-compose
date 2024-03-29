package dev.tr7zw.gradle_compose.provider;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface SourceProvider {

    public boolean hasFile(String path);
    
    public InputStream getStream(String path) throws FileNotFoundException;
    
    public default void markAsDone() {
        // nothing
    }
    
}
