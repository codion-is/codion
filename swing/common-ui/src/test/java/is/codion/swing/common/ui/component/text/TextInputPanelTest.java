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
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  void test() {
    TextInputPanel panel = TextInputPanel.builder()
            .caption("caption")
            .dialogTitle("title")
            .build();
    assertNotNull(panel.button());
    panel.textField().setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", panel.textField().getText());
  }

  @Test
  void constructorNullTextComponent() {
    assertThrows(NullPointerException.class, () -> TextInputPanel.builder(null));
  }

  @Test
  void setTextExceedMaxLength() {
    TextInputPanel panel = TextInputPanel.builder()
            .maximumLength(5)
            .dialogTitle("title")
            .build();
    panel.setText("12345");
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }

  @Test
  void enabledState() throws InterruptedException {
    State enabledState = State.state();
    TextInputPanel inputPanel = TextInputPanel.builder()
            .build();
    linkToEnabledState(enabledState, inputPanel);
    assertFalse(inputPanel.textField().isEnabled());
    assertFalse(inputPanel.button().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(inputPanel.textField().isEnabled());
    assertTrue(inputPanel.button().isEnabled());
  }
}
