/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextInputPanelTest {

  @Test
  void test() {
    TextInputPanel panel = TextInputPanel.builder()
            .caption("caption")
            .dialogTitle("title")
            .build();
    assertNotNull(panel.getButton());
    panel.getTextField().setText("hello");
    assertEquals("hello", panel.getText());
    panel.setText("just");
    assertEquals("just", panel.getTextField().getText());
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
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(inputPanel.getTextField().isEnabled());
    assertFalse(inputPanel.getButton().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(inputPanel.getTextField().isEnabled());
    assertTrue(inputPanel.getButton().isEnabled());
  }
}
