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

import static org.junit.jupiter.api.Assertions.*;

public final class Log4jProxyTest {

	@Test
	void test() {
		new Log4jProxy().files();
	}

	@Test
	void logLevel() {
		Log4jProxy proxy = new Log4jProxy();
		String logger = Log4jProxyTest.class.getPackageName();
		proxy.setLogLevel(proxy.rootLogger(), Level.WARN);
		proxy.setLogLevel(logger, Level.TRACE);
		assertEquals(Level.TRACE, proxy.getLogLevel(logger));
		assertEquals(Level.WARN, proxy.getLogLevel(proxy.rootLogger()));
		assertTrue(LogManager.getLogger(Log4jProxyTest.class).isTraceEnabled());
		assertFalse(LogManager.getLogger("other").isInfoEnabled());
	}
}
