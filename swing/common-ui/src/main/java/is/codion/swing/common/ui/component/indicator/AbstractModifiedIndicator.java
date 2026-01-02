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
package is.codion.swing.common.ui.component.indicator;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

abstract class AbstractModifiedIndicator implements Consumer<Boolean> {

	private static final String LABELED_BY_PROPERTY = "labeledBy";

	private final JComponent component;

	protected AbstractModifiedIndicator(JComponent component) {
		this.component = requireNonNull(component);
	}

	@Override
	public final void accept(Boolean modified) {
		JLabel label = (JLabel) component.getClientProperty(LABELED_BY_PROPERTY);
		if (label != null) {
			SwingUtilities.invokeLater(() -> update(label, modified));
		}
	}

	protected abstract void update(JLabel label, boolean modified);
}
