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
package is.codion.swing.common.model.component.button;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

	@Test
	void iterateState() {
		NullableToggleButtonModel model = new NullableToggleButtonModel();
		assertNull(model.toggleState().get());
		assertFalse(model.isSelected());
		model.toggleState().next();
		assertFalse(model.toggleState().get());
		assertFalse(model.isSelected());
		model.toggleState().next();
		assertTrue(model.toggleState().get());
		assertTrue(model.isSelected());
		model.toggleState().next();
		assertNull(model.toggleState().get());
	}

	@Test
	void setNull() {
		NullableToggleButtonModel model = new NullableToggleButtonModel(true);
		assertTrue(model.toggleState().getOrThrow());
		assertTrue(model.isSelected());
		model.toggleState().clear();
		assertNull(model.toggleState().get());
		assertFalse(model.isSelected());
	}

	@Test
	void setSelected() {
		NullableToggleButtonModel model = new NullableToggleButtonModel(false);
		assertFalse(model.toggleState().getOrThrow());
		model.setSelected(true);
		assertTrue(model.isSelected());
		model.setSelected(false);
		assertFalse(model.isSelected());
		model.toggleState().clear();
		assertFalse(model.isSelected());
	}
}
