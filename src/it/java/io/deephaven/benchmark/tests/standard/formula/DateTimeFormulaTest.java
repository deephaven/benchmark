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
        var q = "source.update(formulas=['New1 = now()'])"; // 0.25.0
        // var q = "source.update(formulas=['New1 = currentTime()'])"; // 0.24.2
        runner.test("Now- now()", q, "int250");
    }

    @Test
    public void parseInstant() {
        setup(1, 1, 1);
        var q = "source.update(formulas=['New1 = parseInstant(`2023-05-31T04:52:14.001 ET`)'])"; // 0.25.0
        // var q = "source.update(formulas=['New1 = convertDateTime(`2023-05-31T04:52:14.001 ET`)'])"; // 0.24.2
        runner.test("ParseInstant- parseInstant(String)", q, "int250");
    }

    @Test
    public void parseDuration() {
        setup(2, 5, 1);
        var q = "source.update(formulas=['New1 = parseDuration(`PT4H52M14S`)'])"; // 0.25.0
        // var q = "source.update(formulas=['New1 = convertPeriod(`T4H52M14S`)'])"; // 0.24.2
        runner.test("ParseDuration- parseDuration(String)", q, "int250");
    }

    @Test
    public void parseLocalTime() {
        setup(3, 10, 1);
        var q = "source.update(formulas=['New1 = parseLocalTime(`04:52:14.001`)'])"; // 0.25.0
        // var q = "source.update(formulas=['New1 = convertTime(`04:52:14.001`)'])"; // 0.24.2
        runner.test("ParseLocalTime- parseLocalTime(String)", q, "int250");
    }

    @Test
    public void epochNanosToZonedDateTime() {
        setup(3, 8, 5);
        var q = "source.update(formulas=['New1 = epochNanosToZonedDateTime(1000000, java.time.ZoneId.systemDefault())'])"; // 0.25.0
        // var q = "source.update(formulas=['New1 = makeZonedDateTime(1000000, java.time.ZoneId.systemDefault())'])"; //
        // 0.24.2
        runner.test("EpochNanosToZonedDateTime- epochNanosToZonedDateTime(long, ZoneId)", q, "int250");
    }

    private void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);
    }

}
