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
package is.codion.dbms.sqlite;

import is.codion.dbms.sqlite.modulepath.ModulePathConnection;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The dialect on the module path, where it reaches the driver's classes, to register {@code upper()} and
 * {@code lower()}, only with the driver a module it reads, a connection refused otherwise. Each case in a JVM of its own, by the class path of this
 * one, as Gradle runs the tests, and skipped where that is not laid out so.
 */
public final class SQLiteModulePathTest {

	private static final String DRIVER = "sqlite-jdbc";
	private static final String MAIN_CLASSES = "classes" + File.separator + "java" + File.separator + "main";
	private static final String TEST_CLASSES = "classes" + File.separator + "java" + File.separator + "test";

	@Test
	void driverAModule() throws Exception {
		// resolved as a provider of java.sql.Driver, its module then read by the dialect's, which requires it statically
		assertEquals(asList("dialect: is.codion.dbms.sqlite", "driver: org.xerial.sqlitejdbc",
						"upper: ÞÓRSMÖRK", "like: 0", "upper: ÞÓRSMÖRK", "like: 0"), connect(true));
	}

	@Test
	void driverOnTheClassPath() throws Exception {
		// the driver's classes not accessible to the dialect's module: each connection refused, naming the module the
		// driver belongs on the module path as, rather than made with SQLite's own upper() and lower()
		assertEquals(asList("dialect: is.codion.dbms.sqlite", "driver: unnamed", "refused: true", "refused: true"), connect(false));
	}

	private static List<String> connect(boolean driverOnModulePath) throws IOException, InterruptedException {
		List<String> modulePath = new ArrayList<>();
		List<String> classPath = new ArrayList<>();
		String driver = null;
		for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
			String name = new File(entry).getName();
			if (name.startsWith(DRIVER)) {
				driver = entry;
			}
			else if (entry.endsWith(TEST_CLASSES)) {
				classPath.add(entry);
			}
			else if (entry.endsWith(MAIN_CLASSES) || name.startsWith("codion-") || name.startsWith("slf4j-api") || name.startsWith("jspecify")) {
				modulePath.add(entry);
			}
		}
		assumeTrue(driver != null && !classPath.isEmpty() && modulePath.stream().anyMatch(entry -> entry.endsWith(MAIN_CLASSES)),
						"Not the class path of a Gradle test run");
		(driverOnModulePath ? modulePath : classPath).add(driver);
		Process process = new ProcessBuilder(ProcessHandle.current().info().command().orElse("java"),
						"-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
						"--module-path", String.join(File.pathSeparator, modulePath),
						"--add-modules", "is.codion.dbms.sqlite",
						"-cp", String.join(File.pathSeparator, classPath),
						ModulePathConnection.class.getName())
						.redirectError(ProcessBuilder.Redirect.DISCARD)
						.start();
		List<String> output = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.add(line);
			}
		}
		assertTrue(process.waitFor(60, TimeUnit.SECONDS));

		return output;
	}
}
