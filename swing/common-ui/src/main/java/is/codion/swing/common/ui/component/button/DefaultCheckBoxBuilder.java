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

import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.ToggleControl;

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
	public CheckBoxBuilder toggle(ToggleControl toggleControl) {
		super.toggle(toggleControl);

		return nullable(toggleControl.value().isNullable());
	}

	@Override
	public CheckBoxBuilder nullable(boolean nullable) {
		this.nullable = nullable;
		return this;
	}

	@Override
	protected JCheckBox createButton() {
		return nullable ? nullableCheckBox() : new JCheckBox();
	}

	@Override
	protected ComponentValue<Boolean, JCheckBox> createComponentValue(JToggleButton component) {
		ComponentValue<Boolean, JCheckBox> componentValue = component instanceof NullableCheckBox ?
						new NullableCheckBoxValue((NullableCheckBox) component) :
						new CheckBoxValue((JCheckBox) component);
		linkedObservableStates.forEach(state -> state.addConsumer(new SetComponentValue(componentValue)));

		return componentValue;
	}

	@Override
	protected boolean supportsNull() {
		return true;
	}
}
