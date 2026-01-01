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
package is.codion.demos.chinook.ui;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;

final class TriStateCheckBoxValue extends AbstractComponentValue<FlatTriStateCheckBox, Boolean> {

	TriStateCheckBoxValue() {
		this(new FlatTriStateCheckBox());
	}

	TriStateCheckBoxValue(FlatTriStateCheckBox checkBox) {
		super(checkBox);
		component().addItemListener(e -> notifyObserver());
	}

	@Override
	protected Boolean getComponentValue() {
		return component().getChecked();
	}

	@Override
	protected void setComponentValue(Boolean value) {
		component().setChecked(value);
	}
}
