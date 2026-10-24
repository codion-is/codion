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
package is.codion.plugin.logback;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class LogbackProxyTest {

	@Test
	void levels() {
		List<Object> levels = new LogbackProxy().levels();
		for (int i = 1; i < levels.size(); i++) {
			assertTrue(((Level) levels.get(i - 1)).toInt() < ((Level) levels.get(i)).toInt());
		}
		assertEquals(Level.OFF, levels.get(levels.size() - 1));
	}

	@Test
	void test() {
		new LogbackProxy().files();
	}

	@Test
	void level() {
		LogbackProxy proxy = new LogbackProxy();
		String logger = LogbackProxyTest.class.getPackage().getName();
		proxy.level(proxy.root(), Level.WARN);
		proxy.level(logger, Level.TRACE);
		assertEquals(Level.TRACE, proxy.level(logger));
		assertEquals(Level.WARN, proxy.level(proxy.root()));
		assertNull(proxy.level(logger + ".child"));
		assertTrue(LoggerFactory.getLogger(LogbackProxyTest.class).isTraceEnabled());
		assertFalse(LoggerFactory.getLogger("other").isInfoEnabled());
	}
}
