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
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.swing.common.ui.control.Control;

import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Collections.reverseOrder;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

final class DefaultListSelectionDialogBuilder<T> extends AbstractSelectionDialogBuilder<T, ListSelectionDialogBuilder<T>>
				implements ListSelectionDialogBuilder<T> {

	private Dimension dialogSize;

	DefaultListSelectionDialogBuilder(Collection<T> values) {
		super(values);
	}

	@Override
	public ListSelectionDialogBuilder<T> dialogSize(Dimension dialogSize) {
		this.dialogSize = requireNonNull(dialogSize);
		return this;
	}

	@Override
	public Optional<T> selectSingle() {
		return selectSingle(dialogSize);
	}

	@Override
	public Collection<T> select() {
		return select(false, dialogSize);
	}

	private Optional<T> selectSingle(Dimension dialogSize) {
		List<T> selected = select(true, dialogSize);
		if (selected.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(selected.get(0));
	}

	private List<T> select(boolean singleSelection, Dimension dialogSize) {
		JList<T> list = createList(singleSelection);
		Control okControl = Control.builder()
						.command(() -> disposeParentWindow(list))
						.enabled(allowEmptySelection ? null : createSelectionNonEmptyState(list))
						.build();
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					okControl.actionPerformed(null);
				}
			}
		});
		State cancelledState = State.state();
		Runnable onCancel = () -> {
			list.clearSelection();
			cancelledState.set(true);
		};
		OkCancelDialogBuilder dialogBuilder = new DefaultOkCancelDialogBuilder(new JScrollPane(list))
						.owner(owner)
						.locationRelativeTo(locationRelativeTo)
						.title(createTitle(singleSelection))
						.size(dialogSize)
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

		return list.getSelectedValuesList();
	}

	private JList<T> createList(boolean singleSelection) {
		DefaultListModel<T> listModel = new DefaultListModel<>();
		values.forEach(listModel::addElement);
		JList<T> list = new JList<>(listModel);
		if (singleSelection) {
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");

		List<Integer> sortedDefaultSelectedIndexes = defaultSelection.stream()
						.mapToInt(listModel::indexOf)
						.boxed()
						.sorted(reverseOrder())//reverse order so that topmost item is selected last
						.toList();
		sortedDefaultSelectedIndexes.forEach(index -> list.getSelectionModel().addSelectionInterval(index, index));
		sortedDefaultSelectedIndexes.stream()
						.mapToInt(Integer::intValue)
						.min()
						.ifPresent(list::ensureIndexIsVisible);

		return list;
	}

	private String createTitle(boolean singleSelection) {
		if (singleSelection) {
			return title == null ? MESSAGES.getString("select_value") : title.get();
		}

		return title == null ? MESSAGES.getString("select_values") : title.get();
	}

	private static State createSelectionNonEmptyState(JList<?> list) {
		State selectionNonEmptyState = State.state(!list.getSelectionModel().isSelectionEmpty());
		list.addListSelectionListener(e -> selectionNonEmptyState.set(!list.getSelectionModel().isSelectionEmpty()));

		return selectionNonEmptyState;
	}
}
