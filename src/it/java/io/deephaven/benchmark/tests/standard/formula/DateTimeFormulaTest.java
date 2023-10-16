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
        setup(6, 35, 15);
        var q = "source.update(formulas=['New1 = now()'])";
        runner.test("Now- now()", q, "int250");
    }

    @Test
    public void parseInstant() {
        setup(1, 1, 1);
        var q = "source.update(formulas=['New1 = parseInstant(`2023-05-31T04:52:14.001 ET`)'])";
        runner.test("ParseInstant- DateTime String with Timezone", q, "int250");
    }

    @Test
    public void parseDuration() {
        setup(2, 9, 1);
        var q = "source.update(formulas=['New1 = parseDuration(`PT4H52M14S`)'])";
        runner.test("ParseDuration- PT Duration String", q, "int250");
    }

    @Test
    public void parseLocalTime() {
        setup(2, 7, 1);
        var q = "source.update(formulas=['New1 = parseLocalTime(`04:52:14.001`)'])";
        runner.test("ParseLocalTime- Time String)", q, "int250");
    }

    @Test
    public void epochNanosToZonedDateTime() {
        setup(2, 11, 10);
        var q = """
        source.update(formulas=['New1=epochNanosToZonedDateTime(1697240421926014741L,java.time.ZoneOffset.UTC)'])
        """;
        runner.test("EpochNanosToZonedDateTime- Nanos Long and ZoneId", q, "int250");
    }

    private void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);
    }

}
