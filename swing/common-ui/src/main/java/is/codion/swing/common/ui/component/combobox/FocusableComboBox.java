/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
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
