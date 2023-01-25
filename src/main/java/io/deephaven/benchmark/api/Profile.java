/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import io.deephaven.benchmark.run.BenchmarkMain;
import io.deephaven.benchmark.util.Log;

/**
 * Represents properties for a the Benchmark API .profile file in addition to allowing retrieval of System and
 * environment properties.
 */
class Profile {
    final private URL url;
    final private Properties props;

    Profile() {
        this(System.getProperty("benchmark.profile", "default.properties"));
    }

    Profile(String profileUri) {
        this.url = findProfileUrl(profileUri);
        this.props = new Properties();
        try (InputStream in = url.openStream()) {
            props.load(in);
            Log.info("Profile: %s", url);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to load profile: " + url);
        }
    }

    boolean isPropertyDefined(String name) {
        return !property(name, "").isBlank();
    }

    String property(String name, String defaultValue) {
        if (defaultValue == null)
            throw new RuntimeException("defaultValue is required for property: " + name);
        String value = props.getProperty(name);
        if (isBlank(value))
            value = System.getProperty(name);
        if (isBlank(value))
            value = System.getenv(name);
        return isBlank(value) ? defaultValue.trim() : value.trim();
    }

    long propertyAsIntegral(String name, String defaultValue) {
        String value = property(name, defaultValue);
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            throw new RuntimeException("Bad integral value: " + name + "=" + value);
        }
    }

    boolean propertyAsBoolean(String name, String defaultValue) {
        String value = property(name, defaultValue).toLowerCase();
        if (value.equals("true") || value.equals("false"))
            return Boolean.valueOf(value);
        throw new RuntimeException("Bad boolean value: " + name + "=" + value);
    }

    Duration propertyAsDuration(String name, String defaultValue) {
        String value = property(name, defaultValue);
        String timeStr = "([0-9]+)(.*)";
        if (!value.matches(timeStr))
            throw new RuntimeException("Bad duration value: " + name + "=" + value);
        String[] split = value.replaceAll(timeStr, "$1,$2").split(",");

        int amount;
        try {
            amount = Integer.parseInt(split[0].trim());
        } catch (Exception ex) {
            throw new RuntimeException("Bad duration amount: " + name + "=" + value);
        }

        switch (split[1].trim()) {
            case "nanos", "nano":
                return Duration.ofNanos(amount);
            case "millis", "milli":
                return Duration.ofMillis(amount);
            case "seconds", "second":
                return Duration.ofSeconds(amount);
            case "minutes", "minute":
                return Duration.ofMinutes(amount);
            default:
                throw new RuntimeException("Bad duration time type: " + name + "=" + value);
        }
    }

    String replaceProperties(String str) {
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            str = str.replace("${" + e.getKey() + "}", e.getValue().toString());
        }
        return str;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private URL findProfileUrl(String uri) {
        uri = uri.trim().replace('\\', '/');

        URL url = findProfileAsHttp(uri);
        if (url == null)
            url = findProfileAsAbsolutePath(uri);
        if (url == null)
            url = findProfileInCurrentDir(uri);
        if (url == null)
            url = findProfileAsResource(BenchmarkMain.class, "profile/" + uri);
        if (url == null)
            url = findProfileAsResource(getClass(), uri);
        if (url == null)
            throw new RuntimeException("Failed to find profile: " + uri);
        return url;
    }

    private URL findProfileAsHttp(String uri) {
        try {
            return (uri.matches("^[A-Za-z][:][/].*")) ? new URL(uri) : null;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find remote profile: " + uri);
        }
    }

    private URL findProfileAsAbsolutePath(String uri) {
        try {
            return uri.startsWith("/") ? Paths.get(uri).toUri().toURL() : null;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find file profile: " + uri);
        }
    }

    private URL findProfileInCurrentDir(String uri) {
        try {
            Path profile = Paths.get(".", uri);
            return profile.toFile().exists() ? profile.toUri().toURL() : null;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find relative profile: " + uri);
        }
    }

    private URL findProfileAsResource(Class<?> parentClass, String uri) {
        try {
            return parentClass.getResource(uri);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to find resource profile: " + uri);
        }
    }

}
