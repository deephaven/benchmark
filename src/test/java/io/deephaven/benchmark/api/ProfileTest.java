package io.deephaven.benchmark.api;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import io.deephaven.benchmark.api.Profile;


public class ProfileTest {
	
	@Test
	public void property() {
		Profile profile = new Profile("test-profile.properties");
		
		assertEquals("localhost:8080", profile.property("test.host", "n/a"), "Wrong host value");
		assertEquals("1 nanos", profile.property("test.duration1", "n/a"), "Wrong nano str value");
		assertEquals(1, profile.propertyAsDuration("test.duration1", "n/a").toNanos(), "Wrong nano value");
		assertEquals(2, profile.propertyAsDuration("test.duration2", "n/a").toMillis(), "Wrong millis value");
		assertEquals(3, profile.propertyAsDuration("test.duration3", "n/a").toSeconds(), "Wrong seconds value");
		assertEquals(4, profile.propertyAsDuration("test.duration4", "n/a").toMinutes(), "Wrong minutes value");
		
		assertEquals(System.getProperty("user.dir"), profile.property("user.dir", "n/a"), "Wrong profile name");
		assertEquals(11, profile.propertyAsDuration("this.prop.is.not.anywhere", "11 nanos").toNanos(), "Wrong default value");
	}
	
}
