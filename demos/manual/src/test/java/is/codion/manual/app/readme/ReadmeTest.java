/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.manual.app.readme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The readme can not include source files, GitHub does not support it, so its java blocks are
 * copies. Each one is preceded by a {@code // readme-source:} comment naming the file it was
 * copied from, and this test verifies that every line of the block is still to be found there.
 */
public final class ReadmeTest {

	private static final String MARKER = "// readme-source: ";
	private static final String JAVA_BLOCK = "[source,java]";
	private static final String FENCE = "----";

	@Test
	void readmeBlocksMatchTheirSource() throws IOException {
		Path readme = readme();
		List<String> lines = Files.readAllLines(readme);
		Path sourceRoot = readme.getParent().resolve("demos/manual/src");
		int blocks = 0;
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).startsWith(MARKER)) {
				Path source = sourceRoot.resolve(lines.get(i).substring(MARKER.length()).trim());
				assertTrue(Files.exists(source), "No such readme source: " + source);
				assertBlockFound(block(lines, i), Files.readAllLines(source), source, i + 1);
				blocks++;
			}
		}
		assertTrue(blocks > 0, "No " + MARKER.trim() + " markers found in " + readme);
	}

	private static void assertBlockFound(List<String> block, List<String> source, Path sourcePath, int line) {
		List<String> sourceLines = normalize(source);
		List<String> missing = normalize(block).stream()
						.filter(blockLine -> !sourceLines.contains(blockLine))
						.collect(toList());
		if (!missing.isEmpty()) {
			fail("readme.adoc:" + line + " has drifted from " + sourcePath.getFileName()
							+ ", these lines are no longer to be found there:\n  " + String.join("\n  ", missing));
		}
	}

	private static List<String> block(List<String> lines, int markerIndex) {
		int start = markerIndex;
		while (!lines.get(start).trim().equals(JAVA_BLOCK)) {
			start++;
		}
		start += 2;// the [source,java] line and the opening fence
		List<String> block = new ArrayList<>();
		for (int i = start; !lines.get(i).trim().equals(FENCE); i++) {
			block.add(lines.get(i));
		}

		return block;
	}

	/**
	 * @return the given lines, trimmed, with comments, blank lines and package declarations removed
	 */
	private static List<String> normalize(List<String> lines) {
		return lines.stream()
						.map(line -> line.trim().replaceAll("\\s+", " "))
						.filter(line -> !line.isEmpty())
						.filter(line -> !line.startsWith("//") && !line.startsWith("*") && !line.startsWith("/*"))
						.filter(line -> !line.startsWith("package "))
						.collect(toList());
	}

	private static Path readme() {
		Path directory = Paths.get("").toAbsolutePath();
		while (directory != null && !Files.exists(directory.resolve("readme.adoc"))) {
			directory = directory.getParent();
		}
		if (directory == null) {
			throw new IllegalStateException("readme.adoc not found");
		}

		return directory.resolve("readme.adoc");
	}
}
