/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Setup for Rolling Tick and Time operations. The Window sizes for different cardinalities are designed to be similar
 * whether using tick or time, so that some level of comparison can be done. All operations are defined with the same
 * windows definitions.
 */
class Setup {
    final StandardTestRunner runner;

    Setup(StandardTestRunner runner) {
        this.runner = runner;
    }

    void factors(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.setScaleFactors(staticFactor, incFactor);
        runner.tables("timed");
    }

    void rollTime0Groups(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(ts_col="timestamp",cols=["Contains=num1"],rev_time="PT2S",fwd_time="PT3S")
        before_row = ${op}(ts_col="timestamp",cols=["Before=num1"],rev_time="PT2S",fwd_time="-PT1S")
        after_row = ${op}(ts_col="timestamp",cols=["After=num1"],rev_time="-PT1S",fwd_time="PT2S")
        """);
    }

    void rollTime1Group(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(ts_col="timestamp",cols=["Contains=num1"],rev_time="PT2S",fwd_time="PT3S")
        before_row = ${op}(ts_col="timestamp",cols=["Before=num1"],rev_time="PT2S",fwd_time="-PT1S")
        after_row = ${op}(ts_col="timestamp",cols=["After=num1"],rev_time="-PT1S",fwd_time="PT2S")   
        """);
    }

    void rollTime2Groups(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(ts_col="timestamp",cols=["Contains=num1"],rev_time="PT4M",fwd_time="PT5M")
        before_row = ${op}(ts_col="timestamp",cols=["Before=num1"],rev_time="PT3M",fwd_time="-PT1M")
        after_row = ${op}(ts_col="timestamp",cols=["After=num1"],rev_time="-PT1M",fwd_time="PT3M")
        """);
    }

    void rollTime3Groups(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(ts_col="timestamp",cols=["Contains=num1"],rev_time="PT40M",fwd_time="PT50M")
        before_row = ${op}(ts_col="timestamp",cols=["Before=num1"],rev_time="PT30M",fwd_time="-PT10M")
        after_row = ${op}(ts_col="timestamp",cols=["After=num1"],rev_time="-PT10M",fwd_time="PT30M")
        """);
    }

    void rollTick0Groups(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(cols=["Contains=num1"], rev_ticks=2000, fwd_ticks=3000)
        before_row = ${op}(cols=["Before=num1"], rev_ticks=2000, fwd_ticks=-1000)
        after_row = ${op}(cols=["After=num1"], rev_ticks=-1000, fwd_ticks=2000)
        """);
    }

    void rollTick1Group(String op) {
        addSetupQuery(op, """
        contains_row = ${op}(cols=["Contains=num1"], rev_ticks=20, fwd_ticks=30)
        before_row = ${op}(cols=["Before=num1"], rev_ticks=20, fwd_ticks=-10)
        after_row = ${op}(cols=["After=num1"], rev_ticks=-10, fwd_ticks=20)
        """);
    }

    void rollTick2Groups(String op) {
        rollTick1Group(op);
    }

    void rollTick3Groups(String op) {
        rollTick1Group(op);
    }

    void emTime0Groups(String op) {
        addSetupQuery(op, "dk=${op}(ts_col='timestamp',decay_time='PT5S',cols=['X=num1'])");
    }

    void emTime1Group(String op) {
        addSetupQuery(op, "dk=${op}(ts_col='timestamp',decay_time='PT5S',cols=['X=num1'])");
    }

    void emTime2Groups(String op) {
        addSetupQuery(op, "dk=${op}(ts_col='timestamp',decay_time='PT9M',cols=['X=num1'])");
    }

    void emTime3Groups(String op) {
        addSetupQuery(op, "dk=${op}(ts_col='timestamp',decay_time='PT90M',cols=['X=num1'])");
    }

    void emTick0Groups(String op) {
        addSetupQuery(op, "dk=${op}(decay_ticks=5000,cols=['X=num1'])");
    }

    void emTick1Group(String op) {
        addSetupQuery(op, "dk=${op}(decay_ticks=50,cols=['X=num1'])");
    }

    void emTick2Groups(String op) {
        emTick1Group(op);
    }

    void emTick3Groups(String op) {
        emTick1Group(op);
    }

    private void addSetupQuery(String op, String setup) {
        var s = """
        from deephaven.updateby import ${op}
        ${setup}
        """;
        s = s.replace("${setup}", setup);
        s = s.replace("${op}", op);
        runner.addSetupQuery(s);
    }

}
