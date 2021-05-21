/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

import is.codion.swing.common.ui.combobox.SteppedComboBox;

public interface ComboBoxBuilder<T> extends ComponentBuilder<T, SteppedComboBox<T>, ComboBoxBuilder<T>> {

  /**
   * @return this builder instance
   */
  ComboBoxBuilder<T> editable(boolean editable);
}
