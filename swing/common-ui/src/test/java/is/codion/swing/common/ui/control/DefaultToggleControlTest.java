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
 * Copyright (c) 2013 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.model.component.button.NullableToggleButtonModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxMenuItemBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonBuilder;

import org.junit.jupiter.api.Test;

import javax.swing.ButtonModel;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import static is.codion.swing.common.ui.component.Components.toggleButton;
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
		ButtonModel buttonModel = toggleButton()
						.toggle(nonNullableControl)
						.build()
						.getModel();
		assertFalse(buttonModel instanceof NullableToggleButtonModel);
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
	void stateToggleControl() {
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
		SwingUtilities.invokeLater(() -> {
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
		NullableToggleButtonModel buttonModel = Components.nullableCheckBox()
						.toggle(toggleControl)
						.build()
						.model();
		buttonModel.set(null);
		assertNull(value.get());
		buttonModel.setSelected(false);
		assertFalse(value.getOrThrow());
		buttonModel.setSelected(true);
		assertTrue(value.getOrThrow());
		buttonModel.set(null);
		assertNull(value.get());

		value.set(false);
		assertFalse(buttonModel.isSelected());
		assertFalse(buttonModel.get());
		value.set(true);
		assertTrue(buttonModel.isSelected());
		assertTrue(buttonModel.get());
		value.clear();
		assertFalse(buttonModel.isSelected());
		assertNull(buttonModel.get());
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
	void copy() {
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

		SwingUtilities.invokeLater(() -> {
			enabled.set(true);

			assertTrue(control.isEnabled());
			assertTrue(copy.isEnabled());

			assertNotEquals(control.caption().orElse(null), copy.caption().orElse(null));
			assertNotEquals(control.description().orElse(null), copy.description().orElse(null));
			assertEquals(control.mnemonic().orElse(0), copy.mnemonic().orElse(1));
			assertNotEquals(control.getValue("key"), copy.getValue("key"));
		});
	}
}
