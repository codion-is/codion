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

import javax.swing.JCheckBox;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import static is.codion.swing.common.ui.component.button.NullableCheckBox.nullableCheckBox;

final class DefaultCheckBoxBuilder extends DefaultToggleButtonBuilder<JCheckBox, CheckBoxBuilder> implements CheckBoxBuilder {

	private boolean nullable = false;

	DefaultCheckBoxBuilder() {
		horizontalAlignment(SwingConstants.LEADING);
	}

	@Override
	public CheckBoxBuilder nullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}

	@Override
	protected JToggleButton createToggleButton() {
		return nullable ? nullableCheckBox() : new JCheckBox();
	}
}
