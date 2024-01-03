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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.button;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NullableToggleButtonModelTest {

  @Test
  void iterateState() {
    NullableToggleButtonModel model = new NullableToggleButtonModel();
    assertNull(model.getState());
    assertFalse(model.isSelected());
    model.nextState();
    assertFalse(model.getState());
    assertFalse(model.isSelected());
    model.nextState();
    assertTrue(model.getState());
    assertTrue(model.isSelected());
    model.nextState();
    assertNull(model.getState());
  }

  @Test
  void setNull() {
    NullableToggleButtonModel model = new NullableToggleButtonModel(true);
    assertTrue(model.getState());
    assertTrue(model.isSelected());
    model.setState(null);
    assertNull(model.getState());
    assertFalse(model.isSelected());
  }

  @Test
  void setSelected() {
    NullableToggleButtonModel model = new NullableToggleButtonModel(false);
    assertFalse(model.getState());
    model.setSelected(true);
    assertTrue(model.isSelected());
    model.setSelected(false);
    assertFalse(model.isSelected());
    model.setState(null);
    assertFalse(model.isSelected());
  }
}
