/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

final class MoveCaretToStartListener extends FocusAdapter {

  private final JTextComponent textComponent;

  MoveCaretToStartListener(JTextComponent textComponent) {
    this.textComponent = textComponent;
  }

  @Override
  public void focusGained(FocusEvent e) {
    textComponent.setCaretPosition(0);
  }
}
