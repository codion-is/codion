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
package is.codion.framework.db.local.tracer;

import is.codion.common.logging.MethodTrace;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import static is.codion.framework.db.local.tracer.MethodTracer.methodTracer;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class MethodTracerTest {

	@Test
	void test() {
		MethodTracer tracer = methodTracer(10);
		assertFalse(tracer.isEnabled());
		tracer.setEnabled(true);

		String methodName = "test";
		tracer.enter(methodName);
		tracer.exit(methodName);

		assertEquals(1, tracer.entries().size());
		tracer.setEnabled(false);
		assertEquals(0, tracer.entries().size());
	}

	@Test
	void serialize() throws IOException, ClassNotFoundException {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method");
		tracer.enter("method2");
		tracer.exit("method2");
		tracer.exit("method");

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		new ObjectOutputStream(byteOut).writeObject(tracer.entries());
		new ObjectInputStream(new ByteArrayInputStream(byteOut.toByteArray())).readObject();
	}

	@Test
	void enableDisable() {
		MethodTracer tracer = methodTracer(10);
		assertFalse(tracer.isEnabled());
		tracer.enter("method");

		assertEquals(0, tracer.entries().size());

		tracer.setEnabled(true);
		tracer.enter("method2");
		tracer.exit("method2");

		assertEquals(1, tracer.entries().size());

		tracer.setEnabled(false);
		assertEquals(0, tracer.entries().size());
	}

	@Test
	void singleLevelTracing() {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method");
		tracer.exit("method");

		assertEquals(1, tracer.entries().size());
		MethodTrace entry = tracer.entries().get(0);
		assertEquals("method", entry.method());

		tracer.enter("method2");
		tracer.exit("method2");

		assertEquals(2, tracer.entries().size());
		MethodTrace entry2 = tracer.entries().get(1);
		assertEquals("method2", entry2.method());

		assertTrue(tracer.entries().containsAll(asList(entry, entry2)));
	}

	@Test
	void twoLevelTracing() {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method", new Object[] {"param1", "param2"});
		tracer.enter("subMethod");
		tracer.exit("subMethod");
		tracer.enter("subMethod2");
		tracer.exit("subMethod2");
		tracer.exit("method");

		assertEquals(1, tracer.entries().size());

		MethodTrace lastEntry = tracer.entries().get(0);
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
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method");
		tracer.enter("method");
		tracer.exit("method");
		tracer.enter("method");
		tracer.exit("method");
		tracer.exit("method");

		assertEquals(1, tracer.entries().size());

		MethodTrace lastEntry = tracer.entries().get(0);
		assertEquals("method", lastEntry.method());
		assertFalse(lastEntry.children().isEmpty());
		List<MethodTrace> subLog = lastEntry.children();
		assertEquals(2, subLog.size());
		MethodTrace subEntry = subLog.get(0);
		assertEquals("method", subEntry.method());
		assertTrue(subEntry.children().isEmpty());
	}

	@Test
	void threeLevelTracing() {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("one");
		tracer.enter("two");
		tracer.enter("three");
		tracer.exit("three");
		tracer.exit("two");
		tracer.enter("two2");
		tracer.enter("three2");
		tracer.exit("three2");
		tracer.exit("two2");
		tracer.exit("one");

		assertEquals(1, tracer.entries().size());

		MethodTrace entry = tracer.entries().get(0);
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
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method");
		tracer.exit("method");
		assertThrows(IllegalStateException.class, () -> tracer.exit("method"));
	}

	@Test
	void wrongMethodName() {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("method");
		assertThrows(IllegalStateException.class, () -> tracer.exit("anotherMethod"));
	}

	@Test
	void appendLogEntry() {
		MethodTracer tracer = methodTracer(10);
		tracer.setEnabled(true);
		tracer.enter("one");
		tracer.enter("two");
		tracer.enter("three");
		tracer.exit("three");
		tracer.exit("two");
		tracer.enter("two2");
		tracer.enter("three2");
		tracer.exit("three2");
		tracer.exit("two2");
		tracer.exit("one");
		tracer.entries().get(0).appendTo(new StringBuilder());
	}
}
