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
 * Copyright (c) 2013 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.NullableCheckBox;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;
import is.codion.swing.common.ui.key.KeyEvents;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import static is.codion.swing.common.ui.component.Components.toggleButton;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultToggleControlTest {

	@Test
	void toggleControlTest() {
		Value<Boolean> booleanValue = Value.nonNull(false);
		ToggleControl control = Control.builder().toggle(booleanValue).build();
		control.value().set(true);
		assertTrue(booleanValue.getOrThrow());
		control.value().set(false);
		assertFalse(booleanValue.getOrThrow());
		booleanValue.set(true);
		assertTrue(control.value().getOrThrow());

		Value<Boolean> nullableValue = Value.nonNull(false);
		nullableValue.set(true);
		ToggleControl nullableControl = Control.builder().toggle(nullableValue).build();
		JToggleButton.ToggleButtonModel toggleButtonModel = (JToggleButton.ToggleButtonModel) toggleButton()
						.toggle(nullableControl)
						.build()
						.getModel();
		assertTrue(toggleButtonModel.isSelected());
		assertTrue(nullableControl.value().getOrThrow());
		toggleButtonModel.setSelected(false);
		assertFalse(nullableControl.value().getOrThrow());
		toggleButtonModel.setSelected(true);
		assertTrue(nullableControl.value().getOrThrow());
		nullableValue.set(false);
		assertFalse(nullableControl.value().getOrThrow());
		nullableValue.clear();
		assertFalse(toggleButtonModel.isSelected());

		Value<Boolean> nonNullableValue = Value.builder()
						.nonNull(false)
						.value(true)
						.build();
		ToggleControl nonNullableControl = Control.builder().toggle(nonNullableValue).build();
		assertTrue(nonNullableControl.value().getOrThrow());
		nonNullableValue.set(false);
		assertFalse(nonNullableControl.value().getOrThrow());
		nonNullableValue.clear();
		assertFalse(nonNullableControl.value().getOrThrow());

		State state = State.state(true);
		ToggleControl toggleControl = Control.toggle(state);
		assertTrue(toggleControl.value().getOrThrow());
		JToggleButton toggleButton = ToggleButtonBuilder.builder()
						.toggle(toggleControl)
						.build();
		assertTrue(toggleButton.isSelected());
	}

	@Test
	void stateToggleControl() throws Exception {
		State state = State.state();
		State enabledState = State.state(false);
		ToggleControl control = Control.builder()
						.toggle(state)
						.caption("stateToggleControl")
						.enabled(enabledState)
						.build();
		ButtonModel buttonModel = toggleButton()
						.toggle(control)
						.build()
						.getModel();
		assertFalse(control.isEnabled());
		assertFalse(buttonModel.isEnabled());
		SwingUtilities.invokeAndWait(() -> {
			enabledState.set(true);
			assertTrue(control.isEnabled());
			assertTrue(buttonModel.isEnabled());
			assertEquals("stateToggleControl", control.caption().orElse(null));
			assertFalse(control.value().getOrThrow());
			state.set(true);
			assertTrue(control.value().getOrThrow());
			state.set(false);
			assertFalse(control.value().getOrThrow());
			control.value().set(true);
			assertTrue(state.is());
			control.value().set(false);
			assertFalse(state.is());

			enabledState.set(false);
			assertFalse(control.isEnabled());
			enabledState.set(true);
			assertTrue(control.isEnabled());
		});
	}

	@Test
	void nullableToggleControl() {
		Value<Boolean> value = Value.nullable();
		ToggleControl toggleControl = Control.builder().toggle(value).build();
		NullableCheckBox checkBox = Components.nullableCheckBox()
						.toggle(toggleControl)
						.build();
		checkBox.set(null);
		assertNull(value.get());
		checkBox.set(false);
		assertFalse(value.getOrThrow());
		checkBox.set(true);
		assertTrue(value.getOrThrow());
		checkBox.set(null);
		assertNull(value.get());

		value.set(false);
		assertFalse(checkBox.isSelected());
		assertFalse(checkBox.get());
		value.set(true);
		assertTrue(checkBox.isSelected());
		assertTrue(checkBox.get());
		value.clear();
		assertFalse(checkBox.isSelected());
		assertNull(checkBox.get());
	}

	@Test
	void checkBox() {
		Value<Boolean> value = Value.nonNull(false);
		JCheckBox box = CheckBoxBuilder.builder()
						.toggle(Control.builder()
										.toggle(value)
										.caption("Test"))
						.build();
		assertEquals("Test", box.getText());
	}

	@Test
	void checkBoxMenuItem() {
		Value<Boolean> value = Value.nonNull(false);
		JMenuItem item = CheckBoxMenuItemBuilder.builder()
						.toggle(Control.builder()
										.toggle(value)
										.caption("Test"))
						.build();
		assertEquals("Test", item.getText());
	}

	@Test
	void copy() throws Exception {
		State state = State.state();
		State enabled = State.state();
		ToggleControl control = Control.builder()
						.toggle(state)
						.enabled(enabled)
						.caption("name")
						.description("desc")
						.mnemonic('n')
						.value("key", "value")
						.build();
		ToggleControl copy = control.copy(state)
						.caption("new name")
						.description("new desc")
						.value("key", "newvalue")
						.build();

		assertFalse(control.isEnabled());
		assertFalse(copy.isEnabled());

		SwingUtilities.invokeAndWait(() -> {
			enabled.set(true);

			assertTrue(control.isEnabled());
			assertTrue(copy.isEnabled());

			assertNotEquals(control.caption().orElse(null), copy.caption().orElse(null));
			assertNotEquals(control.description().orElse(null), copy.description().orElse(null));
			assertEquals(control.mnemonic().orElse(0), copy.mnemonic().orElse(1));
			assertNotEquals(control.getValue("key"), copy.getValue("key"));
		});
	}

	@Test
	void actionPerformed() {
		State state = State.state();
		ToggleControl control = Control.toggle(state);
		control.actionPerformed(null);
		assertTrue(state.is());
		control.actionPerformed(null);
		assertFalse(state.is());

		// false -> null -> true, as NullableCheckBox
		Value<Boolean> nullable = Value.nullable(false);
		ToggleControl nullableControl = Control.toggle(nullable);
		nullableControl.actionPerformed(null);
		assertNull(nullable.get());
		nullableControl.actionPerformed(null);
		assertTrue(nullable.getOrThrow());
		nullableControl.actionPerformed(null);
		assertFalse(nullable.getOrThrow());
	}

	@Test
	void clickTogglesOnce() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			State state = State.state();
			ToggleControl control = Control.toggle(state);
			JToggleButton toggleButton = toggleButton()
							.toggle(control)
							.build();
			toggleButton.doClick();
			assertTrue(state.is());
			assertTrue(toggleButton.isSelected());
			JCheckBox checkBox = Components.checkBox()
							.toggle(control)
							.build();
			checkBox.doClick();
			assertFalse(state.is());
			assertFalse(checkBox.isSelected());
			JMenuItem menuItem = Components.checkBoxMenuItem()
							.toggle(control)
							.build();
			menuItem.doClick();
			assertTrue(state.is());
			assertTrue(menuItem.isSelected());
			// the toolbar and menu builders
			JToolBar toolBar = Components.toolBar()
							.control(control)
							.build();
			((AbstractButton) toolBar.getComponent(0)).doClick();
			assertFalse(state.is());
			JPopupMenu popupMenu = Components.menu()
							.control(control)
							.buildPopupMenu();
			((AbstractButton) popupMenu.getComponent(0)).doClick();
			assertTrue(state.is());

			Value<Boolean> nullable = Value.nullable(false);
			NullableCheckBox nullableCheckBox = Components.nullableCheckBox()
							.toggle(Control.toggle(nullable))
							.build();
			nullableCheckBox.doClick();
			assertNull(nullable.get());
			nullableCheckBox.doClick();
			assertTrue(nullable.getOrThrow());
		});
	}

	@Test
	void keyBinding() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			State state = State.state();
			State enabled = State.state(true);
			ToggleControl control = Control.builder()
							.toggle(state)
							.enabled(enabled)
							.build();
			JPanel panel = new JPanel();
			KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK);
			KeyEvents.builder()
							.keyStroke(keyStroke)
							.action(control)
							.enable(panel);
			pressKey(panel, keyStroke);
			assertTrue(state.is());
			pressKey(panel, keyStroke);
			assertFalse(state.is());
			enabled.set(false);
			pressKey(panel, keyStroke);
			assertFalse(state.is());
		});
	}

	@Test
	void controlAsAction() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			State state = State.state();
			ToggleControl control = Control.toggle(state);
			// a toggle control given as the control or action of a toggle button is linked, as by toggle()
			JCheckBox controlCheckBox = Components.checkBox()
							.control(control)
							.build();
			JCheckBox actionCheckBox = Components.checkBox()
							.action(control)
							.build();
			controlCheckBox.doClick();
			assertTrue(state.is());
			assertTrue(actionCheckBox.isSelected());
			actionCheckBox.doClick();
			assertFalse(state.is());
			assertFalse(controlCheckBox.isSelected());
			state.set(true);
			assertTrue(controlCheckBox.isSelected());
			assertTrue(actionCheckBox.isSelected());
			assertThrows(IllegalArgumentException.class, () -> Components.checkBox()
							.action(Control.toggle(Value.nullable(false))));
			JMenuItem checkBoxMenuItem = Components.checkBoxMenuItem()
							.action(control)
							.build();
			checkBoxMenuItem.doClick();
			assertFalse(state.is());
			assertFalse(checkBoxMenuItem.isSelected());

			// other buttons perform it
			JButton button = Components.button()
							.control(control)
							.build();
			button.doClick();
			assertTrue(state.is());
			JMenuItem menuItem = Components.menuItem()
							.control(control)
							.build();
			menuItem.doClick();
			assertFalse(state.is());
		});
	}

	@Test
	void linkedButtonFollowsControl() throws Exception {
		Value<String> caption = Value.nullable("caption");
		State enabled = State.state(true);
		ToggleControl control = Control.builder()
						.toggle(State.state())
						.caption(caption)
						.enabled(enabled)
						.build();
		JToggleButton toggleButton = toggleButton()
						.toggle(control)
						.build();
		assertEquals("caption", toggleButton.getText());
		SwingUtilities.invokeAndWait(() -> {
			caption.set("changed");
			enabled.set(false);
		});
		assertEquals("changed", toggleButton.getText());
		assertFalse(toggleButton.isEnabled());
		// the control is presented, but not performed when the button is pressed
		assertFalse(asList(toggleButton.getActionListeners()).contains(control));
	}

	private static void pressKey(JComponent component, KeyStroke keyStroke) {
		Object actionKey = component.getInputMap(JComponent.WHEN_FOCUSED).get(keyStroke);
		KeyEvent event = new KeyEvent(component, KeyEvent.KEY_PRESSED, 0, keyStroke.getModifiers(),
						keyStroke.getKeyCode(), KeyEvent.CHAR_UNDEFINED);
		SwingUtilities.notifyAction(component.getActionMap().get(actionKey), keyStroke, event, component, event.getModifiersEx());
	}
}
