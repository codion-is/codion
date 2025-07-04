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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local.logger;

import is.codion.common.logging.MethodTrace;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class MethodLoggerTest {

	private static final MethodLogger.ArgumentFormatter FORMATTER = (methodName, argument) -> Objects.toString(argument);

	@Test
	void test() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		assertFalse(logger.isEnabled());
		logger.setEnabled(true);

		String methodName = "test";
		logger.enter(methodName);
		logger.exit(methodName);

		assertEquals(1, logger.entries().size());
		logger.setEnabled(false);
		assertEquals(0, logger.entries().size());
	}

	@Test
	void serialize() throws IOException, ClassNotFoundException {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method");
		logger.enter("method2");
		logger.exit("method2");
		logger.exit("method");

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		new ObjectOutputStream(byteOut).writeObject(logger.entries());
		new ObjectInputStream(new ByteArrayInputStream(byteOut.toByteArray())).readObject();
	}

	@Test
	void enableDisable() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		assertFalse(logger.isEnabled());
		logger.enter("method");

		assertEquals(0, logger.entries().size());

		logger.setEnabled(true);
		logger.enter("method2");
		logger.exit("method2");

		assertEquals(1, logger.entries().size());

		logger.setEnabled(false);
		assertEquals(0, logger.entries().size());
	}

	@Test
	void singleLevelLogging() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method");
		logger.exit("method");

		assertEquals(1, logger.entries().size());
		MethodTrace entry = logger.entries().get(0);
		assertEquals("method", entry.method());

		logger.enter("method2");
		logger.exit("method2");

		assertEquals(2, logger.entries().size());
		MethodTrace entry2 = logger.entries().get(1);
		assertEquals("method2", entry2.method());

		assertTrue(logger.entries().containsAll(asList(entry, entry2)));
	}

	@Test
	void twoLevelLogging() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method", new Object[] {"param1", "param2"});
		logger.enter("subMethod");
		logger.exit("subMethod");
		logger.enter("subMethod2");
		logger.exit("subMethod2");
		logger.exit("method");

		assertEquals(1, logger.entries().size());

		MethodTrace lastEntry = logger.entries().get(0);
		assertEquals("method", lastEntry.method());
		assertFalse(lastEntry.children().isEmpty());
		List<MethodTrace> subLog = lastEntry.children();
		assertEquals(2, subLog.size());
		MethodTrace subEntry = subLog.get(0);
		assertEquals("subMethod", subEntry.method());
		assertTrue(subEntry.children().isEmpty());
	}

	@Test
	void twoLevelLoggingSameMethodName() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method");
		logger.enter("method");
		logger.exit("method");
		logger.enter("method");
		logger.exit("method");
		logger.exit("method");

		assertEquals(1, logger.entries().size());

		MethodTrace lastEntry = logger.entries().get(0);
		assertEquals("method", lastEntry.method());
		assertFalse(lastEntry.children().isEmpty());
		List<MethodTrace> subLog = lastEntry.children();
		assertEquals(2, subLog.size());
		MethodTrace subEntry = subLog.get(0);
		assertEquals("method", subEntry.method());
		assertTrue(subEntry.children().isEmpty());
	}

	@Test
	void threeLevelLogging() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("one");
		logger.enter("two");
		logger.enter("three");
		logger.exit("three");
		logger.exit("two");
		logger.enter("two2");
		logger.enter("three2");
		logger.exit("three2");
		logger.exit("two2");
		logger.exit("one");

		assertEquals(1, logger.entries().size());

		MethodTrace entry = logger.entries().get(0);
		assertEquals("one", entry.method());
		assertFalse(entry.children().isEmpty());
		List<MethodTrace> subLog = entry.children();
		assertEquals(2, subLog.size());
		MethodTrace subEntry1 = subLog.get(0);
		assertEquals("two", subEntry1.method());
		assertFalse(entry.children().isEmpty());
		MethodTrace subEntry2 = subLog.get(1);
		assertEquals("two2", subEntry2.method());
		assertFalse(entry.children().isEmpty());

		List<MethodTrace> subSubLog = subEntry1.children();
		MethodTrace subSubEntry = subSubLog.get(0);
		assertEquals("three", subSubEntry.method());
		assertTrue(subSubEntry.children().isEmpty());
		List<MethodTrace> subSubLog2 = subEntry2.children();
		MethodTrace subSubEntry2 = subSubLog2.get(0);
		assertEquals("three2", subSubEntry2.method());
		assertTrue(subSubEntry2.children().isEmpty());
	}

	@Test
	void exitBeforeEnter() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method");
		logger.exit("method");
		assertThrows(IllegalStateException.class, () -> logger.exit("method"));
	}

	@Test
	void wrongMethodName() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("method");
		assertThrows(IllegalStateException.class, () -> logger.exit("anotherMethod"));
	}

	@Test
	void appendLogEntry() {
		MethodLogger logger = MethodLogger.methodLogger(10, FORMATTER);
		logger.setEnabled(true);
		logger.enter("one");
		logger.enter("two");
		logger.enter("three");
		logger.exit("three");
		logger.exit("two");
		logger.enter("two2");
		logger.enter("three2");
		logger.exit("three2");
		logger.exit("two2");
		logger.exit("one");
		logger.entries().get(0).appendTo(new StringBuilder());
	}
}
