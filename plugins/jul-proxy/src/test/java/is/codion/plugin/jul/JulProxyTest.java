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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jul;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public final class JulProxyTest {

	@Test
	void levels() {
		List<Object> levels = new JulProxy().levels();
		for (int i = 1; i < levels.size(); i++) {
			assertTrue(((Level) levels.get(i - 1)).intValue() < ((Level) levels.get(i)).intValue());
		}
		assertEquals(Level.OFF, levels.get(levels.size() - 1));
	}

	@Test
	void root() {
		JulProxy proxy = new JulProxy();
		Logger logger = Logger.getLogger(JulProxyTest.class.getName());
		proxy.level(proxy.root(), Level.FINE);
		assertEquals(Level.FINE, proxy.level(proxy.root()));
		assertTrue(logger.isLoggable(Level.FINE));
		proxy.level(proxy.root(), Level.WARNING);
		assertFalse(logger.isLoggable(Level.INFO));
	}

	@Test
	void noLogger() {
		String parent = JulProxyTest.class.getName() + ".parent";
		assertNull(new JulProxy().level(parent));
		new JulProxy().level(parent, Level.FINEST);
		System.gc();
		assertEquals(Level.FINEST, new JulProxy().level(parent));
		assertTrue(Logger.getLogger(parent + ".child").isLoggable(Level.FINEST));
	}
}
