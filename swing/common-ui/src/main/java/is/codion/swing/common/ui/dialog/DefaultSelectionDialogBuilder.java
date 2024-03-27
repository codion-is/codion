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
import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Objects.requireNonNull;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

final class DefaultSelectionDialogBuilder<T> extends AbstractDialogBuilder<SelectionDialogBuilder<T>>
				implements SelectionDialogBuilder<T> {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultSelectionDialogBuilder.class.getName());

	private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;

	private final Collection<T> values;
	private final Collection<T> defaultSelection = new ArrayList<>();
	private boolean allowEmptySelection = false;

	DefaultSelectionDialogBuilder(Collection<T> values) {
		if (requireNonNull(values).isEmpty()) {
			throw new IllegalArgumentException("One or more items to select from must be provided");
		}
		this.values = new ArrayList<>(values);
	}

	@Override
	public SelectionDialogBuilder<T> defaultSelection(T defaultSelection) {
		return defaultSelection(Collections.singletonList(requireNonNull(defaultSelection)));
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
	public Optional<T> selectSingle() {
		return selectSingle(this);
	}

	@Override
	public Collection<T> select() {
		return select(this, false);
	}

	private static <T> Optional<T> selectSingle(DefaultSelectionDialogBuilder<T> builder) {
		List<T> selected = select(builder, true);
		if (selected.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(selected.get(0));
	}

	private static <T> List<T> select(DefaultSelectionDialogBuilder<T> builder, boolean singleSelection) {
		JList<T> list = createList(builder, singleSelection);
		Control okControl = Control.builder(() -> Utilities.parentDialog(list).dispose())
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

		builder.defaultSelection.stream()
						.mapToInt(listModel::indexOf)
						.boxed()
						.sorted(Collections.reverseOrder())//reverse order so that topmost item is selected last
						.mapToInt(Integer::intValue)
						.peek(index -> list.getSelectionModel().addSelectionInterval(index, index))
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
			return builder.titleProvider == null ? MESSAGES.getString("select_value") : builder.titleProvider.get();
		}

		return builder.titleProvider == null ? MESSAGES.getString("select_values") : builder.titleProvider.get();
	}
}
