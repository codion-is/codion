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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public final class KeyboardShortcutsPanelTest {

	@Test
	void constructor() {
		new KeyboardShortcutsPanel();
	}

	@Test
	void messagesResolve() {
		//a missing message bundle key silently resolves to "!key!"
		assertNoUnresolvedMessages(new KeyboardShortcutsPanel());
	}

	@Test
	void localesDefineTheSameKeys() {
		//the panel resolves its bundle once, at class initialization, so no other locale is reachable
		//from here; the bundles themselves are compared instead, a locale missing a key falling back
		//to the english message rather than resolving to "!key!"
		assertEquals(keys(""), keys("_is_IS"));
	}

	private static Set<String> keys(String locale) {
		Properties properties = new Properties();
		try (InputStream inputStream = KeyboardShortcutsPanel.class
						.getResourceAsStream("KeyboardShortcutsPanel" + locale + ".properties")) {
			properties.load(inputStream);
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return properties.stringPropertyNames();
	}

	private static void assertNoUnresolvedMessages(Container container) {
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel) {
				String text = ((JLabel) component).getText();
				assertFalse(text != null && text.startsWith("!"), text);
			}
			if (component instanceof Container) {
				assertNoUnresolvedMessages((Container) component);
			}
		}
	}
}
