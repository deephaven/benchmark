package io.deephaven.benchmark.jfr;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import io.deephaven.benchmark.jfr.JfrReader;

@Disabled
public class TestJfrReader {
	
	@Test
	public void getEventNames() {
		JfrReader jfr = new JfrReader(getClass().getResource("server.jfr"));
		assertEquals("Wrong method names", "", jfr.getEventNames().toString());
	}

}
