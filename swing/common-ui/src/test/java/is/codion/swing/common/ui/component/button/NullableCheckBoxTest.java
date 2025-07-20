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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.model.component.button.NullableToggleButtonModel;

import org.junit.jupiter.api.Test;

import java.awt.event.MouseListener;

import static org.junit.jupiter.api.Assertions.*;

public class NullableCheckBoxTest {

	@Test
	void test() {
		NullableCheckBox box = new NullableCheckBox(new NullableToggleButtonModel(false), "Test");
		assertFalse(box.model().state().getOrThrow());
		MouseListener mouseListener = box.getMouseListeners()[1];
		mouseListener.mouseClicked(null);
		assertTrue(box.model().state().getOrThrow());
		mouseListener.mouseClicked(null);
		assertNull(box.model().state().get());
		mouseListener.mouseClicked(null);
		assertFalse(box.model().state().get());

		box.getModel().setEnabled(false);
		mouseListener.mouseClicked(null);
		assertFalse(box.model().state().get());

		box.model().state().set(null);
		assertNull(box.model().state().get());

		assertThrows(UnsupportedOperationException.class, () -> box.setModel(new NullableToggleButtonModel()));
	}
}
