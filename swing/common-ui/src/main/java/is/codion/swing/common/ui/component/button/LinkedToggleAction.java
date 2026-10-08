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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.control.ToggleControl;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;

/**
 * The action of a button whose model is linked to the value of a {@link ToggleControl}, presenting the control, its
 * caption, icon and enabled state, without performing it, since the button model toggles the value.
 */
final class LinkedToggleAction implements Action {

	private final ToggleControl toggleControl;

	LinkedToggleAction(ToggleControl toggleControl) {
		this.toggleControl = toggleControl;
	}

	@Override
	public void actionPerformed(ActionEvent e) {/*the button model toggles the linked value*/}

	@Override
	public @Nullable Object getValue(String key) {
		return toggleControl.getValue(key);
	}

	@Override
	public void putValue(String key, @Nullable Object value) {
		toggleControl.putValue(key, value);
	}

	@Override
	public void setEnabled(boolean enabled) {
		toggleControl.setEnabled(enabled);
	}

	@Override
	public boolean isEnabled() {
		return toggleControl.isEnabled();
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		toggleControl.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		toggleControl.removePropertyChangeListener(listener);
	}
}
