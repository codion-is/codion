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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.component.table.FilterTableModel;
import is.codion.common.model.component.table.FilterTableSort;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.swing.common.model.component.list.FilterListSelection;

import org.jspecify.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A Swing {@link javax.swing.table.TableModel} coat over the UI-agnostic
 * {@link FilterTableModel}: delegates the rich model surface to a common instance — built with a
 * {@link javax.swing.ListSelectionModel} based selection ({@link FilterListSelection}) and a
 * {@code ProgressWorker} based refresher — and adds the {@link javax.swing.table.TableModel} methods
 * and cell editing (via {@link RowEditor}), firing {@code TableModelEvent}s off the common model's items.
 */
final class DefaultSwingFilterTableModel<R, C> extends AbstractTableModel implements SwingFilterTableModel<R, C> {

	private final FilterTableModel<R, C> model;
	private final FilterListSelection<R> selection;
	private final Function<SwingFilterTableModel<R, C>, RowEditor<R, C>> rowEditorFactory;
	private final RemoveSelectionListener removeSelectionListener;

	private @Nullable RowEditor<R, C> rowEditor;

	private DefaultSwingFilterTableModel(DefaultBuilder<R, C> builder) {
		this.rowEditorFactory = builder.rowEditorFactory;
		this.model = builder.builder
						.selection(FilterListSelection::filterListSelection)
						.refresher(modelItems -> new TableRefreshWorker<>(builder.itemSupplier, builder.async, builder.onRefreshException, modelItems))
						.listener(new TableModelAdapter())
						.build();
		this.selection = (FilterListSelection<R>) model.selection();
		this.removeSelectionListener = new RemoveSelectionListener();
		if (builder.refresh) {
			model.items().refresh();
		}
		addTableModelListener(removeSelectionListener);
	}

	@Override
	public Items<R> items() {
		return model.items();
	}

	@Override
	public ColumnValues<C> values() {
		return model.values();
	}

	@Override
	public TableColumns<R, C> columns() {
		return model.columns();
	}

	@Override
	public TableConditionModel<C> filters() {
		return model.filters();
	}

	@Override
	public FilterTableSort<R, C> sort() {
		return model.sort();
	}

	@Override
	public Export<C> export() {
		return model.export();
	}

	@Override
	public FilterListSelection<R> selection() {
		return selection;
	}

	@Override
	public int getColumnCount() {
		return model.columns().identifiers().size();
	}

	@Override
	public int getRowCount() {
		return model.items().included().size();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return model.columns().columnClass(model.columns().identifier(columnIndex));
	}

