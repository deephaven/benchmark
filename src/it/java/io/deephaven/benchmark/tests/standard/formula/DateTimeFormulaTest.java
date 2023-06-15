/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for using DateTimeUtil formulas in operations like update where the formula has an outsized
 * performance impact for the operation.
 */
public class DateTimeFormulaTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @Test
    public void now() {
        setup(6, 40, 5);
        var q = "source.update(formulas=['New1 = now()'])";
        runner.test("Now- now()", q, "int250");
    }

    @Test
    public void parseInstant() {
        setup(1, 1, 1);
        var q = "source.update(formulas=['New1 = parseInstant(`2023-05-31T04:52:14.001 ET`)'])";
        runner.test("ParseInstant- parseInstant(String)", q, "int250");
    }

    @Test
    public void parseDuration() {
        setup(3, 10, 2);
        var q = "source.update(formulas=['New1 = parseDuration(`PT4H52M14S`)'])";
        runner.test("ParseDuration- parseDuration(String)", q, "int250");
    }

    @Test
    public void parseLocalTime() {
        setup(3, 10, 1);
        var q = "source.update(formulas=['New1 = parseLocalTime(`04:52:14.001`)'])";
        runner.test("ParseLocalTime- parseLocalTime(String)", q, "int250");
    }

    @Test
    public void epochNanosToZonedDateTime() {
        setup(3, 8, 5);
        var q = "source.update(formulas=['New1 = epochNanosToZonedDateTime(1000000, java.time.ZoneId.systemDefault())'])";
        runner.test("EpochNanosToZonedDateTime- epochNanosToZonedDateTime(long, ZoneId)", q, "int250");
    }

    private void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);
    }

}
