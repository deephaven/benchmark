package io.deephaven.benchmark.tests.experimental.mergescale;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class NormalScaleTest {
    final ScaleTestRunner runner = new ScaleTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @Test  @Disabled
    public void normalSmallTable() {
        runner.runTest("Normal Small Table", "small", rowCount / 4, rowCount / 4);
    }
    
    @Test  @Disabled
    public void normalMediumTable() {
        runner.runTest("Normal Medium Table", "medium", rowCount / 2, rowCount / 2);
    }
    
    @Test
    public void normalLargeTable() {
        runner.runTest("Normal Large Table", "large", rowCount, rowCount);
    }

}
