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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;

import javax.swing.Action;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

abstract class ControlHandler implements Consumer<Action> {

	@Override
	public final void accept(Action action) {
		if (action == Controls.SEPARATOR) {
			onSeparator();
		}
		else if (action instanceof Controls) {
			if (((Controls) action).notEmpty()) {
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
		while (!actions.isEmpty() && actions.get(0) == Controls.SEPARATOR) {
			actions.remove(0);
		}
		while (!actions.isEmpty() && actions.get(actions.size() - 1) == Controls.SEPARATOR) {
			actions.remove(actions.size() - 1);
		}
		List<Action> cleaned = new ArrayList<>(actions.size());
		for (int i = 0; i < actions.size(); i++) {
			Action action = actions.get(i);
			// Prevent multiple separators
			if (action == Controls.SEPARATOR && i > 0 && actions.get(i - 1) == Controls.SEPARATOR) {
				continue;
			}
			else {
				cleaned.add(action);
			}
		}

		return cleaned;
	}
}
