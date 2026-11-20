/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the join table operation. The output table contains all of the rows and columns of the left table
 * plus additional columns containing data from the right table. For columns appended to the left table, row values
 * equal the row values from the right table where the key values in the left and right tables are equal. If there is no
 * matching key in the right table, appended row values are NULL. If there are multiple matches, the operation will
 * fail.
 */
public class JoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setMaxRowFactor(3);
        runner.tables("source", "right");
    }

    @Test
    void joinOn1Col() {
        runner.setScaleFactors(2.8, 0.5);
        var q = "source.join(right, on=['key5 = r_key5'])";
        runner.test("Join- Join On 1 Col", q, "key5", "num1");
    }

    @Test
    void joinOn2Cols() {
        runner.setScaleFactors(11, 1);
        var q = "source.join(right, on=['key1 = r_wild', 'key2 = r_key2'])";
        runner.test("Join- Join On 2 Cols", q, "key1", "key2", "num1");
    }

    @Test
    void joinOn3Cols() {
        runner.setScaleFactors(8.8, 0.9);
        var q = "source.join(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 = r_key1'])";
        runner.test("Join- Join On 3 Cols", q, "key1", "key2", "num1");
    }

    /** Twin of {@link #joinOn2ColsAfterLastBy()}. first_by sends the join adds only. */
    @Test
    void joinOn2ColsAfterFirstBy() {
        runner.setScaleFactors(3, 3);
        var q = "source.first_by(by=['key5'])" + JOIN_SELECT_RIGHT_COL;
        runner.test("Join- Select Right Col After FirstBy", q, "key5", "key1", "key2", "num1");
    }

    /** Twin of {@link #joinOn2ColsAfterFirstBy()}. last_by sends the join modifies, including join key changes. */
    @Test
    void joinOn2ColsAfterLastBy() {
        runner.setScaleFactors(3, 3);
        var q = "source.last_by(by=['key5'])" + JOIN_SELECT_RIGHT_COL;
        runner.test("Join- Select Right Col After LastBy", q, "key5", "key1", "key2", "num1");
    }

    // Reads one right column through the join's row-to-slot lookup. One int column: a ticking select stores a
    // 1024-slot block per left row (~4 GB at 1M left rows)
    static final String JOIN_SELECT_RIGHT_COL =
            ".join(right, on=['key1 = r_wild', 'key2 = r_key2'], joins=['r_key4']).select(['r_key4'])";

}
