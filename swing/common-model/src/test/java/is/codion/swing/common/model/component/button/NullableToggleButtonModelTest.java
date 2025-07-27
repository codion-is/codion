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

import static is.codion.swing.common.model.component.button.NullableToggleButtonModel.nullableToggleButtonModel;
import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

	@Test
	void iterateState() {
		NullableToggleButtonModel model = nullableToggleButtonModel();
		assertNull(model.state().get());
		assertFalse(model.isSelected());
		model.state().next();
		assertFalse(model.state().get());
		assertFalse(model.isSelected());
		model.state().next();
		assertTrue(model.state().get());
		assertTrue(model.isSelected());
		model.state().next();
		assertNull(model.state().get());
	}

	@Test
	void setNull() {
		NullableToggleButtonModel model = nullableToggleButtonModel(true);
		assertTrue(model.state().getOrThrow());
		assertTrue(model.isSelected());
		model.state().clear();
		assertNull(model.state().get());
		assertFalse(model.isSelected());
	}

	@Test
	void setSelected() {
		NullableToggleButtonModel model = nullableToggleButtonModel(false);
		assertFalse(model.state().getOrThrow());
		model.setSelected(true);
		assertTrue(model.isSelected());
		model.setSelected(false);
		assertFalse(model.isSelected());
		model.state().clear();
		assertFalse(model.isSelected());
	}
}
