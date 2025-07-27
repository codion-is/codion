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

import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

final class DefaultItemSpinnerBuilder<T> extends AbstractSpinnerBuilder<T, ItemSpinnerBuilder<T>>
				implements ItemSpinnerBuilder<T> {

	DefaultItemSpinnerBuilder() {
		super(new SpinnerListModel());
	}

	@Override
	public ItemSpinnerBuilder<T> model(SpinnerModel model) {
		if (!(model instanceof SpinnerListModel)) {
			throw new IllegalArgumentException("model must be of type SpinnerListModel");
		}
		return super.model(model);
	}

	@Override
	protected ComponentValue<T, JSpinner> createComponentValue(JSpinner component) {
		return new SpinnerItemValue<>(component);
	}
}
