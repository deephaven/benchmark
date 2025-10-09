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
        setup(50, 4, 2);
        var q = "source.update(formulas=['New1 = now()'])";
        runner.test("Now- now()", q);
    }

    @Test
    public void parseInstant() {
        setup(1, 1, 1);
        var q = "source.update(formulas=['New1 = parseInstant(`2023-05-31T04:52:14.001 ET`)'])";
        runner.test("ParseInstant- DateTime String with Timezone", q);
    }

    @Test
    public void parseDuration() {
        setup(3, 6, 1);
        var q = "source.update(formulas=['New1 = parseDuration(`PT4H52M14S`)'])";
        runner.test("ParseDuration- PT Duration String", q);
    }

    @Test
    public void parseLocalTime() {
        setup(3, 5, 1);
        var q = "source.update(formulas=['New1 = parseLocalTime(`04:52:14.001`)'])";
        runner.test("ParseLocalTime- Time String", q);
    }

    @Test
    @Tag("Iterate")
    public void epochNanosToZonedDateTime() {
        setup(24, 1, 1);
        var q = """
        source.update(formulas=['New1=epochNanosToZonedDateTime(1697240421926014741L,java.time.ZoneOffset.UTC)'])
        """;
        runner.test("EpochNanosToZonedDateTime- Nanos Long and ZoneId", q);
    }

    private void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.setScaleFactors(staticFactor, incFactor);
    }

}
