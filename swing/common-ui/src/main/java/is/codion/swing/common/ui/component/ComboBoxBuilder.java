/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.ComboBoxEditor;
import javax.swing.ListCellRenderer;

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

  /**
   * @param renderer the renderer for the combo box
   * @return this builder instance
   */
  ComboBoxBuilder<T> renderer(ListCellRenderer<T> renderer);

  /**
   * @param editor the editor for the combo box
   * @return this builder instance
   */
  ComboBoxBuilder<T> editor(ComboBoxEditor editor);

  /**
   * Enable mouse wheel scrolling on the combo box
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  ComboBoxBuilder<T> mouseWheelScrolling(boolean mouseWheelScrolling);
}
