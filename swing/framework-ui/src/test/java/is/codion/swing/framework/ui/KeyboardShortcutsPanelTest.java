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

import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.component.text.TextInput;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.ControlKey;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.ResourceBundle.getBundle;
import static org.junit.jupiter.api.Assertions.*;

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
	void keyStrokeText() {
		ControlKey<CommandControl> up = CommandControl.key("up", keyStroke(VK_UP, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		ControlKey<CommandControl> down = CommandControl.key("down", keyStroke(VK_DOWN, CTRL_DOWN_MASK | ALT_DOWN_MASK));
		ControlKey<CommandControl> left = CommandControl.key("left", keyStroke(VK_LEFT, SHIFT_DOWN_MASK));
		ControlKey<CommandControl> none = CommandControl.key("none");
		// the modifiers shared shown once
		assertEquals("Ctrl + Alt + ↑/↓", KeyboardShortcutsPanel.keys(up, down));
		assertEquals("Ctrl + Alt + ↑ / Shift + ←", KeyboardShortcutsPanel.keys(up, left));
		// keys without a key stroke left out
		assertEquals("Ctrl + Alt + ↑", KeyboardShortcutsPanel.keys(up, none));
		assertEquals("", KeyboardShortcutsPanel.keys(none));
	}

	@Test
	void defaultKeyStrokes() {
		// the key strokes shown are the default ones, a section without any left out
		Value<KeyStroke> delete = EntityTablePanel.ControlKeys.DELETE.defaultKeystroke();
		Value<KeyStroke> displayTextArea = TextInput.ControlKeys.DISPLAY_TEXT_AREA.defaultKeystroke();
		KeyStroke deleteKeyStroke = delete.get();
		KeyStroke displayTextAreaKeyStroke = displayTextArea.get();
		try {
			delete.set(keyStroke(VK_DELETE, CTRL_DOWN_MASK));
			displayTextArea.clear();
			List<String> texts = texts(new KeyboardShortcutsPanel(), new ArrayList<>());
			assertTrue(texts.contains("Ctrl + " + KeyEvent.getKeyText(VK_DELETE)));
			assertFalse(texts.contains(getBundle(KeyboardShortcutsPanel.class.getName()).getString("display_input_dialog")));
		}
		finally {
			delete.set(deleteKeyStroke);
			displayTextArea.set(displayTextAreaKeyStroke);
		}
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

	private static List<String> texts(Container container, List<String> texts) {
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel) {
				texts.add(((JLabel) component).getText());
			}
			if (component instanceof Container) {
				texts(((Container) component), texts);
			}
		}

		return texts;
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
