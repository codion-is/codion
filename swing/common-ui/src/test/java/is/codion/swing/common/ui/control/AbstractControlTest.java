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

import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.icon.SVGIconsTest;

import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static javax.swing.Action.*;
import static org.junit.jupiter.api.Assertions.*;

public final class AbstractControlTest {

	@Test
	void immutableAfterConstruction() {
		Control control = Control.builder()
						.command(() -> {})
						.caption("Test")
						.build();

		// These should throw UnsupportedOperationException
		assertThrows(UnsupportedOperationException.class, () -> control.setEnabled(false));
		assertThrows(UnsupportedOperationException.class, () -> control.putValue(NAME, "New Name"));
		assertThrows(UnsupportedOperationException.class, () -> control.putValue(Action.SMALL_ICON, null));
	}

	@Test
	void enabledStatePropagation() throws Exception {
		State enabledState = State.state(true);
		AtomicInteger actionCount = new AtomicInteger();

		Control control = Control.builder()
						.command(actionCount::incrementAndGet)
						.enabled(enabledState)
						.build();

		// Control should be initially enabled
		assertTrue(control.isEnabled());

		// Action should execute when enabled
		control.actionPerformed(new ActionEvent(this, 0, "test"));
		assertEquals(1, actionCount.get());

		// Disable via state
		SwingUtilities.invokeAndWait(() -> enabledState.set(false));
		assertFalse(control.isEnabled());

		// Re-enable
		SwingUtilities.invokeAndWait(() -> enabledState.set(true));
		assertTrue(control.isEnabled());
		control.actionPerformed(new ActionEvent(this, 0, "test"));
		assertEquals(2, actionCount.get());
	}

	@Test
	void getValue() {
		Control control = Control.builder()
						.command(() -> {})
						.caption("Caption")
						.description("Description")
						.smallIcon(SVGIconsTest.SMALL_ICON)
						.largeIcon(SVGIconsTest.LARGE_ICON)
						.mnemonic('C')
						.keyStroke(KeyStroke.getKeyStroke("ctrl C"))
						.build();

		assertEquals("Caption", control.getValue(NAME));
		assertEquals("Description", control.getValue(SHORT_DESCRIPTION));
		assertNotNull(control.getValue(Action.SMALL_ICON));
		assertNotNull(control.getValue(LARGE_ICON_KEY));
		assertTrue(control.largeIcon().isPresent());
		assertEquals(67, control.getValue(MNEMONIC_KEY)); // 'C' as int
		assertEquals(KeyStroke.getKeyStroke("ctrl C"), control.getValue(ACCELERATOR_KEY));

		// Test enabled via getValue
		assertTrue((Boolean) control.getValue("enabled"));
	}

	@Test
	void customProperties() {
		Font font = new Font("Arial", Font.BOLD, 12);
		Color background = Color.RED;
		Color foreground = Color.BLUE;

		Control control = Control.builder()
						.command(() -> {})
						.font(font)
						.background(background)
						.foreground(foreground)
						.build();

		assertEquals(font, control.getValue("Font"));
		assertEquals(background, control.getValue("Background"));
		assertEquals(foreground, control.getValue("Foreground"));

		assertTrue(control.font().isPresent());
		assertEquals(font, control.font().get());
		assertTrue(control.background().isPresent());
		assertEquals(background, control.background().get());
		assertTrue(control.foreground().isPresent());
		assertEquals(foreground, control.foreground().get());
	}

	@Test
	void keys() {
		Control control = Control.builder()
						.command(() -> {})
						.caption("Caption")
						.description("Description")
						.smallIcon(SVGIconsTest.SMALL_ICON)
						.build();

		assertTrue(control.keys().contains(NAME));
		assertTrue(control.keys().contains(SHORT_DESCRIPTION));
		assertTrue(control.keys().contains(Action.SMALL_ICON));

		// Should only contain String keys
		control.keys().forEach(key -> assertInstanceOf(String.class, key));
	}

	@Test
	void toStringReturnsCaption() {
		Control withCaption = Control.builder()
						.command(() -> {})
						.caption("My Caption")
						.build();
		assertEquals("My Caption", withCaption.toString());

		Control withoutCaption = Control.builder()
						.command(() -> {})
						.build();
		assertNotEquals("", withoutCaption.toString()); // Should return super.toString()
	}

	@Test
	void builderWithNullEnabledState() {
		// Should create its own enabled state
		Control control = Control.builder()
						.command(() -> {})
						.build();
		assertTrue(control.isEnabled());
		assertTrue(control.enabled().is());
	}
}