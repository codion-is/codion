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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.swing.common.ui.control.Control;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import static java.awt.event.KeyEvent.VK_ENTER;
import static org.junit.jupiter.api.Assertions.*;

public class KeyEventsTest {

  @Test
  void addRemoveKeyEvent() {
    JTextField textField = new JTextField();
    final String actionName = "testing";
    Control control = Control.builder(() -> {}).name(actionName).build();
    assertNull(textField.getActionMap().get(actionName));
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(control);
    builder.enable(textField);
    assertNotNull(textField.getActionMap().get(actionName));
    builder.disable(textField);
    assertNull(textField.getActionMap().get(actionName));
  }

  @Test
  void addKeyEventWithoutName() {
    JComboBox<String> comboBox = new JComboBox<>();
    KeyEvents.Builder builder = KeyEvents.builder(VK_ENTER).action(Control.control(() -> {})).onKeyRelease(true);
    builder.enable(comboBox);
    builder.disable(comboBox);
  }

  @Test
  void actionMissing() {
    assertThrows(IllegalStateException.class, () -> KeyEvents.builder(VK_ENTER).enable(new JTextField()));
  }
}
