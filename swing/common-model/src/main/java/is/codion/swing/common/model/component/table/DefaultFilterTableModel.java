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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.model.FilterModel;
import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableModel<R, C> extends AbstractTableModel implements FilterTableModel<R, C> {

	/**
	 * A Comparator for comparing {@link Comparable} instances.
	 */
	static final Comparator<Comparable<Object>> COMPARABLE_COMPARATOR = Comparable::compareTo;

	/**
	 * A Comparator for comparing Objects according to their toString() value.
	 */
	static final Comparator<?> STRING_COMPARATOR = Comparator.comparing(Object::toString);

	private final DefaultFilterTableItems<R, C> items;
	private final DefaultColumnValues columnValues = new DefaultColumnValues();
	private final Function<FilterTableModel<R, C>, RowEditor<R, C>> rowEditorFactory;
	private final RemoveSelectionListener removeSelectionListener;

	private RowEditor<R, C> rowEditor;

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.items = new DefaultFilterTableItems<>(this, builder.validator, builder.supplier,
						builder.refreshStrategy, builder.asyncRefresh, builder.columns, builder.filterModelFactory);
		this.rowEditorFactory = builder.rowEditorFactory;
		this.removeSelectionListener = new RemoveSelectionListener();
		addTableModelListener(removeSelectionListener);
	}

	@Override
	public FilterTableModelItems<R> items() {
		return items;
	}

	@Override
	public ColumnValues<C> values() {
		return columnValues;
	}

	@Override
	public int getColumnCount() {
		return items.columns.identifiers().size();
	}

	@Override
	public int getRowCount() {
		return items.visible().count();
	}

	@Override
	public TableSelection<R> selection() {
		return items.selection;
	}

	@Override
	public TableConditionModel<C> filters() {
		return items.filters;
	}

	@Override
	public FilterTableSortModel<R, C> sort() {
		return items.sorter;
	}

	@Override
	public Class<?> getColumnClass(C identifier) {
		return items.columns.columnClass(requireNonNull(identifier));
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return items.columns.columnClass(items.columns.identifier(columnIndex));
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return columnValues.valueAt(rowIndex, columnIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return rowEditor().editable(items.visible().get(rowIndex), items.columns.identifier(columnIndex));
	}

	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		rowEditor().set(value, items.visible().get(rowIndex), items.columns.identifier(columnIndex));
	}

	@Override
	public TableColumns<R, C> columns() {
		return items.columns;
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

	private RowEditor<R, C> rowEditor() {
		if (rowEditor == null) {
			rowEditor = rowEditorFactory.apply(this);
		}

		return rowEditor;
	}

	private final class RemoveSelectionListener implements TableModelListener {

		@Override
		public void tableChanged(TableModelEvent e) {
			if (e.getType() == TableModelEvent.DELETE) {
				items.selection.removeIndexInterval(e.getFirstRow(), e.getLastRow());
			}
		}
	}

	private final class DefaultColumnValues implements ColumnValues<C> {

		@Override
		public <T> List<T> get(C identifier) {
			return (List<T>) values(IntStream.range(0, items.visible().count()).boxed(), validateIdentifier(identifier));
		}

		@Override
		public <T> List<T> selected(C identifier) {
			return (List<T>) values(selection().indexes().get().stream(), validateIdentifier(identifier));
		}

		@Override
		public String string(int rowIndex, C identifier) {
			return items.columns.string(items.visible().get(rowIndex), requireNonNull(identifier));
		}

		@Override
		public Object value(int rowIndex, C identifier) {
			return items.columns.value(items.visible().get(rowIndex), identifier);
		}

		private List<Object> values(Stream<Integer> rowIndexStream, C identifier) {
			return rowIndexStream.map(rowIndex -> value(rowIndex, identifier)).collect(toList());
		}

		private Object valueAt(int rowIndex, int columnIndex) {
			return value(rowIndex, items.columns.identifier(columnIndex));
		}

		private C validateIdentifier(C identifier) {
			int modelIndex = items.columns.identifiers().indexOf(identifier);
			if (modelIndex == -1) {
				throw new IllegalArgumentException("Unknown column identifier: " + identifier);
			}

			return identifier;
		}
	}

	private static final class DefaultColumnFilterFactory<C> implements Supplier<Map<C, ConditionModel<?>>> {

		private final TableColumns<?, C> columns;

		private DefaultColumnFilterFactory(TableColumns<?, C> columns) {
			this.columns = columns;
		}

		@Override
		public Map<C, ConditionModel<?>> get() {
			Map<C, ConditionModel<?>> columnFilterModels = new HashMap<>();
			for (C identifier : columns.identifiers()) {
				Class<?> columnClass = columns.columnClass(requireNonNull(identifier));
				if (Comparable.class.isAssignableFrom(columnClass)) {
					columnFilterModels.put(identifier, ConditionModel.builder(columnClass).build());
				}
			}

			return columnFilterModels;
		}
	}

	private static final class DefaultRowEditor<R, C> implements RowEditor<R, C> {

		@Override
		public boolean editable(R row, C identifier) {
			return false;
		}

		@Override
		public void set(Object value, R row, C identifier) {}
	}

	private static final class DefaultRowEditorFactory<R, C> implements Function<FilterTableModel<R, C>, RowEditor<R, C>> {

		@Override
		public RowEditor<R, C> apply(FilterTableModel<R, C> tableModel) {
			return new DefaultRowEditor<>();
		}
	}

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		private final TableColumns<R, C> columns;

		private Supplier<? extends Collection<R>> supplier;
		private Predicate<R> validator = new ValidPredicate<>();
		private Supplier<Map<C, ConditionModel<?>>> filterModelFactory;
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private boolean asyncRefresh = FilterModel.ASYNC_REFRESH.getOrThrow();
		private Function<FilterTableModel<R, C>, RowEditor<R, C>> rowEditorFactory = new DefaultRowEditorFactory<>();

		DefaultBuilder(TableColumns<R, C> columns) {
			if (requireNonNull(columns).identifiers().isEmpty()) {
				throw new IllegalArgumentException("No columns specified");
			}
			this.columns = validateIdentifiers(columns);
			this.filterModelFactory = new DefaultColumnFilterFactory<>(columns);
		}

		@Override
		public Builder<R, C> filterModelFactory(Supplier<Map<C, ConditionModel<?>>> filterModelFactory) {
			this.filterModelFactory = requireNonNull(filterModelFactory);
			return this;
		}

		@Override
		public Builder<R, C> supplier(Supplier<? extends Collection<R>> supplier) {
			this.supplier = requireNonNull(supplier);
			return this;
		}

		@Override
		public Builder<R, C> validator(Predicate<R> validator) {
			this.validator = requireNonNull(validator);
			return this;
		}

		@Override
		public Builder<R, C> refreshStrategy(RefreshStrategy refreshStrategy) {
			this.refreshStrategy = requireNonNull(refreshStrategy);
			return this;
		}

		@Override
		public Builder<R, C> asyncRefresh(boolean asyncRefresh) {
			this.asyncRefresh = asyncRefresh;
			return this;
		}

		@Override
		public Builder<R, C> rowEditor(Function<FilterTableModel<R, C>, RowEditor<R, C>> rowEditor) {
			this.rowEditorFactory = requireNonNull(rowEditor);
			return this;
		}

		@Override
		public FilterTableModel<R, C> build() {
			return new DefaultFilterTableModel<>(this);
		}

		private TableColumns<R, C> validateIdentifiers(TableColumns<R, C> columns) {
			if (new HashSet<>(columns.identifiers()).size() != columns.identifiers().size()) {
				throw new IllegalArgumentException("Column identifiers are not unique");
			}

			return columns;
		}

		private static final class ValidPredicate<R> implements Predicate<R> {

			@Override
			public boolean test(R r) {
				return true;
			}
		}
	}
}
