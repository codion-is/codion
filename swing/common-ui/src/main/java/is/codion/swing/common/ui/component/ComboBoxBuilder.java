/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.combobox.Completion;
import is.codion.swing.common.ui.combobox.SteppedComboBox;

import javax.swing.ComboBoxEditor;
import javax.swing.ListCellRenderer;

/**
 * Builds a {@link SteppedComboBox}.
 * @param <T> the value type
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComboBoxBuilder<T, C extends SteppedComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends ComponentBuilder<T, C, B> {

  /**
   * @param popupWidth the required popup with
   * @return this builder instance
   */
  B popupWidth(int popupWidth);

  /**
   * @param editable specifies whether the combo box should be editable
   * @return this builder instance
   */
  B editable(boolean editable);

  /**
   * @param completionMode the completion mode
   * @return this builder instance
   */
  B completionMode(Completion.Mode completionMode);

  /**
   * @param renderer the renderer for the combo box
   * @return this builder instance
   */
  B renderer(ListCellRenderer<T> renderer);

  /**
   * @param editor the editor for the combo box
   * @return this builder instance
   */
  B editor(ComboBoxEditor editor);

  /**
   * Enable mouse wheel scrolling on the combo box
   * @param mouseWheelScrolling true if mouse wheel scrolling should be enabled
   * @return this builder instance
   */
  B mouseWheelScrolling(boolean mouseWheelScrolling);

  /**
   * Enable mouse wheel scrolling on the combo box, with wrap around
   * @param mouseWheelScrollingWithWrapAround true if mouse wheel scrolling with wrap around should be enabled
   * @return this builder instance
   */
  B mouseWheelScrollingWithWrapAround(boolean mouseWheelScrollingWithWrapAround);
}
