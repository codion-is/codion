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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.utilities.resource.MessageBundle;

import java.util.ArrayList;
import java.util.Collection;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

abstract class AbstractSelectionDialogBuilder<T, B extends SelectionDialogBuilder<T, B>> extends AbstractDialogBuilder<B>
				implements SelectionDialogBuilder<T, B> {

	protected static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;

	protected static final MessageBundle MESSAGES =
					messageBundle(SelectionDialogBuilder.class, getBundle(SelectionDialogBuilder.class.getName()));

	protected final Collection<T> values;
	protected boolean allowEmptySelection = false;

	AbstractSelectionDialogBuilder(Collection<T> values) {
		if (requireNonNull(values).isEmpty()) {
			throw new IllegalArgumentException("One or more items to select from must be provided");
		}
		this.values = new ArrayList<>(values);
	}

	@Override
	public final B allowEmptySelection(boolean allowEmptySelection) {
		this.allowEmptySelection = allowEmptySelection;
		return (B) this;
	}
}
