/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.util.function.Consumer;

final class MoveCaretListener<T> implements Consumer<T> {

  private final JComboBox<?> comboBox;

  MoveCaretListener(JComboBox<T> comboBox) {
    this.comboBox = comboBox;
  }

  @Override
  public void accept(Object selectedItem) {
    Component editorComponent = comboBox.getEditor().getEditorComponent();
    if (selectedItem != null && editorComponent instanceof JTextComponent) {
      ((JTextComponent) editorComponent).setCaretPosition(0);
    }
  }
}
