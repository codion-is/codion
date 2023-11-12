/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;

import org.junit.jupiter.api.Test;

import static is.codion.swing.common.ui.Utilities.linkToEnabledState;
import static org.junit.jupiter.api.Assertions.*;

public class TextFieldPanelTest {

  @Test
  void test() {
    TextFieldPanel panel = TextFieldPanel.builder()
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
    assertThrows(NullPointerException.class, () -> TextFieldPanel.builder(null));
  }

  @Test
  void setTextExceedMaxLength() {
    TextFieldPanel panel = TextFieldPanel.builder()
            .maximumLength(5)
            .dialogTitle("title")
            .build();
    panel.setText("12345");
    assertThrows(IllegalArgumentException.class, () -> panel.setText("123456"));
  }

  @Test
  void enabledState() throws InterruptedException {
    State enabledState = State.state();
    TextFieldPanel inputPanel = TextFieldPanel.builder()
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
