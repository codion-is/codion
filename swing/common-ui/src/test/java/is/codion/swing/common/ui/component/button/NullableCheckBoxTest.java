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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import org.junit.jupiter.api.Test;

import javax.swing.JToggleButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import static is.codion.swing.common.ui.component.button.NullableCheckBox.nullableCheckBox;
import static org.junit.jupiter.api.Assertions.*;

public class NullableCheckBoxTest {

	@Test
	void test() {
		NullableCheckBox box = nullableCheckBox("Test");
		box.set(false);
		box.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				box.toggle();
			}
		});
		assertFalse(box.get());
		MouseListener mouseListener = box.getMouseListeners()[1];
		mouseListener.mouseClicked(null);
		assertNull(box.get());
		mouseListener.mouseClicked(null);
		assertTrue(box.get());
		mouseListener.mouseClicked(null);
		assertFalse(box.get());

		box.set(null);
		assertNull(box.get());

		assertThrows(UnsupportedOperationException.class, () -> box.setModel(new JToggleButton.ToggleButtonModel()));
	}
}
