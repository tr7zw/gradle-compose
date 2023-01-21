package dev.tr7zw.gradle_compose.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class GithubUtil {

    private static Gson gson = new Gson();

    private GithubUtil() {
        // private
    }

    public static String getSha(String url) {
        GitTrees gitTree;
        try {
            gitTree = gson.fromJson(new InputStreamReader(new URL(
                    "https://api.github.com/repos/" + getOrg(url) + "/" + getRepo(url) + "/git/trees/" + getBranch(url))
                            .openStream()),
                    GitTrees.class);
            return gitTree.sha;
        } catch (JsonSyntaxException | JsonIOException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getOrg(String url) {
        url = url.substring(url.indexOf(".com/") + 5);
        return url.split("/")[0];
    }

    public static String getRepo(String url) {
        url = url.substring(url.indexOf(".com/") + 5);
        return url.split("/")[1];
    }

    public static String getBranch(String url) {
        url = url.substring(url.indexOf(".com/") + 5);
        String[] args = url.split("/");
        if (args.length < 3) {
            return "HEAD";
        }
        return args[2];
    }

    private static class GitTrees {
        String sha;
    }
}
