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
package is.codion.common.rmi.server;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static is.codion.common.rmi.server.SerializationFilterFactory.SERIALIZATION_FILTER_PATTERN_FILE;
import static org.junit.jupiter.api.Assertions.*;

public final class SerializationFilterFactoryTest {

	@Test
	void patternFileWithBlankLines() throws IOException {
		File patternFile = File.createTempFile("serialization-filter", ".txt");
		patternFile.deleteOnExit();
		Files.write(patternFile.toPath(), Arrays.asList(
						"# a comment",
						"",
						"is.codion.**",
						"   ",
						"java.util.**",
						""), StandardCharsets.UTF_8);
		SERIALIZATION_FILTER_PATTERN_FILE.set(patternFile.getAbsolutePath());
		try {
			//a blank line must not join into an empty pattern element, createFilter rejects those
			String patterns = SerializationFilterFactory.createPatterns();
			assertFalse(patterns.contains(";;"), patterns);
			assertTrue(patterns.contains("is.codion.**"));
			assertTrue(patterns.contains("java.util.**"));
			assertNotNull(new SerializationFilterFactory().create());
		}
		finally {
			SERIALIZATION_FILTER_PATTERN_FILE.clear();
		}
	}
}
