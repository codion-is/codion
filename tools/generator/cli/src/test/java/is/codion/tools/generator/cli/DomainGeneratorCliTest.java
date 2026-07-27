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
package is.codion.tools.generator.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public final class DomainGeneratorCliTest {

	private static final String USER = System.getProperty("codion.test.user", "scott:tiger");
	private static final String PACKAGE = "is.codion.test.domain";
	private static final String[] STORE = {"--user", USER, "--schema", "STORE", "--package", PACKAGE};

	private final ByteArrayOutputStream out = new ByteArrayOutputStream();
	private final ByteArrayOutputStream err = new ByteArrayOutputStream();

	@Test
	void combinedToStandardOutput() {
		assertEquals(0, run(STORE));
		String source = out();
		assertTrue(source.contains("package " + PACKAGE + ";"));
		assertTrue(source.contains("public final class Store extends DomainModel"));
		assertTrue(source.contains("public interface Category"));
		assertTrue(source.contains("public interface Item"));
		assertFalse(source.contains("record Dto"));
		assertTrue(err().contains("Generated 2 entities from schema STORE"));
	}

	@Test
	void dtos() {
		assertEquals(0, run(arguments("--dtos")));
		assertTrue(out().contains("record Dto"));
	}

	@Test
	void combined(@TempDir Path outputDir) throws Exception {
		assertEquals(0, run(arguments("--output-dir", outputDir.toString())));
		Path domainFile = outputDir.resolve("is/codion/test/domain/Store.java");
		assertTrue(Files.exists(domainFile));
		assertTrue(Files.readString(domainFile).contains("public final class Store extends DomainModel"));
	}

	@Test
	void overwrite(@TempDir Path outputDir) {
		assertEquals(0, run(arguments("--output-dir", outputDir.toString())));
		// A second run refuses to replace the existing source
		assertEquals(1, run(arguments("--output-dir", outputDir.toString())));
		assertTrue(err().contains("--overwrite"));
		assertEquals(0, run(arguments("--output-dir", outputDir.toString(), "--overwrite")));
	}

	@Test
	void splitApiImpl(@TempDir Path outputDir) {
		assertEquals(0, run(arguments("--output-dir", outputDir.toString(), "--split-api-impl")));
		assertTrue(Files.exists(outputDir.resolve("is/codion/test/domain/api/Store.java")));
		assertTrue(Files.exists(outputDir.resolve("is/codion/test/domain/StoreImpl.java")));
	}

	@Test
	void i18nAndTest(@TempDir Path directory) {
		Path outputDir = directory.resolve("java");
		Path resourceDir = directory.resolve("resources");
		Path testDir = directory.resolve("test");
		assertEquals(0, run(arguments("--output-dir", outputDir.toString(),
						"--resource-dir", resourceDir.toString(), "--i18n",
						"--test-dir", testDir.toString(), "--test")));
		assertTrue(Files.exists(outputDir.resolve("is/codion/test/domain/Store.java")));
		assertTrue(Files.exists(resourceDir.resolve("is/codion/test/domain/Store$Category.properties")));
		assertTrue(Files.exists(testDir.resolve("is/codion/test/domain/StoreTest.java")));
	}

	@Test
	void schemaNotFound() {
		assertEquals(1, run("--user", USER, "--schema", "NONE", "--package", PACKAGE));
		assertTrue(err().contains("Schema not found: NONE"));
	}

	@Test
	void emptySchema() {
		assertEquals(1, run("--user", USER, "--schema", "EMPTY", "--package", PACKAGE));
		assertTrue(err().contains("No tables found in schema: EMPTY"));
	}

	@Test
	void help() {
		assertEquals(0, run("--help"));
		assertTrue(out().contains("Usage:"));
	}

	@Test
	void usageErrors(@TempDir Path outputDir) {
		assertEquals(2, run("--user", USER, "--package", PACKAGE));
		assertTrue(err().contains("--schema"));

		assertEquals(2, run("--user", USER, "--schema", "STORE"));
		assertTrue(err().contains("--package"));

		assertEquals(2, run(arguments("--unknown")));
		assertTrue(err().contains("Unknown option: --unknown"));

		assertEquals(2, run("--user", USER, "--schema", "STORE", "--package"));
		assertTrue(err().contains("Missing value for option: --package"));

		// The i18n properties and the unit test are additional files, standard output can not represent them
		assertEquals(2, run(arguments("--i18n")));
		assertEquals(2, run(arguments("--test")));
		// and they require their own output directories
		assertEquals(2, run(arguments("--output-dir", outputDir.toString(), "--i18n")));
		assertEquals(2, run(arguments("--output-dir", outputDir.toString(), "--test")));
	}

	private int run(String... arguments) {
		out.reset();
		err.reset();

		return DomainGeneratorCli.run(arguments, new PrintStream(out, true, UTF_8), new PrintStream(err, true, UTF_8));
	}

	private String out() {
		return out.toString(UTF_8);
	}

	private String err() {
		return err.toString(UTF_8);
	}

	private static String[] arguments(String... additional) {
		String[] arguments = new String[STORE.length + additional.length];
		System.arraycopy(STORE, 0, arguments, 0, STORE.length);
		System.arraycopy(additional, 0, arguments, STORE.length, additional.length);

		return arguments;
	}
}
