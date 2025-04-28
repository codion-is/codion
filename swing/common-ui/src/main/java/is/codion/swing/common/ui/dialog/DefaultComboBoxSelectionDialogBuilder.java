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
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import java.awt.Dimension;
import java.util.Collection;
import java.util.Optional;

import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static is.codion.swing.common.ui.component.Components.comboBox;
import static java.util.Objects.requireNonNull;

final class DefaultComboBoxSelectionDialogBuilder<T> extends AbstractSelectionDialogBuilder<T, ComboBoxSelectionDialogBuilder<T>>
				implements ComboBoxSelectionDialogBuilder<T> {

	private T defaultSelection;

	DefaultComboBoxSelectionDialogBuilder(Collection<T> values) {
		super(values);
	}

	@Override
	public ComboBoxSelectionDialogBuilder<T> defaultSelection(T defaultSelection) {
		if (!values.contains(requireNonNull(defaultSelection))) {
			throw new IllegalArgumentException("defaultSelection was not found in selection items");
		}
		this.defaultSelection = defaultSelection;
		return this;
	}

	@Override
	public Optional<T> select() {
		FilterComboBoxModel<T> comboBoxModel = FilterComboBoxModel.builder(values).build();
		comboBoxModel.selection().item().set(defaultSelection);
		JComboBox<T> comboBox = comboBox(comboBoxModel)
						.build();
		Control okControl = Control.builder()
						.command(() -> disposeParentWindow(comboBox))
						.enabled(allowEmptySelection ? null : comboBoxModel.selection().empty().not())
						.build();
		State cancelledState = State.state();
		Runnable onCancel = () -> {
			comboBoxModel.selection().clear();
			cancelledState.set(true);
		};
		OkCancelDialogBuilder dialogBuilder = new DefaultOkCancelDialogBuilder(comboBox)
						.owner(owner)
						.locationRelativeTo(locationRelativeTo)
						.title(createTitle())
						.okAction(okControl)
						.onCancel(onCancel);
		onBuildConsumers.forEach(dialogBuilder::onBuild);

		JDialog dialog = dialogBuilder.build();
		if (dialog.getSize().width > MAX_SELECT_VALUE_DIALOG_WIDTH) {
			dialog.setSize(new Dimension(MAX_SELECT_VALUE_DIALOG_WIDTH, dialog.getSize().height));
		}
		dialog.setVisible(true);
		if (cancelledState.get()) {
			throw new CancelException();
		}

		return Optional.ofNullable(comboBoxModel.selection().item().get());
	}

	private String createTitle() {
		return title == null ? MESSAGES.getString("select_values") : title.get();
	}
}
