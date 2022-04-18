/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

final class SelectAllFocusListener extends FocusAdapter {

  private final JTextComponent textComponent;

  SelectAllFocusListener(JTextComponent textComponent) {
    this.textComponent = textComponent;
  }

  @Override
  public void focusGained(FocusEvent e) {
    textComponent.selectAll();
  }

  @Override
  public void focusLost(FocusEvent e) {
    textComponent.select(0, 0);
  }
}
