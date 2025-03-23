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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.indicator;

import is.codion.common.state.ObservableState;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

final class FlatLafValidIndicator {

	private final JComponent component;

	FlatLafValidIndicator(JComponent component, ObservableState valid) {
		this.component = component;
		valid.addConsumer(this::update);
		update(valid.get());
	}

	private void update(boolean valid) {
		SwingUtilities.invokeLater(() ->
						component.putClientProperty(FlatClientProperties.OUTLINE, valid ? null : FlatClientProperties.OUTLINE_ERROR));
	}
}
