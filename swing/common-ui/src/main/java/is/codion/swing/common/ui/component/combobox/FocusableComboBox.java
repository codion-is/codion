package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

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
}
