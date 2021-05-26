/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

public interface ComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>> {

  /**
   * @param editable specifies whether the combo box should be editable
   * @return this builder instance
   */
  ComboBoxBuilder<T> editable(boolean editable);

  /**
   * @param completionMode the completion mode
   * @return this builder instance
   */
  ComboBoxBuilder<T> completionMode(Completion.Mode completionMode);
}
