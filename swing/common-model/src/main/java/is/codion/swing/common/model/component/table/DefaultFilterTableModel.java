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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.VisibleItems.ItemsListener;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.model.component.list.AbstractRefreshWorker;
import is.codion.swing.common.model.component.list.FilterListSelection;

import org.jspecify.annotations.Nullable;

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

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.common.value.Value.Notify.SET;
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

	private final Items<R> items;
	private final TableColumns<R, C> columns;
	private final TableConditionModel<C> filters;
	private final FilterListSelection<R> selection;
	private final DefaultFilterTableSort<R, C> sort;
	private final DefaultColumnValues columnValues = new DefaultColumnValues();
	private final Function<FilterTableModel<R, C>, Editor<R, C>> editorFactory;
	private final RemoveSelectionListener removeSelectionListener;

	private @Nullable Editor<R, C> editor;

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.columns = builder.columns;
		this.filters = tableConditionModel(builder.filters);
		this.sort = new DefaultFilterTableSort<>(columns);
		this.editorFactory = builder.editorFactory;
		this.items = Items.builder()
						.refresher(builder::createRefresher)
						.selection(FilterListSelection::filterListSelection)
						.sort(sort)
						.validator(builder.validator)
						.visiblePredicate(new DefaultVisiblePredicate<>(builder.columns, filters))
						.refreshStrategy(builder.refreshStrategy)
						.listener(new TableModelAdapter())
						.build();
		this.items.visible().predicate().set(builder.visiblePredicate);
		this.selection = (FilterListSelection<R>) items.visible().selection();
		this.removeSelectionListener = new RemoveSelectionListener();
		addTableModelListener(removeSelectionListener);
	}

	@Override
	public Items<R> items() {
		return items;
	}

	@Override
	public ColumnValues<C> values() {
		return columnValues;
	}

	@Override
	public int getColumnCount() {
		return columns.identifiers().size();
	}

	@Override
	public int getRowCount() {
		return items.visible().count();
	}

	@Override
	public FilterListSelection<R> selection() {
		return selection;
	}

	@Override
	public TableConditionModel<C> filters() {
		return filters;
	}

	@Override
	public FilterTableSort<R, C> sort() {
		return sort;
	}

	@Override
	public Class<?> getColumnClass(C identifier) {
		return columns.columnClass(requireNonNull(identifier));
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columns.columnClass(columns.identifier(columnIndex));
	}

	@Override
	public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
		return columnValues.valueAt(rowIndex, columnIndex);
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return editor().editable(items.visible().get(rowIndex), columns.identifier(columnIndex));
	}

	@Override
	public void setValueAt(@Nullable Object value, int rowIndex, int columnIndex) {
		editor().set(value, rowIndex, items.visible().get(rowIndex), columns.identifier(columnIndex));
	}

	@Override
	public TableColumns<R, C> columns() {
		return columns;
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

	private Editor<R, C> editor() {
		if (editor == null) {
			editor = editorFactory.apply(this);
		}

		return editor;
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
			return columns.string(items.visible().get(rowIndex), requireNonNull(identifier));
		}

		@Override
		public @Nullable Object value(int rowIndex, C identifier) {
			return columns.value(items.visible().get(rowIndex), identifier);
		}

		private List<@Nullable Object> values(Stream<Integer> rowIndexStream, C identifier) {
			return rowIndexStream.map(rowIndex -> value(rowIndex, identifier)).collect(toList());
		}

		private @Nullable Object valueAt(int rowIndex, int columnIndex) {
			return value(rowIndex, columns.identifier(columnIndex));
		}

		private C validateIdentifier(C identifier) {
			int modelIndex = columns.identifiers().indexOf(identifier);
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
					columnFilterModels.put(identifier, ConditionModel.builder()
									.valueClass(columnClass)
									.build());
				}
			}

			return columnFilterModels;
		}
	}

	private static final class DefaultVisiblePredicate<R, C>
					extends AbstractValue<Predicate<R>> implements VisiblePredicate<R> {

		private final TableColumns<R, C> tableColumns;
		private final TableConditionModel<C> filters;

		private @Nullable Predicate<R> predicate;

		private DefaultVisiblePredicate(TableColumns<R, C> columns, TableConditionModel<C> filters) {
			super(SET);
			this.tableColumns = columns;
			this.filters = filters;
			this.filters.changed().addListener(this::notifyListeners);
		}

		@Override
		public boolean test(R item) {
			if (!VisiblePredicate.super.test(item)) {
				return false;
			}

			return filters.get().entrySet().stream()
							.filter(entry -> entry.getValue().enabled().is())
							.allMatch(entry -> accepts(item, entry.getValue(), entry.getKey(), tableColumns));
		}

		@Override
		protected @Nullable Predicate<R> getValue() {
			return predicate;
		}

		@Override
		protected void setValue(@Nullable Predicate<R> predicate) {
			this.predicate = predicate;
		}

		private boolean accepts(R item, ConditionModel<?> condition, C identifier, TableColumns<R, C> columns) {
			if (condition.valueClass().equals(String.class)) {
				String string = columns.string(item, identifier);

				return ((ConditionModel<String>) condition).accepts(string.isEmpty() ? null : string);
			}

			return condition.accepts(columns.comparable(item, identifier));
		}
	}

	private static final class DefaultEditor<R, C> implements Editor<R, C> {

		@Override
		public boolean editable(R row, C identifier) {
			return false;
		}

		@Override
		public void set(@Nullable Object value, int rowIndex, R row, C identifier) {}
	}

	private static final class DefaultEditorFactory<R, C> implements Function<FilterTableModel<R, C>, Editor<R, C>> {

		@Override
		public Editor<R, C> apply(FilterTableModel<R, C> tableModel) {
			return new DefaultEditor<>();
		}
	}

	static final class DefaultColumnsStep implements Builder.ColumnsStep {

		@Override
		public <R, C> Builder<R, C> columns(TableColumns<R, C> columns) {
			return new DefaultBuilder<>(columns);
		}
	}

	static final class DefaultBuilder<R, C> implements Builder<R, C> {

		static final Builder.ColumnsStep COLUMNS = new DefaultColumnsStep();

		private final TableColumns<R, C> columns;

		private @Nullable Supplier<? extends Collection<R>> supplier;
		private Predicate<R> validator = new ValidPredicate<>();
		private Supplier<Map<C, ConditionModel<?>>> filters;
		private RefreshStrategy refreshStrategy = RefreshStrategy.CLEAR;
		private boolean async = FilterModel.ASYNC.getOrThrow();
		private Function<FilterTableModel<R, C>, Editor<R, C>> editorFactory = new DefaultEditorFactory<>();
		private @Nullable Predicate<R> visiblePredicate;

		private DefaultBuilder(TableColumns<R, C> columns) {
			if (requireNonNull(columns).identifiers().isEmpty()) {
				throw new IllegalArgumentException("No columns specified");
			}
			this.columns = validateIdentifiers(columns);
			this.filters = new DefaultColumnFilterFactory<>(columns);
		}

		@Override
		public Builder<R, C> filters(Supplier<Map<C, ConditionModel<?>>> filters) {
			this.filters = requireNonNull(filters);
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
		public Builder<R, C> async(boolean async) {
			this.async = async;
			return this;
		}

		@Override
		public Builder<R, C> editor(Function<FilterTableModel<R, C>, Editor<R, C>> editor) {
			this.editorFactory = requireNonNull(editor);
			return this;
		}

		@Override
		public Builder<R, C> visible(Predicate<R> predicate) {
			this.visiblePredicate = requireNonNull(predicate);
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

		private Refresher<R> createRefresher(Items<R> items) {
			return new DefaultRefreshWorker<>(supplier, items, async);
		}

		private static final class DefaultRefreshWorker<R> extends AbstractRefreshWorker<R> {

			private final Items<R> items;

			private DefaultRefreshWorker(@Nullable Supplier<? extends Collection<R>> supplier,
																	 Items<R> items, boolean async) {
				super((Supplier<Collection<R>>) supplier, async);
				this.items = items;
			}

			@Override
			protected void processResult(Collection<R> result) {
				items.set(result);
			}
		}

		private static final class ValidPredicate<R> implements Predicate<R> {

			@Override
			public boolean test(R r) {
				return true;
			}
		}
	}
}
