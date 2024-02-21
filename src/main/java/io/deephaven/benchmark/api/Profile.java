/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.InputStream;
import java.net.URI;
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

    /**
     * Initialize the profile from the supplied benchmark.profile property. That that property is absent, use the
     * default properties file.
     */
    Profile() {
        this(System.getProperty("benchmark.profile", "default.properties"));
    }

    /**
     * Initialize the profile from the given property file. Search through common locations like Http, current
     * directory, jar resources, etc to find the file.
     * 
     * @param profileUri the profile property file URI
     */
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

    /**
     * Return whether or not the given property name is defined in the profile.
     * 
     * @param name a property name
     * @return true if the property name is not empty, otherwise false
     */
    boolean isPropertyDefined(String name) {
        return !property(name, "").isBlank();
    }

    /**
     * Get the value of the given property name, or a default value if it is missing.
     * 
     * @param name a property name
     * @param defaultValue the value to use if the property is undefined
     * @return the property value or default
     */
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

    /**
     * Get the value of the given property name as a long.
     * 
     * @param name a property name
     * @param defaultValue the value to use if the property is undefined
     * @return the property value or default
     */
    long propertyAsIntegral(String name, String defaultValue) {
        String value = property(name, defaultValue);
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            throw new RuntimeException("Bad integral value: " + name + "=" + value);
        }
    }

    /**
     * Get the value of the given property name as a boolean
     * 
     * @param name a property name
     * @param defaultValue the value to use if the property is undefined
     * @return the property value or default
     */
    boolean propertyAsBoolean(String name, String defaultValue) {
        String value = property(name, defaultValue).toLowerCase();
        if (value.equals("true") || value.equals("false"))
            return Boolean.valueOf(value);
        throw new RuntimeException("Bad boolean value: " + name + "=" + value);
    }

    /**
     * Get the value of the given property name as a Duration. Supported unit types are; nano, milli, second, minute.
     * Fractional amounts are not supported.
     * <p/>
     * ex. 100000 nanos, 10 millis, 1 second, 5 minutes
     * 
     * @param name a property name
     * @param defaultValue the value to use if the property is undefined
     * @return the property value or default as a Duration
     */
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

    /**
     * Replace property descriptors (ex. ${my.property}) in the given string using values in this profile. Properties
     * existing the given string that match no profile property are ignored.
     * 
     * @param str the string containing properties to replace
     * @return the string with any matched properties
     */
    String replaceProperties(String str) {
        for (Map.Entry<Object, Object> e : props.entrySet()) {
            str = str.replace("${" + e.getKey() + "}", e.getValue().toString());
        }
        return str;
    }

    /**
     * Return a copy of the properties loaded into the profile
     * 
     * @return a copy of the profile properties
     */
    Properties getProperties() {
        return (Properties) props.clone();
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
            return (uri.matches("^[A-Za-z][:][/].*")) ? new URI(uri).toURL() : null;
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
