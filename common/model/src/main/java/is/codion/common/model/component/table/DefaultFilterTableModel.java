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
package is.codion.common.model.component.table;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.model.filter.FilterModel;
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.value.AbstractValue;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.common.reactive.value.Value.Notify.SET;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableModel<R, C> implements FilterTableModel<R, C> {

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
	private final MultiSelection<R> selection;
	private final DefaultFilterTableSort<R, C> sort;
	private final DefaultColumnValues columnValues = new DefaultColumnValues();

	private DefaultFilterTableModel(DefaultBuilder<R, C> builder) {
		this.columns = builder.columns;
		this.filters = tableConditionModel(builder.filters);
		this.sort = new DefaultFilterTableSort<>(columns);
		Function<Items<R>, Refresher<R>> refresherFactory = builder.refresherFactory != null
						? builder.refresherFactory
						: modelItems -> FilterModel.refresher(builder.supplier, modelItems::set, builder.onRefreshException);
		Function<IncludedItems<R>, MultiSelection<R>> selectionFactory = builder.selectionFactory != null
						? builder.selectionFactory
						: MultiSelection::multiSelection;
		Items.Builder<R> itemsBuilder = Items.<R>builder()
						.refresher(refresherFactory)
						.selection(selectionFactory)
						.sort(sort)
						.validator(builder.validator)
						.include(new DefaultInclude<>(builder.columns, filters));
		builder.itemsListeners.forEach(itemsBuilder::listener);
		this.items = itemsBuilder.build();
		this.items.included().predicate().set(builder.included);
		this.selection = (MultiSelection<R>) items.included().selection();
		builder.selectionListeners.forEach(selection.indexes()::addListener);
		builder.itemSelectedListeners.forEach(selection.item()::addConsumer);
		builder.itemsSelectedListeners.forEach(selection.items()::addConsumer);
		builder.indexSelectedListeners.forEach(selection.index()::addConsumer);
		builder.indexesSelectedListeners.forEach(selection.indexes()::addConsumer);
		if (builder.refresh) {
			items.refresh();
		}
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
	public MultiSelection<R> selection() {
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
	public TableColumns<R, C> columns() {
		return columns;
	}

	@Override
	public Export<C> export() {
		return new DefaultExport();
	}

	private final class DefaultColumnValues implements ColumnValues<C> {

		@Override
		public <T> List<T> get(C identifier) {
			return (List<T>) values(IntStream.range(0, items.included().size()).boxed(), validateIdentifier(identifier));
		}

		@Override
		public <T> List<T> selected(C identifier) {
			return (List<T>) values(selection().indexes().get().stream(), validateIdentifier(identifier));
		}

		@Override
		public String formatted(int rowIndex, C identifier) {
			return columns.formatted(items.included().get(rowIndex), requireNonNull(identifier));
		}

		@Override
		public @Nullable Object value(int rowIndex, C identifier) {
			return columns.value(items.included().get(rowIndex), identifier);
		}

		private List<@Nullable Object> values(Stream<Integer> rowIndexStream, C identifier) {
			return rowIndexStream.map(rowIndex -> value(rowIndex, identifier)).collect(toList());
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

	private static final class DefaultInclude<R, C>
					extends AbstractValue<Predicate<R>> implements IncludePredicate<R> {

		private final TableColumns<R, C> tableColumns;
		private final TableConditionModel<C> filters;

		private @Nullable Predicate<R> predicate;

		private DefaultInclude(TableColumns<R, C> columns, TableConditionModel<C> filters) {
			super(SET);
			this.tableColumns = columns;
			this.filters = filters;
			this.filters.changed().addListener(this::notifyObserver);
		}

		@Override
		public boolean test(R item) {
			if (!IncludePredicate.super.test(item)) {
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
				String formatted = columns.formatted(item, identifier);

				return ((ConditionModel<String>) condition).accepts(formatted.isEmpty() ? null : formatted);
			}

			return condition.accepts(columns.comparable(item, identifier));
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

		private static final ValidPredicate<Object> DEFAULT_VALID_PREDICATE = new ValidPredicate<>();

		private final TableColumns<R, C> columns;
		private final List<Runnable> selectionListeners = new ArrayList<>();
		private final List<Consumer<R>> itemSelectedListeners = new ArrayList<>();
		private final List<Consumer<List<R>>> itemsSelectedListeners = new ArrayList<>();
		private final List<Consumer<Integer>> indexSelectedListeners = new ArrayList<>();
		private final List<Consumer<List<Integer>>> indexesSelectedListeners = new ArrayList<>();
		private final List<ItemsListener> itemsListeners = new ArrayList<>();

		private @Nullable Supplier<Collection<R>> supplier;
		private Predicate<R> validator = (Predicate<R>) DEFAULT_VALID_PREDICATE;
		private Supplier<Map<C, ConditionModel<?>>> filters;
		private @Nullable Consumer<Exception> onRefreshException;
		private @Nullable Predicate<R> included;
		private boolean refresh = false;
		private @Nullable Function<Items<R>, Refresher<R>> refresherFactory;
		private @Nullable Function<IncludedItems<R>, MultiSelection<R>> selectionFactory;

		private DefaultBuilder(TableColumns<R, C> columns) {
			if (requireNonNull(columns).identifiers().isEmpty()) {
				throw new IllegalArgumentException("TableColumns does not specify any column identifiers");
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
		public Builder<R, C> items(Supplier<Collection<R>> items) {
			this.supplier = requireNonNull(items);
			return this;
		}

		@Override
		public Builder<R, C> validator(Predicate<R> validator) {
			this.validator = requireNonNull(validator);
			return this;
		}

		@Override
		public Builder<R, C> onRefreshException(Consumer<Exception> onRefreshException) {
			this.onRefreshException = requireNonNull(onRefreshException);
			return this;
		}

		@Override
		public Builder<R, C> included(Predicate<R> included) {
			this.included = requireNonNull(included);
			return this;
		}

		@Override
		public Builder<R, C> refresh(boolean refresh) {
			this.refresh = refresh;
			return this;
		}

		@Override
		public Builder<R, C> onSelectionChanged(Runnable listener) {
			selectionListeners.add(requireNonNull(listener));
			return this;
		}

		@Override
		public Builder<R, C> onItemSelected(Consumer<R> item) {
			itemSelectedListeners.add(requireNonNull(item));
			return this;
		}

		@Override
		public Builder<R, C> onItemsSelected(Consumer<List<R>> items) {
			itemsSelectedListeners.add(requireNonNull(items));
			return this;
		}

		@Override
		public Builder<R, C> onIndexSelected(Consumer<Integer> index) {
			indexSelectedListeners.add(requireNonNull(index));
			return this;
		}

		@Override
		public Builder<R, C> onIndexesSelected(Consumer<List<Integer>> indexes) {
			indexesSelectedListeners.add(requireNonNull(indexes));
			return this;
		}

		@Override
		public Builder<R, C> selection(Function<IncludedItems<R>, MultiSelection<R>> selection) {
			this.selectionFactory = requireNonNull(selection);
			return this;
		}

		@Override
		public Builder<R, C> refresher(Function<Items<R>, Refresher<R>> refresher) {
			this.refresherFactory = requireNonNull(refresher);
			return this;
		}

		@Override
		public Builder<R, C> listener(ItemsListener itemsListener) {
			itemsListeners.add(requireNonNull(itemsListener));
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

	private final class DefaultExport implements Export<C> {

		private List<C> exportColumns = columns.identifiers();
		private char delimiter = '\t';
		private boolean header = true;
		private boolean selected = false;
		private @Nullable String newlineReplacement = " ";

		@Override
		public Export<C> columns(List<C> columns) {
			this.exportColumns = requireNonNull(columns);
			return this;
		}

		@Override
		public Export<C> delimiter(char delimiter) {
			this.delimiter = delimiter;
			return this;
		}

		@Override
		public Export<C> header(boolean header) {
			this.header = header;
			return this;
		}

		@Override
		public Export<C> selected(boolean selected) {
			this.selected = selected;
			return this;
		}

		@Override
		public Export<C> replaceNewline(@Nullable String replacement) {
			this.newlineReplacement = replacement;
			return this;
		}

		@Override
		public String get() {
			List<Integer> rows = selected ?
							selection().indexes().get() :
							IntStream.range(0, items().included().size())
											.boxed()
											.collect(toList());

			List<List<String>> lines = new ArrayList<>();
			if (header) {
				lines.add(exportColumns.stream()
								.map(columns::caption)
								.collect(toList()));
			}
			lines.addAll(rows.stream()
							.map(row -> stringValues(row, exportColumns))
							.collect(toList()));

			return lines.stream()
							.map(line -> join(String.valueOf(delimiter), line))
							.collect(joining("\n"));
		}

		private List<String> stringValues(int row, List<C> columns) {
			return columns.stream()
							.map(column -> columnValues.formatted(row, column))
							.map(String::trim)
							.map(this::replaceNewlines)
							.collect(toList());
		}

		private String replaceNewlines(String string) {
			if (newlineReplacement != null) {
				return string.replace("\r\n", newlineReplacement).replace("\n", newlineReplacement).replace("\r", newlineReplacement);
			}

			return string;
		}
	}
}
