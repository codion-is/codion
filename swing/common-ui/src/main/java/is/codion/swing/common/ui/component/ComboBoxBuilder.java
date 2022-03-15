/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.combobox.Completion;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.ListCellRenderer;
import java.awt.event.ItemListener;

/**
 * Builds a {@link JComboBox}.
 * @param <T> the value type
 * @param <C> the component type
 * @param <B> the builder type
 */
public interface ComboBoxBuilder<T, C extends JComboBox<T>, B extends ComboBoxBuilder<T, C, B>> extends ComponentBuilder<T, C, B> {

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

  /**
   * @param maximumRowCount the maximum row count before a scroll bar is shown
   * @return this builder instance
   */
  B maximumRowCount(int maximumRowCount);

  /**
   * When a JComboBox is editable, and a long item is selected, the caret is moved to the end, hiding
   * the start of the selected item. Enabling this moves the caret to the front on selection, thereby
   * showing the start of the selected item.
   * Note that this only works for {@link is.codion.common.model.combobox.FilteredComboBoxModel}.
   * This is enabled by default.
   * @param moveCaretOnSelection if true the caret is moved to the front of the editor component on selection,
   * displaying the start of the selected item, instead of the end
   * @return this builder instance
   */
  B moveCaretOnSelection(boolean moveCaretOnSelection);

  /**
   * @param popupWidth a fixed popup width
   * @return this builder instance
   */
  B popupWidth(int popupWidth);

  /**
   * @param itemListener the item listener
   * @return this builder instance
   */
  B itemListener(ItemListener itemListener);
}
