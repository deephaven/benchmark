package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.*;

public class ClassesTest {

	@Test
	public void getDuplicatesClasses() throws Exception {
		Classes c = new Classes(".", getLib("small-lib1.jar"), getLib("small-lib2.jar"), "target");
		assertEquals(
			"""
			io/deephaven/configuration/CacheDir.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/ConfigDir.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/Configuration$NullableConfiguration.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/Configuration.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/ConfigurationContext.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/ConfigurationException.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/ConfigurationScope.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/DataDir.class [small-lib1.jar, small-lib2.jar]
			io/deephaven/configuration/PropertyException.class [small-lib1.jar, small-lib2.jar]
			""",
			String.join("\n", c.getDuplicatesClasses()) + "\n",
			"Wrong duplicate list"
		);
	}
	
	@Test @Disabled
	public void getDuplicatesClassesOnClassPath() throws Exception {
		Classes c = new Classes();
		List<String> dupes = c.getDuplicatesClasses();
		System.out.println("Dupes: " + String.join("\n", dupes));
		
		List<String> jars = c.getJarsForClass("AbstractManagedChannelImplBuilder");
		System.out.println("Jars: " + String.join("\n", jars));
	}
	
	private String getLib(String name) throws Exception {
		return Paths.get(getClass().getResource(name).toURI()).toString();
	}
	
}
