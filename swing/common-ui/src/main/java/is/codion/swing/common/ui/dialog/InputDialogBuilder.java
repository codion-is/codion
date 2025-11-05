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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.reactive.state.ObservableState;
import is.codion.swing.common.ui.component.builder.ComponentValueBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Displays the component from a given component value in a dialog and returns the value if the user accepts the input.
 */
public interface InputDialogBuilder<T> extends DialogBuilder<InputDialogBuilder<T>> {

	/**
	 * Provides an {@link InputDialogBuilder} instance
	 */
	interface ComponentStep {

		/**
		 * @param componentBuilder the builder which component to display
		 * @param <T> the value type
		 * @return a builder for an input dialog
		 */
		<T> InputDialogBuilder<T> component(ComponentValueBuilder<?, T, ?> componentBuilder);

		/**
		 * @param componentValue the value which component to display
		 * @param <T> the value type
		 * @return a builder for an input dialog
		 */
		<T> InputDialogBuilder<T> component(ComponentValue<?, T> componentValue);
	}

	/**
	 * @param caption the label caption
	 * @return this builder instance
	 */
	InputDialogBuilder<T> caption(@Nullable String caption);

	/**
	 * A {@link ObservableState} indicating whether the input is valid, this state controls the enabled state of the OK button.
	 * Overrides any previously set {@link #validator(Predicate)}.
	 * @param valid an {@link ObservableState} indicating whether the input value is valid
	 * @return this builder instance
	 */
	InputDialogBuilder<T> valid(ObservableState valid);

	/**
	 * Sets the {@link #valid(ObservableState)} according to the given predicate.
	 * Overrides any previously set {@link #valid(ObservableState)}.
	 * @param validator the valididator predicate
	 * @return this builder instance
	 */
	InputDialogBuilder<T> validator(Predicate<@Nullable T> validator);

	/**
	 * Shows the input dialog and calls {@code closeDialog} with the current
	 * input when OK is pressed, closing the dialog if the predicate returns true.
	 * @param closeDialog called with the current input to determine if the dialog should be closed
	 */
	void show(Predicate<@Nullable T> closeDialog);

	/**
	 * Shows the input dialog and returns the value if the user presses OK
	 * @return the value from the component value if the user accepts the input
	 * @throws is.codion.common.model.CancelException in case the user cancels
	 */
	T show();
}
