/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.model.component.text.DocumentAdapter;

import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;
import java.util.function.Consumer;

final class OnTextChangedListener implements DocumentAdapter {

  private final Consumer<String> onTextChanged;
  private final JTextComponent textComponent;

  OnTextChangedListener(Consumer<String> onTextChanged, JTextComponent textComponent) {
    this.onTextChanged = onTextChanged;
    this.textComponent = textComponent;
  }

  @Override
  public void contentsChanged(DocumentEvent e) {
    onTextChanged.accept(textComponent.getText());
  }
}
