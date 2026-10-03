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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

abstract class ControlHandler implements Consumer<Action> {

	@Override
	public final void accept(Action action) {
		if (action == Controls.SEPARATOR) {
			onSeparator();
		}
		else if (action instanceof Controls) {
			if (((Controls) action).size() > 0) {
				onControls((Controls) action);
			}
		}
		else if (action instanceof ToggleControl) {
			onToggleControl((ToggleControl) action);
		}
		else if (action instanceof Control) {
			onControl((Control) action);
		}
		else {
			onAction(action);
		}
	}

	abstract void onSeparator();

	abstract void onControl(Control control);

	abstract void onToggleControl(ToggleControl toggleControl);

	abstract void onControls(Controls controls);

	abstract void onAction(Action action);

	/**
	 * Trims separators from the ends and removes adjacent duplicate separators
	 * @param actions the actions
	 * @return the cleaned action list
	 */
	protected static List<Action> cleanupSeparators(List<Action> actions) {
		return cleanupSeparators(actions, action -> action == Controls.SEPARATOR);
	}

	/**
	 * Trims separators from the ends and removes adjacent duplicate separators
	 * @param items the items
	 * @param separator identifies the separators
	 * @param <T> the item type
	 * @return a new list containing the cleaned items
	 */
	static <T> List<T> cleanupSeparators(List<T> items, Predicate<T> separator) {
		List<T> cleaned = new ArrayList<>(items.size());
		for (T item : items) {
			// skips leading and adjacent duplicate separators
			if (!separator.test(item) || (!cleaned.isEmpty() && !separator.test(cleaned.get(cleaned.size() - 1)))) {
				cleaned.add(item);
			}
		}
		// at most one trailing separator remains
		if (!cleaned.isEmpty() && separator.test(cleaned.get(cleaned.size() - 1))) {
			cleaned.remove(cleaned.size() - 1);
		}

		return cleaned;
	}
}
