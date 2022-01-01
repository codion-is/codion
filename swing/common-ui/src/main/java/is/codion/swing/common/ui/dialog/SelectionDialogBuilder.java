/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import java.util.Collection;
import java.util.Optional;

/**
 * A builder for a selection dialog.
 * @param <T> the value type
 */
public interface SelectionDialogBuilder<T> extends DialogBuilder<SelectionDialogBuilder<T>> {

  /**
   * @param singleSelection if true then the selection is restricted to a single value
   * @return this SelectionDialogBuilder instance
   */
  SelectionDialogBuilder<T> singleSelection(boolean singleSelection);

  /**
   * @param defaultSelection the item selected by default
   * @return this SelectionDialogBuilder instance
   */
  SelectionDialogBuilder<T> defaultSelection(T defaultSelection);

  /**
   * @param defaultSelection the items selected by default
   * @return this SelectionDialogBuilder instance
   */
  SelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection);

  /**
   * @return the selected value, {@link Optional#empty()} if none was selected
   */
  Optional<T> selectSingle();

  /**
   * @return the selected values, an empty Collection if none was selected
   */
  Collection<T> select();
}
