package io.deephaven.benchmark.tests.experimental.mergescale;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class MergeScaleTest {
    final ScaleTestRunner runner = new ScaleTestRunner(this);
    final long baseRowCount = runner.scaleRowCount / 8;
    
    @Test @Disabled
    public void mergeSmallTable() {
        runner.runTest("Merged Small Table", "small", baseRowCount, runner.scaleRowCount / 4);
    }
    
    @Test @Disabled
    public void mergeMediumTable() {
        runner.runTest("Merged Medium Table", "medium", baseRowCount, runner.scaleRowCount / 2);
    }
    
    @Test
    public void mergeLargeTable() {
        runner.runTest("Merged Large Table", "large", baseRowCount, runner.scaleRowCount);
    }

}
