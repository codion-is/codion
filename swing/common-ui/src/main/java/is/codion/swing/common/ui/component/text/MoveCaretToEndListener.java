/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

final class MoveCaretToEndListener extends FocusAdapter {

  private final JTextComponent textComponent;

  MoveCaretToEndListener(JTextComponent textComponent) {
    this.textComponent = textComponent;
  }

  @Override
  public void focusGained(FocusEvent e) {
    textComponent.setCaretPosition(textComponent.getText().length());
  }
}
