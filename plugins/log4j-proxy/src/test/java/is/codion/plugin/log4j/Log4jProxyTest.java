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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class Log4jProxyTest {

	@Test
	void files() {
		assertEquals(new HashSet<>(asList("build/log4j/file.log", "build/log4j/rolling.log", "build/log4j/random-access.log")),
						new HashSet<>(new Log4jProxy().files()));
	}

	@Test
	void level() {
		Log4jProxy proxy = new Log4jProxy();
		String logger = Log4jProxyTest.class.getPackage().getName();
		proxy.level(proxy.root(), Level.WARN);
		proxy.level(logger, Level.TRACE);
		assertEquals(Level.TRACE, proxy.level(logger));
		assertEquals(Level.WARN, proxy.level(proxy.root()));
		assertNull(proxy.level(logger + ".child"));
		assertNull(proxy.level("file"));
		assertTrue(LogManager.getLogger(Log4jProxyTest.class).isTraceEnabled());
		assertFalse(LogManager.getLogger("other").isInfoEnabled());
	}
}
