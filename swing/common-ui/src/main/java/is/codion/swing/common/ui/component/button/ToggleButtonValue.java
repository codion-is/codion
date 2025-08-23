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

import is.codion.swing.common.ui.component.value.AbstractComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

final class ToggleButtonValue<C extends AbstractButton> extends AbstractComponentValue<C, Boolean> {

	ToggleButtonValue(C button) {
		super(button, false);
		button.getModel().addItemListener(new NotifyOnItemEvent());
	}

	@Override
	protected Boolean getComponentValue() {
		return component().isSelected();
	}

	@Override
	protected void setComponentValue(@Nullable Boolean value) {
		component().setSelected(value != null && value);
	}

	private final class NotifyOnItemEvent implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent itemEvent) {
			notifyListeners();
		}
	}
}
