/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.io.BufferedInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Classes {
    final List<String> jarPaths;

    public Classes() {
        this(System.getProperty("java.class.path").split("[" + System.getProperty("path.separator") + "]"));
    }

    public Classes(String... jarPaths) {
        this.jarPaths = Arrays.stream(jarPaths).filter(j -> j.endsWith(".jar")).toList();
    }

    public List<String> getDuplicatesClasses() {
        var classes = new TreeMap<String, ClassInfo>();
        for (String jarPath : jarPaths) {
            addClasses(jarPath, classes);
        }
        return classes.values().stream().filter(c -> c.paths.size() > 1).map(c -> c.toString()).toList();
    }

    public List<String> getJarsForClass(String className) {
        var classes = new TreeMap<String, ClassInfo>();
        for (String jarPath : jarPaths) {
            addClasses(jarPath, classes);
        }
        return classes.values().stream().filter(c -> c.name.contains(className)).map(c -> c.toString()).toList();
    }

    private void addClasses(String jarPath, Map<String, ClassInfo> classes) {
        try (ZipInputStream in =
                new ZipInputStream(new BufferedInputStream(Files.newInputStream(Paths.get(jarPath))))) {
            for (ZipEntry e = in.getNextEntry(); e != null; e = in.getNextEntry()) {
                String className = e.getName();
                if (!className.endsWith(".class"))
                    continue;
                ClassInfo info = classes.get(className);
                if (info == null)
                    classes.put(className, info = new ClassInfo(className));
                info.paths.add(Paths.get(jarPath).getFileName().toString());
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read jar path: " + jarPath, ex);
        }
    }

    static class ClassInfo {
        final String name;
        final Set<String> paths = new TreeSet<>();

        ClassInfo(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name + " " + paths;
        }
    }

}