	@Override
	public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
		return model.values().value(rowIndex, model.columns().identifier(columnIndex));
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return rowEditor().editable(model.items().included().get(rowIndex), model.columns().identifier(columnIndex));
	}

	@Override
	public void setValueAt(@Nullable Object value, int rowIndex, int columnIndex) {
		rowEditor().set(value, rowIndex, model.items().included().get(rowIndex), model.columns().identifier(columnIndex));
	}

	@Override
	public RowEditor<R, C> rowEditor() {
		if (rowEditor == null) {
			rowEditor = rowEditorFactory.apply(this);
		}

		return rowEditor;
	}

	@Override
	public void addTableModelListener(TableModelListener listener) {
		super.addTableModelListener(listener);
		if (listener instanceof JTable) {
			// JTable handles removing the selected indexes on row removal
			removeTableModelListener(removeSelectionListener);
		}
	}

	@Override
	public void removeTableModelListener(TableModelListener listener) {
		super.removeTableModelListener(listener);
		if (listener instanceof JTable) {
			// JTable handles removing the selected indexes on row removal
			addTableModelListener(removeSelectionListener);
		}
	}

	private final class RemoveSelectionListener implements TableModelListener {

		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.DELETE) {
				selection().removeIndexInterval(e.getFirstRow(), e.getLastRow());
			}
		}
	}

	private final class TableModelAdapter implements ItemsListener {

		@Override
		public void inserted(int firstIndex, int lastIndex) {
			fireTableRowsInserted(firstIndex, lastIndex);
		}

		@Override
		public void updated(int firstIndex, int lastIndex) {
			fireTableRowsUpdated(firstIndex, lastIndex);
		}

		@Override
		public void deleted(int firstIndex, int lastIndex) {
			fireTableRowsDeleted(firstIndex, lastIndex);
		}

		@Override
		public void changed() {
			fireTableDataChanged();
		}
	}

	private static final class DefaultRowEditor<R, C> implements RowEditor<R, C> {

		@Override
		public boolean editable(R row, C identifier) {
			return false;
		}

		@Override
		public void set(@Nullable Object value, int rowIndex, R row, C identifier) {}
	}

	private static final class DefaultRowEditorFactory<R, C> implements Function<SwingFilterTableModel<R, C>, RowEditor<R, C>> {

		@Override
		public RowEditor<R, C> apply(SwingFilterTableModel<R, C> tableModel) {
			return new DefaultRowEditor<>();
		}
	}

	static final class DefaultColumnsStep implements Builder.ColumnsStep {

		@Override
		public <R, C> Builder<R, C> columns(TableColumns<R, C> columns) {
			return new DefaultBuilder<>(FilterTableModel.builder().columns(columns));
		}
	}

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		static final Builder.ColumnsStep COLUMNS = new DefaultColumnsStep();

		private final FilterTableModel.Builder<R, C> builder;

		private @Nullable Supplier<? extends Collection<R>> itemSupplier;
		private boolean async = FilterModel.ASYNC.getOrThrow();
		private @Nullable Consumer<Exception> onRefreshException;
		private Function<SwingFilterTableModel<R, C>, RowEditor<R, C>> rowEditorFactory = new DefaultRowEditorFactory<>();
		private boolean refresh = false;

		private DefaultBuilder(FilterTableModel.Builder<R, C> builder) {
			this.builder = builder;
		}

		@Override
		public Builder<R, C> filters(Supplier<Map<C, ConditionModel<?>>> filters) {
			builder.filters(filters);
			return this;
		}

		@Override
		public Builder<R, C> items(Supplier<? extends Collection<R>> items) {
			this.itemSupplier = requireNonNull(items);
			return this;
		}

		@Override
		public Builder<R, C> validator(Predicate<R> validator) {
			builder.validator(validator);
			return this;
		}

		@Override
		public Builder<R, C> async(boolean async) {
			this.async = async;
			return this;
		}

		@Override
		public Builder<R, C> onRefreshException(Consumer<Exception> onRefreshException) {
			this.onRefreshException = requireNonNull(onRefreshException);
			return this;
		}

		@Override
		public Builder<R, C> rowEditor(Function<SwingFilterTableModel<R, C>, RowEditor<R, C>> rowEditor) {
			this.rowEditorFactory = requireNonNull(rowEditor);
			return this;
		}

		@Override
		public Builder<R, C> included(Predicate<R> included) {
			builder.included(included);
			return this;
		}

		@Override
		public Builder<R, C> refresh(boolean refresh) {
			this.refresh = refresh;
			return this;
		}

		@Override
		public Builder<R, C> onSelectionChanged(Runnable listener) {
			builder.onSelectionChanged(listener);
			return this;
		}

		@Override
		public Builder<R, C> onItemSelected(Consumer<R> item) {
			builder.onItemSelected(item);
			return this;
		}

		@Override
		public Builder<R, C> onItemsSelected(Consumer<List<R>> items) {
			builder.onItemsSelected(items);
			return this;
		}

		@Override
		public Builder<R, C> onIndexSelected(Consumer<Integer> index) {
			builder.onIndexSelected(index);
			return this;
		}

		@Override
		public Builder<R, C> onIndexesSelected(Consumer<List<Integer>> indexes) {
			builder.onIndexesSelected(indexes);
			return this;
		}

		@Override
		public SwingFilterTableModel<R, C> build() {
			return new DefaultSwingFilterTableModel<>(this);
		}
	}

	private static final class TableRefreshWorker<R> extends FilterModel.AbstractRefresher<R> {

		private final Items<R> items;

		private TableRefreshWorker(@Nullable Supplier<? extends Collection<R>> supplier, boolean async,
		                           @Nullable Consumer<Exception> onException, Items<R> items) {
			super((Supplier<Collection<R>>) supplier, async, onException);
			this.items = items;
		}

		@Override
		protected void processResult(Collection<R> result) {
			items.set(result);
		}
	}
}
