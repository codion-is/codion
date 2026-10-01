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
import is.codion.common.model.filter.FilterModel.IncludedItems.ItemsListener;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.utilities.Text;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static is.codion.common.model.condition.TableConditionModel.tableConditionModel;
import static is.codion.common.reactive.value.Value.Notify.SET;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class DefaultFilterTableModel<R, C> implements FilterTableModel<R, C> {

	/**
	 * A Comparator collating Strings according to the {@link Text#COLLATOR_LOCALE} locale.
	 */
	static final Comparator<String> LEXICAL_COMPARATOR = Text.collator();

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

	DefaultFilterTableModel(AbstractFilterTableModelBuilder<R, C, ?> builder,
													Function<IncludedItems<R>, MultiSelection<R>> selectionFactory, @Nullable ItemsListener listener) {
		this.columns = builder.columns;
		this.filters = createFilters(builder.columns, builder.filters);
		this.sort = new DefaultFilterTableSort<>(columns);
		Items.Builder<R> itemsBuilder = Items.builder()
						.selection(selectionFactory)
						.sort(sort)
						.validator(builder.validator)
						.included(new DefaultInclude<>(builder.columns, filters));
		if (builder.supplier != null) {
			itemsBuilder.items(builder.supplier);
		}
		if (builder.onRefreshException != null) {
			itemsBuilder.onRefreshException(builder.onRefreshException);
		}
		if (listener != null) {
			itemsBuilder.listener(listener);
		}
		this.items = itemsBuilder.build();
		this.items.included().predicate().set(builder.included);
		this.selection = (MultiSelection<R>) items.included().selection();
		builder.selectionListeners.forEach(selection.indexes()::addListener);
		builder.itemSelectedListeners.forEach(selection.item()::addConsumer);
		builder.itemsSelectedListeners.forEach(selection.items()::addConsumer);
		builder.indexSelectedListeners.forEach(selection.index()::addConsumer);
		builder.indexesSelectedListeners.forEach(selection.indexes()::addConsumer);
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
			return unmodifiableList(rowIndexStream.map(rowIndex -> value(rowIndex, identifier)).collect(toList()));
		}

		private C validateIdentifier(C identifier) {
			int modelIndex = columns.identifiers().indexOf(identifier);
			if (modelIndex == -1) {
				throw new IllegalArgumentException("Unknown column identifier: " + identifier);
			}

			return identifier;
		}
	}

	private static <C> TableConditionModel<C> createFilters(TableColumns<?, C> columns,
																													@Nullable Consumer<TableConditionModel.Builder<C>> configuration) {
		DefaultFiltersBuilder<C> builder = new DefaultFiltersBuilder<>(columns);
		if (configuration != null) {
			configuration.accept(builder);
		}

		return builder.build();
	}

	private static final class DefaultFiltersBuilder<C> implements TableConditionModel.Builder<C> {

		private final TableColumns<?, C> columns;
		private final Set<C> excluded = new HashSet<>();
		private final Map<C, Consumer<?>> configured = new HashMap<>();

		private DefaultFiltersBuilder(TableColumns<?, C> columns) {
			this.columns = columns;
		}

		@Override
		public TableConditionModel.Builder<C> exclude(C... identifiers) {
			excluded.addAll(asList(requireNonNull(identifiers)));
			return this;
		}

		@Override
		public <T> TableConditionModel.Builder<C> condition(C identifier, Consumer<ConditionModel.Builder<T>> condition) {
			configured.put(requireNonNull(identifier), requireNonNull(condition));
			return this;
		}

		@Override
		public TableConditionModel<C> build() {
			Map<C, ConditionModel.Builder<?>> defaults = new LinkedHashMap<>();
			for (C identifier : columns.identifiers()) {
				columns.filter(identifier).ifPresent(filter -> defaults.put(identifier, filter));
			}
			validate(defaults.keySet());
			Map<C, ConditionModel<?>> filters = new HashMap<>();
			defaults.forEach((identifier, filter) -> {
				if (!excluded.contains(identifier)) {
					filters.put(identifier, filter(identifier, filter));
				}
			});

			return tableConditionModel(filters);
		}

		private <T> ConditionModel<T> filter(C identifier, ConditionModel.Builder<T> builder) {
			Consumer<ConditionModel.Builder<T>> configuration = (Consumer<ConditionModel.Builder<T>>) configured.get(identifier);
			if (configuration != null) {
				configuration.accept(builder);
			}
			ConditionModel<T> filter = builder.build();
			Class<?> columnType = columns.type(identifier);
			if (!filter.type().equals(String.class) && !filter.type().equals(columnType)) {
				throw new IllegalArgumentException("Filter " + identifier + " is of type " + filter.type().getName() +
								", neither String nor the column type " + columnType.getName());
			}

			return filter;
		}

		private void validate(Set<C> filterable) {
			excluded.forEach(identifier -> validate(identifier, filterable));
			configured.keySet().forEach(identifier -> validate(identifier, filterable));
			for (C identifier : excluded) {
				if (configured.containsKey(identifier)) {
					throw new IllegalArgumentException("Filter " + identifier + " is both excluded and configured");
				}
			}
		}

		private static <C> void validate(C identifier, Set<C> filterable) {
			if (!filterable.contains(identifier)) {
				throw new IllegalArgumentException(identifier + " is not a filterable column");
			}
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
			if (condition.type().equals(String.class)) {
				String formatted = columns.formatted(item, identifier);

				return ((ConditionModel<String>) condition).accepts(formatted.isEmpty() ? null : formatted);
			}

			return condition.accepts(columns.comparable(item, identifier));
		}
	}

	static final class DefaultColumnsStep implements Builder.ColumnsStep {

		@Override
		public <R, C> Builder<R, C, ?> columns(TableColumns<R, C> columns) {
			return new DefaultBuilder<>(columns);
		}
	}

	static final class DefaultBuilder<R, C> extends AbstractFilterTableModelBuilder<R, C, DefaultBuilder<R, C>> {

		static final Builder.ColumnsStep COLUMNS = new DefaultColumnsStep();

		private DefaultBuilder(TableColumns<R, C> columns) {
			super(columns);
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
