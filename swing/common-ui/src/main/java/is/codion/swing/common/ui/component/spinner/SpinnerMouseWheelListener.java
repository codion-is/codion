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
package is.codion.swing.common.ui.component.spinner;

import javax.swing.JSpinner;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import static java.util.Objects.requireNonNull;

/**
 * A mouse wheel listener for JSpinner, moving to the next or previous value on wheel spin.
 * Up/away increases the value and down/towards decreases it unless reversed.
 */
final class SpinnerMouseWheelListener implements MouseWheelListener {

	private final JSpinner spinner;
	private final boolean reversed;

	/**
	 * Instantiates a new mouse wheel listener
	 * @param spinner the spinner
	 * @param reversed if true then up/away decreases the value and down/towards increases it.
	 */
	SpinnerMouseWheelListener(JSpinner spinner, boolean reversed) {
		this.spinner = requireNonNull(spinner);
		this.reversed = reversed;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent event) {
		if (spinner.isEnabled()) {
			int wheelRotation = event.getWheelRotation();
			if (wheelRotation != 0) {
				Object newValue = (reversed ? wheelRotation > 0 : wheelRotation < 0) ?
								spinner.getModel().getNextValue() : spinner.getModel().getPreviousValue();
				if (newValue != null) {
					spinner.getModel().setValue(newValue);
				}
			}
		}
	}
}
