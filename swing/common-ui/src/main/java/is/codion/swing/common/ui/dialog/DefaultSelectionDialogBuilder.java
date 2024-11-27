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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.resource.MessageBundle;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

final class DefaultSelectionDialogBuilder<T> extends AbstractDialogBuilder<SelectionDialogBuilder<T>>
				implements SelectionDialogBuilder<T> {

	private static final MessageBundle MESSAGES =
					messageBundle(DefaultSelectionDialogBuilder.class, getBundle(DefaultSelectionDialogBuilder.class.getName()));

	private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;

	private final Collection<T> values;
	private final Collection<T> defaultSelection = new ArrayList<>();
	private boolean allowEmptySelection = false;
	private Dimension dialogSize;

	DefaultSelectionDialogBuilder(Collection<T> values) {
		if (requireNonNull(values).isEmpty()) {
			throw new IllegalArgumentException("One or more items to select from must be provided");
		}
		this.values = new ArrayList<>(values);
	}

	@Override
	public SelectionDialogBuilder<T> defaultSelection(T defaultSelection) {
		return defaultSelection(singletonList(requireNonNull(defaultSelection)));
	}

	@Override
	public SelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection) {
		if (!values.containsAll(requireNonNull(defaultSelection))) {
			throw new IllegalArgumentException("defaultSelection was not found in selection items");
		}
		this.defaultSelection.addAll(defaultSelection);
		return this;
	}

	@Override
	public SelectionDialogBuilder<T> allowEmptySelection(boolean allowEmptySelection) {
		this.allowEmptySelection = allowEmptySelection;
		return this;
	}

	@Override
	public SelectionDialogBuilder<T> dialogSize(Dimension dialogSize) {
		this.dialogSize = requireNonNull(dialogSize);
		return this;
	}

	@Override
	public Optional<T> selectSingle() {
		return selectSingle(this, dialogSize);
	}

	@Override
	public Collection<T> select() {
		return select(this, false, dialogSize);
	}

	private static <T> Optional<T> selectSingle(DefaultSelectionDialogBuilder<T> builder, Dimension dialogSize) {
		List<T> selected = select(builder, true, dialogSize);
		if (selected.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(selected.get(0));
	}

	private static <T> List<T> select(DefaultSelectionDialogBuilder<T> builder, boolean singleSelection, Dimension dialogSize) {
		JList<T> list = createList(builder, singleSelection);
		Control okControl = Control.builder()
						.command(() -> disposeParentWindow(list))
						.enabled(builder.allowEmptySelection ? null : createSelectionNonEmptyState(list))
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
		JDialog dialog = new DefaultOkCancelDialogBuilder(new JScrollPane(list))
						.owner(builder.owner)
						.locationRelativeTo(builder.locationRelativeTo)
						.title(createTitle(builder, singleSelection))
						.size(dialogSize)
						.okAction(okControl)
						.onCancel(onCancel)
						.build();
		if (dialog.getSize().width > MAX_SELECT_VALUE_DIALOG_WIDTH) {
			dialog.setSize(new Dimension(MAX_SELECT_VALUE_DIALOG_WIDTH, dialog.getSize().height));
		}
		dialog.setVisible(true);
		if (cancelledState.get()) {
			throw new CancelException();
		}

		return list.getSelectedValuesList();
	}

	private static <T> JList<T> createList(DefaultSelectionDialogBuilder<T> builder, boolean singleSelection) {
		DefaultListModel<T> listModel = new DefaultListModel<>();
		builder.values.forEach(listModel::addElement);
		JList<T> list = new JList<>(listModel);
		if (singleSelection) {
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		}
		list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");

		List<Integer> sortedDefaultSelectedIndexes = builder.defaultSelection.stream()
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

	private static State createSelectionNonEmptyState(JList<?> list) {
		State selectionNonEmptyState = State.state(!list.getSelectionModel().isSelectionEmpty());
		list.addListSelectionListener(e -> selectionNonEmptyState.set(!list.getSelectionModel().isSelectionEmpty()));

		return selectionNonEmptyState;
	}

	private static <T> String createTitle(DefaultSelectionDialogBuilder<T> builder, boolean singleSelection) {
		if (singleSelection) {
			return builder.title == null ? MESSAGES.getString("select_value") : builder.title.get();
		}

		return builder.title == null ? MESSAGES.getString("select_values") : builder.title.get();
	}
}
