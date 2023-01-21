package dev.tr7zw.gradle_compose;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GithubProvider implements SourceProvider {

    private final String repoURL;

    @Override
    public boolean hasFile(String path) {
        try {
            readStringFromURL(repoURL + path).close();
            return true;
        }catch(IOException ex) {
            return false;
        }
    }

    @Override
    public InputStream getStream(String path) throws FileNotFoundException {
        try {
            return readStringFromURL(repoURL + path);
        } catch (IOException e) {
            throw new FileNotFoundException();
        }
    }

    private InputStream readStringFromURL(String requestURL) throws IOException {
        return new URL(requestURL).openStream();
    }

}
