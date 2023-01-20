package io.deephaven.benchmark.run;

import static org.junit.Assert.assertEquals;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;
import io.deephaven.benchmark.util.Filer;

public class ResultSummaryTest {
	
	@Test
	public void summarize() throws Exception {
		Path rootDir = Paths.get(getClass().getResource("findme.txt").toURI()).resolveSibling("test-data");
		var summary = new ResultSummary(rootDir);
		summary.summarize();
		assertEquals(
			"""
			run-id,name,timestamp,duration,test-rate
			1856590b0c0,Join animals and adjectives - Parquet Views,1672446259396,3.025,1111111.1
			1856590b0c0,Join animals and adjectives - Incremental Release,1672446262629,1.705,98619.32
			1856590b0c0,Count Records From Kakfa Stream,1672446264337,3.678,49603.17
			1856591d58e,Join animals and adjectives - Parquet Views,1672446334353,2.932,1204819.4
			1856591d58e,Join animals and adjectives - Incremental Release,1672446337481,1.092,210526.31
			1856591d58e,Count Records From Kakfa Stream,1672446338577,6.329,21654.396
			""".replace("\r", "").trim(),
			Filer.getFileText(summary.summaryFile));
	}
}
