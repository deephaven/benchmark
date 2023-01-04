package io.deephaven.verify.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public class Filer {
	
	/**
	 * Delete the given directory recursively
	 * @param dir the directory to delete
	 */
	static public void deleteAll(Path dir) {
		try {
			if(!Files.exists(dir)) return;
			Files.walk(dir)
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile).forEach(File::delete);
		} catch(Exception ex) {
			throw new RuntimeException("Failed to delete directory: " + dir);
		}
	}
	
	/**
	 * Read the text of a file while preserving newlines and getting rid of carriage returns
	 * @param file the file to read
	 * @return the text of the file trimmed and carriage-return-less
	 */
	static public String getFileText(Path file) {
		try {
			return new String(Files.readAllBytes(file)).replace("\r", "").trim();
		} catch(IOException ex) {
			throw new RuntimeException("Failed to get text contents of file: " + file, ex);
		}
	}
	
}
