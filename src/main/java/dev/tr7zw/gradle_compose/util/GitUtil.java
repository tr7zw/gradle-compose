package dev.tr7zw.gradle_compose.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GitUtil {

    private GitUtil() {
        // private
    }

    public static boolean gitAvailable() {
        try {
            String[] command = new String[] { "git", "--version" };

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void runGitCommand(File dir, String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(dir);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            waitForExit(p);
            if (p.exitValue() != 0) {
                throw new RuntimeException("Error while running git command `" + String.join(" ", command) + "` in " + dir.getAbsolutePath());
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error while running git command `" + String.join(" ", command) + "` in " + dir.getAbsolutePath(),
                    e);
        }
    }

    public static String runGitCommandGetBranchName(File dir, String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(dir);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            String name = "upstream";
            while ((line = reader.readLine()) != null) {
                if (line.contains("upstream/")) {
                    name = line.split("/")[1].trim();
                }
            }
            waitForExit(p);
            return name;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean runGitCommandCheckFail(File dir, String[] command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(dir);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;

            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            waitForExit(p);
            return p.exitValue() != 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void waitForExit(Process p) {
        long start = System.currentTimeMillis();
        while (p.isAlive()) {
            if (start + 1000 * 60 < System.currentTimeMillis()) {
                throw new RuntimeException("The git process took too long! " + p.isAlive());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
