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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SizesTest {

  @Test
  void setPreferredWidth() {
    JTextField textField = new JTextField();
    Sizes.setPreferredWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getPreferredSize().height), textField.getPreferredSize());
    JComboBox<String> box = new JComboBox<>();
    box.setPreferredSize(new Dimension(10, 10));
    Sizes.setPreferredWidth(box, 42);
    assertEquals(10, box.getPreferredSize().height);
    assertEquals(42, box.getPreferredSize().width);
  }

  @Test
  void setPreferredHeight() {
    JTextField textField = new JTextField();
    Sizes.setPreferredHeight(textField, 42);
    assertEquals(new Dimension(textField.getPreferredSize().width, 42), textField.getPreferredSize());
  }

  @Test
  void setMinimumWidth() {
    JTextField textField = new JTextField();
    Sizes.setMinimumWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getMinimumSize().height), textField.getMinimumSize());
    JComboBox<String> box = new JComboBox<>();
    box.setMinimumSize(new Dimension(10, 10));
    Sizes.setMinimumWidth(box, 42);
    assertEquals(10, box.getMinimumSize().height);
    assertEquals(42, box.getMinimumSize().width);
  }

  @Test
  void setMinimumHeight() {
    JTextField textField = new JTextField();
    Sizes.setMinimumHeight(textField, 42);
    assertEquals(new Dimension(textField.getMinimumSize().width, 42), textField.getMinimumSize());
  }

  @Test
  void setMaximumWidth() {
    JTextField textField = new JTextField();
    Sizes.setMaximumWidth(textField, 42);
    assertEquals(new Dimension(42, textField.getMaximumSize().height), textField.getMaximumSize());
    JComboBox<String> box = new JComboBox<>();
    box.setMaximumSize(new Dimension(10, 10));
    Sizes.setMaximumWidth(box, 42);
    assertEquals(10, box.getMaximumSize().height);
    assertEquals(42, box.getMaximumSize().width);
  }

  @Test
  void setMaximumHeight() {
    JTextField textField = new JTextField();
    Sizes.setMaximumHeight(textField, 42);
    assertEquals(new Dimension(textField.getMaximumSize().width, 42), textField.getMaximumSize());
  }
}
