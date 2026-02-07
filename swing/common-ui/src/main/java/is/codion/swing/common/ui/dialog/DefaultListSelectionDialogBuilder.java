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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.control.Control;

import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static is.codion.swing.common.ui.window.Windows.resizeToFitScreen;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Collections.reverseOrder;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;

final class DefaultListSelectionDialogBuilder<T> extends AbstractSelectionDialogBuilder<T, ListSelectionDialogBuilder<T>>
				implements ListSelectionDialogBuilder<T> {

	private final Collection<T> defaultSelection = new ArrayList<>();

	private @Nullable Comparator<T> comparator;
	private @Nullable Dimension dialogSize;

	DefaultListSelectionDialogBuilder(Collection<T> values) {
		super(values);
	}

	@Override
	public ListSelectionDialogBuilder<T> defaultSelection(T defaultSelection) {
		return defaultSelection(singletonList(requireNonNull(defaultSelection)));
	}

	@Override
	public ListSelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection) {
		if (!values.containsAll(requireNonNull(defaultSelection))) {
			throw new IllegalArgumentException("defaultSelection was not found in selection items");
		}
		this.defaultSelection.clear();
		this.defaultSelection.addAll(defaultSelection);
		return this;
	}

	@Override
	public ListSelectionDialogBuilder<T> dialogSize(@Nullable Dimension dialogSize) {
		this.dialogSize = dialogSize;
		return this;
	}

	@Override
	public ListSelectionDialogBuilder<T> comparator(@Nullable Comparator<T> comparator) {
		this.comparator = comparator;
		return this;
	}

	@Override
	public SelectionStep<T> select() {
		return new DefaultSelectionStep();
	}

	private final class DefaultSelectionStep implements SelectionStep<T> {

		@Override
		public Optional<T> single() {
			List<T> selected = select(true, dialogSize);
			if (selected.isEmpty()) {
				return Optional.empty();
			}

			return Optional.of(selected.get(0));
		}

		@Override
		public Collection<T> multiple() {
			return select(false, dialogSize);
		}

		private List<T> select(boolean singleSelection, @Nullable Dimension dialogSize) {
			FilterList<T> list = createList(singleSelection);
			Control okControl = Control.builder()
							.command(() -> Ancestor.window().of(list).dispose())
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
			OkCancelDialogBuilder dialogBuilder = DefaultOkCancelDialogBuilder.OK_CANCEL_COMPONENT
							.component(new JScrollPane(list))
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
			resizeToFitScreen(dialog);
			dialog.setVisible(true);
			if (cancelledState.is()) {
				throw new CancelException();
			}

			return list.getSelectedValuesList();
		}

		private FilterList<T> createList(boolean singleSelection) {
			FilterListModel<T> model = FilterListModel.builder()
							.items(values)
							.comparator(comparator)
							.build();
			FilterList<T> list = FilterList.builder()
							.model(model)
							.items()
							.build();
			if (singleSelection) {
				list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			}
			list.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");

			List<Integer> sortedDefaultSelectedIndexes = defaultSelection.stream()
							.mapToInt(model.items().included()::indexOf)
							.boxed()
							.sorted(reverseOrder())//reverse order so that topmost item is selected last
							.collect(toList());
			sortedDefaultSelectedIndexes.forEach(model.selection().indexes()::add);
			sortedDefaultSelectedIndexes.stream()
							.mapToInt(Integer::intValue)
							.min()
							.ifPresent(list::ensureIndexIsVisible);

			return list;
		}

		private @Nullable String createTitle(boolean singleSelection) {
			if (singleSelection) {
				return title == null ? MESSAGES.getString("select_value") : title.get();
			}

			return title == null ? MESSAGES.getString("select_values") : title.get();
		}

		private State createSelectionNonEmptyState(FilterList<?> list) {
			State selectionNonEmptyState = State.state(!list.getSelectionModel().isSelectionEmpty());
			list.addListSelectionListener(e -> selectionNonEmptyState.set(!list.getSelectionModel().isSelectionEmpty()));

			return selectionNonEmptyState;
		}
	}
}
