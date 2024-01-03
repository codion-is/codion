/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.awt.event.FocusListener;

final class FocusableComboBox<T> extends JComboBox<T> {

  FocusableComboBox(ComboBoxModel<T> model) {
    super(model);
  }

  /**
   * Overridden as a workaround for editable combo boxes as initial focus components on
   * detail panels stealing the focus from the parent panel on initialization
   */
  @Override
  public void requestFocus() {
    if (isEditable()) {
      getEditor().getEditorComponent().requestFocus();
    }
    else {
      super.requestFocus();
    }
  }

  @Override
  public synchronized void addFocusListener(FocusListener listener) {
    super.addFocusListener(listener);
    if (isEditable()) {
      getEditor().getEditorComponent().addFocusListener(listener);
    }
  }

  @Override
  public synchronized void removeFocusListener(FocusListener listener) {
    super.removeFocusListener(listener);
    if (isEditable()) {
      getEditor().getEditorComponent().removeFocusListener(listener);
    }
  }
}
