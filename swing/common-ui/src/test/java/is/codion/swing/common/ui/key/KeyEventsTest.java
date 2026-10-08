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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.key;

import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.nio.file.Paths;

import static is.codion.swing.common.ui.control.Control.command;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_A;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static org.junit.jupiter.api.Assertions.*;

public class KeyEventsTest {

	@Test
	void addRemoveKeyEvent() {
		JTextField textField = new JTextField();
		Control control = Control.builder().command(() -> {}).build();
		KeyEvents.Builder builder = KeyEvents.builder()
						.keyCode(VK_ENTER)
						.action(control);
		builder.enable(textField);
		String actionMapKey = (String) textField.getInputMap().get(KeyStroke.getKeyStroke(VK_ENTER, 0));
		assertSame(control, textField.getActionMap().get(actionMapKey));
		builder.disable(textField);
		assertNull(textField.getActionMap().get(actionMapKey));
	}

	@Test
	void addKeyEventWithoutName() {
		JComboBox<String> comboBox = new JComboBox<>();
		KeyEvents.Builder builder = KeyEvents.builder()
						.keyCode(VK_ENTER)
						.action(command(() -> {}))
						.onKeyRelease(true);
		builder.enable(comboBox);
		builder.disable(comboBox);
	}

	@Test
	void alreadyBound() {
		JTextField textField = new JTextField();
		KeyStroke keyStroke = KeyStroke.getKeyStroke(VK_ENTER, CTRL_DOWN_MASK);
		Control control = command(() -> {});
		KeyEvents.Builder builder = KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(control);
		builder.enable(textField);
		// the same action again
		builder.enable(textField);
		// a different one, unnamed as well
		KeyEvents.Builder other = KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(command(() -> {}));
		assertThrows(IllegalStateException.class, () -> other.enable(textField));
		// once disabled
		builder.disable(textField);
		other.enable(textField);
		assertNotSame(control, textField.getActionMap().get(textField.getInputMap().get(keyStroke)));
		// under another condition
		KeyEvents.builder()
						.keyStroke(keyStroke)
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(control)
						.enable(textField);
		// a look and feel binding
		KeyStroke selectAll = KeyStroke.getKeyStroke(VK_A, CTRL_DOWN_MASK);
		assertNotNull(textField.getInputMap().get(selectAll));
		KeyEvents.builder()
						.keyStroke(selectAll)
						.action(control)
						.enable(textField);
	}

	@Test
	void replace() {
		KeyStroke keyStroke = KeyStroke.getKeyStroke(VK_ENTER, CTRL_DOWN_MASK);
		Control control = command(() -> {});
		Control replacement = command(() -> {});
		// nothing bound
		JTextField textField = new JTextField();
		KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(control)
						.replace(textField);
		assertSame(control, textField.getActionMap().get(textField.getInputMap().get(keyStroke)));
		KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(replacement)
						.replace(textField);
		assertSame(replacement, textField.getActionMap().get(textField.getInputMap().get(keyStroke)));
		// the editor of an editable combo box as well
		JComboBox<String> comboBox = new JComboBox<>();
		comboBox.setEditable(true);
		JComponent editor = (JComponent) comboBox.getEditor().getEditorComponent();
		KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(control)
						.enable(comboBox);
		assertThrows(IllegalStateException.class, () -> KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(replacement)
						.enable(comboBox));
		KeyEvents.builder()
						.keyStroke(keyStroke)
						.action(replacement)
						.replace(comboBox);
		assertSame(replacement, comboBox.getActionMap().get(comboBox.getInputMap().get(keyStroke)));
		assertSame(replacement, editor.getActionMap().get(editor.getInputMap().get(keyStroke)));
	}

	@Test
	void actionMissing() {
		assertThrows(IllegalStateException.class, () -> KeyEvents.builder()
						.keyCode(VK_ENTER)
						.enable(new JTextField()));
	}

	@Test
	void headless() throws Exception {
		// in a JVM of its own, headlessness being decided at startup, and the tests running with a display
		Process process = new ProcessBuilder(Paths.get(System.getProperty("java.home"), "bin", "java").toString(),
						"-Djava.awt.headless=true", "-cp", System.getProperty("java.class.path"), Headless.class.getName())
						.inheritIO()
						.start();
		assertTrue(process.waitFor(30, SECONDS));
		assertEquals(0, process.exitValue());
	}

	public static final class Headless {

		public static void main(String[] args) {
			// the toolkit throws HeadlessException for the menu shortcut mask when headless
			if (KeyEvents.MENU_SHORTCUT_MASK != CTRL_DOWN_MASK) {
				System.exit(1);
			}
			KeyEvents.builder()
							.keyCode(VK_ENTER)
							.modifiers(KeyEvents.MENU_SHORTCUT_MASK)
							.action(command(() -> {}))
							.enable(new JTextField());
		}
	}
}
