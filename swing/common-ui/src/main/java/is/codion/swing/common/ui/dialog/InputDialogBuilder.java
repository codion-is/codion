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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.state.StateObserver;

import java.util.function.Predicate;

/**
 * Displays the component from a given component value in a dialog and returns the value if the user accepts the input.
 */
public interface InputDialogBuilder<T> extends DialogBuilder<InputDialogBuilder<T>> {

  /**
   * @param caption the label caption
   * @return this builder instance
   */
  InputDialogBuilder<T> caption(String caption);

  /**
   * A StateObserver indicating whether the input is valid, this state controls the enabled state of the OK button.
   * Overrides {@link #inputValidator(Predicate)}.
   * @param inputValid a StateObserver indicating whether the input value is valid
   * @return this builder instance
   */
  InputDialogBuilder<T> inputValid(StateObserver inputValid);

  /**
   * Sets the {@link #inputValid(StateObserver)} according to the given predicate.
   * @param validInputPredicate the valid input predicate
   * @return this builder instance
   */
  InputDialogBuilder<T> inputValidator(Predicate<T> validInputPredicate);

  /**
   * Shows the input dialog and returns the value if the user presses OK
   * @return the value from the component value if the user accepts the input
   * @throws is.codion.common.model.CancelException in case the user cancels
   */
  T show();
}
