package se.forsman.bevakning.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class FileJsonStore {
    private FileJsonStore() {
    }

    public static String readFileOrResource(String path, String fallbackResource) {
        File file = new File(path);
        if (file.exists()) {
            return readFile(path);
        }
        return readResource(fallbackResource);
    }

    public static String readFile(String path) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte läsa fil: " + path, e);
        }
        return sb.toString();
    }

    public static void writeFile(String path, String content) {
        File file = new File(path);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(content);
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte skriva fil: " + path, e);
        }
    }

    public static void copyResourceToFileIfMissing(String resourcePath, String targetPath) {
        File file = new File(targetPath);
        if (file.exists()) {
            return;
        }
        writeFile(targetPath, readResource(resourcePath));
    }

    private static String readResource(String resourcePath) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Kunde inte hitta resource: " + resourcePath);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte läsa resource: " + resourcePath, e);
        }
        return sb.toString();
    }
}
