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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.control;

import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.Components;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests the integration between Controls and UI components, focusing on
 * meaningful behavior rather than trivial factory methods.
 */
public final class ControlComponentIntegrationTest {

	@Test
	void controlStatePropagatesToMultipleComponents() throws Exception {
		State enabledState = State.state(false);
		AtomicInteger actionCount = new AtomicInteger();

		Control control = Control.builder()
						.command(actionCount::incrementAndGet)
						.caption("Test Action")
						.enabled(enabledState)
						.build();

		// Create multiple UI components bound to the same control
		JButton button = Components.button().control(control).build();
		JMenuItem menuItem = Components.menuItem().control(control).build();

		// Initially disabled
		assertFalse(button.isEnabled());
		assertFalse(menuItem.isEnabled());

		// Enable via state change
		SwingUtilities.invokeAndWait(() -> enabledState.set(true));

		// Both components should be enabled
		assertTrue(button.isEnabled());
		assertTrue(menuItem.isEnabled());

		// Actions should work on both
		button.doClick();
		assertEquals(1, actionCount.get());

		menuItem.doClick();
		assertEquals(2, actionCount.get());

		// Disable again
		SwingUtilities.invokeAndWait(() -> enabledState.set(false));
		assertFalse(button.isEnabled());
		assertFalse(menuItem.isEnabled());
	}

	@Test
	void toggleControlSynchronizesWithValueAndComponents() throws Exception {
		Value<Boolean> toggleValue = Value.builder()
						.nonNull(false)
						.build();
		State enabledState = State.state(true);

		ToggleControl toggleControl = Control.builder()
						.toggle(toggleValue)
						.caption("Toggle Option")
						.enabled(enabledState)
						.build();

		// Create different toggle components
		JCheckBox checkBox = Components.checkBox()
						.toggleControl(toggleControl)
						.build();
		JToggleButton toggleButton = Components.toggleButton()
						.toggleControl(toggleControl)
						.build();
		// Note: CheckBoxMenuItem doesn't support nullable values, so we use a different approach
		JRadioButton radioButton = Components.radioButton()
						.toggleControl(toggleControl)
						.build();

		// All should be unselected initially
		assertFalse(checkBox.isSelected());
		assertFalse(toggleButton.isSelected());
		assertFalse(radioButton.isSelected());
		assertFalse(toggleValue.getOrThrow());

		// Click one component
		SwingUtilities.invokeAndWait(checkBox::doClick);

		// All should be synchronized
		assertTrue(checkBox.isSelected());
		assertTrue(toggleButton.isSelected());
		assertTrue(radioButton.isSelected());
		assertTrue(toggleValue.getOrThrow());

		// Change value directly
		SwingUtilities.invokeAndWait(() -> toggleValue.set(false));

		// All components should update
		assertFalse(checkBox.isSelected());
		assertFalse(toggleButton.isSelected());
		assertFalse(radioButton.isSelected());

		// Disable the control
		SwingUtilities.invokeAndWait(() -> enabledState.set(false));
		assertFalse(checkBox.isEnabled());
		assertFalse(toggleButton.isEnabled());
		assertFalse(radioButton.isEnabled());
	}

	@Test
	void controlCopyPreservesProperties() {
		State originalEnabled = State.state(true);

		Control original = Control.builder()
						.command(() -> {})
						.caption("Original")
						.description("Test description")
						.mnemonic('O')
						.enabled(originalEnabled)
						.build();

		// Create a copy with modified caption
		Control copy = original.copy()
						.caption("Copy")
						.build();

		// Copy should preserve most properties
		assertEquals("Test description", copy.description().orElse(null));
		assertEquals('O', copy.mnemonic().orElse(0));

		// But caption should be different
		assertEquals("Original", original.caption().orElse(null));
		assertEquals("Copy", copy.caption().orElse(null));

		// Both should share the enabled state
		assertTrue(original.isEnabled());
		assertTrue(copy.isEnabled());

		// Changing the shared state affects both
		SwingUtilities.invokeLater(() -> {
			originalEnabled.set(false);// Updates the control enabled state on the EDT
			assertFalse(original.isEnabled());
			assertFalse(copy.isEnabled()); // Copy shares the enabled state
		});
	}

	@Test
	void multipleControlsShareEnabledState() throws Exception {
		State sharedEnabled = State.state(true);
		AtomicInteger action1Count = new AtomicInteger();
		AtomicInteger action2Count = new AtomicInteger();

		Control control1 = Control.builder()
						.command(action1Count::incrementAndGet)
						.caption("Action 1")
						.enabled(sharedEnabled)
						.build();

		Control control2 = Control.builder()
						.command(action2Count::incrementAndGet)
						.caption("Action 2")
						.enabled(sharedEnabled)
						.build();

		JButton button1 = Components.button().control(control1).build();
		JButton button2 = Components.button().control(control2).build();

		// Both enabled
		assertTrue(button1.isEnabled());
		assertTrue(button2.isEnabled());

		// Disable shared state
		SwingUtilities.invokeAndWait(() -> sharedEnabled.set(false));

		// Both should be disabled
		assertFalse(button1.isEnabled());
		assertFalse(button2.isEnabled());

		// Neither should execute
		button1.doClick();
		button2.doClick();
		assertEquals(0, action1Count.get());
		assertEquals(0, action2Count.get());
	}

	@Test
	void controlWithoutBuilderFactoryMethods() {
		// Test the static factory methods that don't require builder
		AtomicInteger commandCount = new AtomicInteger();
		Control commandControl = Control.command(commandCount::incrementAndGet);

		commandControl.actionPerformed(new ActionEvent(this, 0, ""));
		assertEquals(1, commandCount.get());

		// Action command with event
		Control actionControl = Control.action(event ->
						assertEquals("test command", event.getActionCommand()));
		actionControl.actionPerformed(new ActionEvent(this, 0, "test command"));

		// Toggle controls
		Value<Boolean> value = Value.nullable(false);
		ToggleControl toggleValueControl = Control.toggle(value);
		assertFalse(toggleValueControl.value().getOrThrow());

		State state = State.state(true);
		ToggleControl toggleStateControl = Control.toggle(state);
		assertTrue(toggleStateControl.value().getOrThrow());
	}
}