package dev.tr7zw.gradle_compose.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class SaveOnWriteHandler<T> {

    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    private final File file;
    private T handle;
    
    public SaveOnWriteHandler(File file, Class<T> target) {
        this.file = file;
        if(file.exists()) {
            try {
                handle = gson.fromJson(new FileReader(file), target);
            } catch (JsonSyntaxException | JsonIOException | FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        if(handle == null) {
            try {
                handle = target.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Unable to create instance!", e);
            }
        }
    }
    
    public void save() {
        try {
            file.getParentFile().mkdirs();
            Files.write(file.toPath(), gson.toJson(handle).getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error while writing cache", e);
        }
    }
    
    public T getHandle() {
        return handle;
    }
    
}
