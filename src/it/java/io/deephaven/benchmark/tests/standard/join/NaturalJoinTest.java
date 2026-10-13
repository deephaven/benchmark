/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the natural_join table operation. Column values will be added to each left row from exactly one
 * matched row from the right table or null values if no match
 */
public class NaturalJoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source", "right");
    }

    @Test
    void NaturalJoinOn1Col() {
        setup(2);
        var q = "source.natural_join(right, on=['key5 = r_key5'])";
        runner.test("NaturalJoin- Join On 1 Col", q, "key5", "num1");
    }

    @Test
    void NaturalJoinOn2Cols() {
        setup(6);
        var q = "source.natural_join(right, on=['key1 = r_wild', 'key2 = r_key2'])";
        runner.test("NaturalJoin- Join On 2 Cols", q, "key1", "key2", "num1");
    }
    
    @Test
    void NaturalJoinOn3Cols() {
        setup(6);
        var q = "source.natural_join(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 = r_key1'])";
        runner.test("NaturalJoin- Join On 3 Cols", q, "key1", "key2", "num1");
    }

    /**
     * Twin of {@link #NaturalJoinSelectStridedLeft()} with the rows flattened, so the join uses an array redirection.
     * Inc starts with some rows released, since the join picks its redirection when built.
     */
    @Test
    void NaturalJoinSelectFlatLeft() {
        setup(6);
        runner.addSetupQuery("source = source.where(['" + STRIDE_FILTER + "']).select()");
        runner.setInitialFactor(INITIAL_FACTOR);
        runner.test("NaturalJoin- Select Right Col Flat Left", JOIN_SELECT_RIGHT_COL, "key5");
    }

    /**
     * Twin of {@link #NaturalJoinSelectFlatLeft()} with strided row keys, so the join uses the hash-backed
     * <code>WritableRowRedirectionLockFree</code>.
     */
    @Test
    void NaturalJoinSelectStridedLeft() {
        setup(6);
        runner.addSetupQuery("source = source.where(['" + STRIDE_FILTER + "'])");
        runner.setInitialFactor(INITIAL_FACTOR);
        runner.test("NaturalJoin- Select Right Col Strided Left", JOIN_SELECT_RIGHT_COL, "key5");
    }

    // 5x sparse overhead; the hash redirection needs more than 4x
    static final String STRIDE_FILTER = "(int)(ii / 128) % 5 == 0";

    // Enough strided rows (over ~1K) for the join to see the stride when built; 1.2M at a 10M scale
    static final double INITIAL_FACTOR = 0.02;

    // Reads one non-key right column through the join's redirection
    static final String JOIN_SELECT_RIGHT_COL =
            "source.natural_join(right, on=['key5 = r_key5'], joins=['r_key4']).select(['r_key4'])";

}
