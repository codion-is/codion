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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.JComponent;
import java.util.Collection;
import java.util.Optional;

/**
 * A builder for a selection dialog.
 * @param <T> the value type
 */
public interface SelectionDialogBuilder<T> extends DialogBuilder<SelectionDialogBuilder<T>> {

  /**
   * @param defaultSelection the item selected by default
   * @return this SelectionDialogBuilder instance
   * @throws IllegalArgumentException in case the selection values do not contain the default selection item
   */
  SelectionDialogBuilder<T> defaultSelection(T defaultSelection);

  /**
   * @param defaultSelection the items selected by default
   * @return this SelectionDialogBuilder instance
   * @throws IllegalArgumentException in case the selection values do not contain the default selection items
   */
  SelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection);

  /**
   * @param allowEmptySelection if true then the dialog accepts an empty selection, default false
   * @return this SelectionDialogBuilder instance
   */
  SelectionDialogBuilder<T> allowEmptySelection(boolean allowEmptySelection);

  /**
   * @return the selected value, {@link Optional#empty()} if none was selected
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  Optional<T> selectSingle();

  /**
   * @return the selected values, an empty Collection if none was selected
   * @throws is.codion.common.model.CancelException in case the user cancelled
   */
  Collection<T> select();

  /**
   * Provides the user with the ability to select a value.
   * @param <T> the value type
   */
  interface Selector<T> {

    /**
     * @param dialogOwner the dialog owner
     * @return the selected value, an empty Optional if nothing was selected
     * @throws is.codion.common.model.CancelException in case the user cancelled
     */
    Optional<T> select(JComponent dialogOwner);
  }
}
