/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        args = getArgs("my.test.package", "A*, *B ,*C*", "");
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

    /**
     * Test that the execute subcommand is prepended only when no subcommand is given
     */
    @Test
    void addDefaultCommand() {
        var args = new String[] {"-cp", "s-t.jar", "-p", "my.test.package"};
        var expected = new String[] {"execute", "-cp", "s-t.jar", "-p", "my.test.package"};
        assertArrayEquals(expected, ConsoleLauncherUtil.addDefaultCommand(args), "Wrong default command");

        assertArrayEquals(new String[] {"execute"}, ConsoleLauncherUtil.addDefaultCommand(new String[0]));

        args = new String[] {"discover", "-p", "my.test.package"};
        assertArrayEquals(args, ConsoleLauncherUtil.addDefaultCommand(args), "Wrong explicit command");
    }

    private String[] getArgs(String testPackage, String testPattern, String tagName) {
        var args = new ArrayList<String>(Arrays.asList());
        append(args, "-Db.p=my.pr", "-jar", "d-b-*-s.jar", "-cp", "s-t.jar");
        append(args, "-p", testPackage);
        if (!testPattern.isBlank())
            append(args, "-n", testPattern);
        if (!tagName.isBlank())
            append(args, "-t", tagName);
        System.out.println("Args: " + args);
        return args.toArray(new String[0]);
    }

    private void append(List<String> list, String... s) {
        list.addAll(Arrays.asList(s));
    }

}
