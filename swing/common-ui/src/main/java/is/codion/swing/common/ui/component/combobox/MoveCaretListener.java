/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.event.EventDataListener;

import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;
import java.awt.Component;

final class MoveCaretListener<T> implements EventDataListener<T> {

  private final JComboBox<?> comboBox;

  MoveCaretListener(JComboBox<T> comboBox) {
    this.comboBox = comboBox;
  }

  @Override
  public void onEvent(Object selectedItem) {
    Component editorComponent = comboBox.getEditor().getEditorComponent();
    if (selectedItem != null && editorComponent instanceof JTextComponent) {
      ((JTextComponent) editorComponent).setCaretPosition(0);
    }
  }
}
