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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.indicator;

import is.codion.common.reactive.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.darker;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class BackgroundColorValidationIndicatorTest {

	@Test
	void colors() throws Exception {
		Color background = UIManager.getColor("TextField.background");
		State invalid = State.state();
		State warned = State.state();
		JTextField field = new JTextField();
		new BackgroundColorValidationIndicator().enable(field, invalid, warned);
		// colored before any look and feel change
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(background, field.getBackground());
		invalid.set(true);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(darker(background), field.getBackground());
		invalid.set(false);
		warned.set(true);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(darker(background, 0.95), field.getBackground());
		warned.set(false);
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(background, field.getBackground());
	}
}
