/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ConsoleLauncherUtilTest {

    /**
     * Test formatting console launcher args with wildcards for test names and with tag. If there is no test name,
     * nothing should change.
     */
    @Test
    void formatConsoleWildcards() {
        var expected = new String[] {"-Db.p=my.pr", "-jar", "d-b-*-s.jar", "-cp", "s-t.jar", "-p", "my.test.package",
                "-n", "^.*[.](A.*)Test.*$"};
        var args = getArgs("my.test.package", "A*", "");
        var formatted = ConsoleLauncherUtil.formatConsoleWildcards(args);
        assertArrayEquals(expected, formatted, "Wrong formatted args");

        expected = new String[] {"-Db.p=my.pr", "-jar", "d-b-*-s.jar", "-cp", "s-t.jar", "-p", "my.test.package",
                "-n", "^.*[.](A.*|.*B|.*C.*)Test.*$"};
        args = getArgs("my.test.package", "A*,*B,*C*", "");
        formatted = ConsoleLauncherUtil.formatConsoleWildcards(args);
        assertArrayEquals(expected, formatted, "Wrong formatted args");

        expected = new String[] {"-Db.p=my.pr", "-jar", "d-b-*-s.jar", "-cp", "s-t.jar", "-p", "my.test.package",
                "-t", "TAG"};
        args = getArgs("my.test.package", "", "TAG");
        formatted = ConsoleLauncherUtil.formatConsoleWildcards(args);
        assertArrayEquals(expected, formatted, "Wrong formatted args");

        expected = new String[] {"-Db.p=my.pr", "-jar", "d-b-*-s.jar", "-cp", "s-t.jar", "-p", "my.test.package",
                "-n", "^.*[.](.*)Test.*$"};
        args = getArgs("my.test.package", "*", "");
        formatted = ConsoleLauncherUtil.formatConsoleWildcards(args);
        assertArrayEquals(expected, formatted, "Wrong formatted args");
    }

    private String[] getArgs(String testPackage, String testPattern, String tagName) {
        var opts = "-Db.p=my.pr -jar d-b-*-s.jar -cp s-t.jar";
        var args = opts + " -p " + testPackage;
        if (!testPattern.isBlank())
            args += " -n " + testPattern;
        if (!tagName.isBlank())
            args += " -t " + tagName;
        System.out.println("Args: " + args);
        return args.trim().split("\\s+");
    }

}
