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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.swing.common.model.component.table.FilterTableModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static is.codion.common.reactive.value.Value.Notify.SET;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableSearchModel<C> implements FilterTableSearchModel {

	private final FilterTableModel<?, C> tableModel;
	private final FilterTableColumnModel<C> columnModel;
	private final DefaultResults results = new DefaultResults();
	private final State caseSensitive = State.builder()
					.listener(this::performSearch)
					.build();
	private final Value<Predicate<String>> predicate = Value.builder()
					.<Predicate<String>>nullable()
					.notify(SET)
					.listener(this::performSearch)
					.build();
	private final Value<String> searchString = Value.builder()
					.nonNull("")
					.consumer(searchText -> predicate.set(predicate(searchText)))
					.build();
	private final State regularExpression = State.builder()
					.listener(searchString::clear)
					.build();

	DefaultFilterTableSearchModel(FilterTableModel<?, C> tableModel, FilterTableColumnModel<C> columnModel) {
		this.tableModel = requireNonNull(tableModel);
		this.columnModel = requireNonNull(columnModel);
		bindEvents();
	}

	@Override
	public State regularExpression() {
		return regularExpression;
	}

	@Override
	public State caseSensitive() {
		return caseSensitive;
	}

	@Override
	public Value<String> searchString() {
		return searchString;
	}

	@Override
	public Value<Predicate<String>> predicate() {
		return predicate;
	}

	@Override
	public Results results() {
		return results;
	}

	private void performSearch() {
		results.clear();
		if (predicate.isNull() || tableModel.items().included().size() == 0 || tableModel.getColumnCount() == 0) {
			return;
		}
		Predicate<String> searchPredicate = predicate.getOrThrow();
		List<FilterTableColumn<C>> visibleColumns = columnModel.visible().columns();
		for (int row = 0; row < tableModel.items().included().size(); row++) {
			for (int columnIndex = 0; columnIndex < visibleColumns.size(); columnIndex++) {
				FilterTableColumn<C> column = visibleColumns.get(columnIndex);
				if (searchPredicate.test(tableModel.values().formatted(row, column.identifier()))) {
					results.add(new DefaultRowColumn(row, columnIndex));
				}
			}
		}
	}

	private void bindEvents() {
		columnModel.addColumnModelListener(new ClearSearchListener());
		tableModel.items().included().addListener(this::performSearch);
	}

	private @Nullable Predicate<String> predicate(String searchText) {
		if (searchText.isEmpty()) {
			return null;
		}
		if (regularExpression.is()) {
			try {
				return new RegexSearchCondition(searchText);
			}
			catch (PatternSyntaxException e) {
				return null;
			}
		}

		return new StringSearchCondition(searchText, caseSensitive);
	}

	private final class DefaultResults implements Results {

		private final List<RowColumn> searchResults = new ArrayList<>();
		private final Event<List<RowColumn>> resultsChanged = Event.event();
		private final DefaultCurrentResult current = new DefaultCurrentResult();
		private final DefaultSelectResult select = new DefaultSelectResult();

		private int searchResultIndex = -1;

		@Override
		public Optional<RowColumn> next() {
			if (searchResults.isEmpty()) {
				return Optional.empty();
			}

			searchResultIndex = incrementSearchResultIndex();
			current.result.set(searchResults.get(searchResultIndex));

			return current.result.optional();
		}

		@Override
		public Optional<RowColumn> previous() {
			if (searchResults.isEmpty()) {
				return Optional.empty();
			}

			searchResultIndex = decrementSearchResultIndex();
			current.result.set(searchResults.get(searchResultIndex));

			return current.result.optional();
		}

		@Override
		public CurrentResult current() {
			return current;
		}

		@Override
		public List<RowColumn> get() {
			return unmodifiableList(searchResults);
		}

		@Override
		public Observer<List<RowColumn>> observer() {
			return resultsChanged.observer();
		}

		@Override
		public SelectResult select() {
			return select;
		}

		private int incrementSearchResultIndex() {
			return searchResultIndex == -1 || searchResultIndex == searchResults.size() - 1 ? 0 : searchResultIndex + 1;
		}

		private int decrementSearchResultIndex() {
			return searchResultIndex == -1 || searchResultIndex == 0 ? searchResults.size() - 1 : searchResultIndex - 1;
		}

		private void clear() {
			searchResults.clear();
			searchResultIndex = -1;
			current.result.clear();
			resultsChanged.accept(emptyList());
		}

		private void add(DefaultRowColumn rowColumn) {
			searchResults.add(rowColumn);
			resultsChanged.accept(get());
		}

		private final class DefaultSelectResult implements SelectResult {

			@Override
			public Optional<RowColumn> next() {
				return select(true, false);
			}

			@Override
			public Optional<RowColumn> addNext() {
				return select(true, true);
			}

			@Override
			public Optional<RowColumn> previous() {
				return select(false, false);
			}

			@Override
			public Optional<RowColumn> addPrevious() {
				return select(false, true);
			}

			private Optional<RowColumn> select(boolean next, boolean add) {
				Optional<RowColumn> rowColumn = next ? DefaultResults.this.next() : DefaultResults.this.previous();
				if (rowColumn.isPresent()) {
					select(add, rowColumn.get());
				}
				else if (!add) {
					tableModel.selection().clearSelection();
				}

				return rowColumn;
			}

			private void select(boolean add, RowColumn rowColumn) {
				if (add) {
					tableModel.selection().indexes().add(rowColumn.row());
				}
				else {
					tableModel.selection().index().set(rowColumn.row());
				}
			}
		}

		private final class DefaultCurrentResult implements CurrentResult {

			private final Value<RowColumn> result = Value.nullable();

			@Override
			public boolean is(int row, int column) {
				RowColumn rowColumn = result.get();

				return rowColumn != null && rowColumn.is(row, column);
			}

			@Override
			public @Nullable RowColumn get() {
				return result.get();
			}

			@Override
			public Observer<RowColumn> observer() {
				return result.observer();
			}
		}
	}

	private final class ClearSearchListener implements TableColumnModelListener {

		@Override
		public void columnAdded(TableColumnModelEvent e) {
			searchString.clear();
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
			searchString.clear();
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			searchString.clear();
		}

		@Override
		public void columnMarginChanged(ChangeEvent e) {}

		@Override
		public void columnSelectionChanged(ListSelectionEvent e) {}
	}

	private static final class RegexSearchCondition implements Predicate<String> {

		private final Pattern pattern;

		private RegexSearchCondition(String patternString) {
			this.pattern = Pattern.compile(patternString);
		}

		@Override
		public boolean test(String item) {
			return item != null && pattern.matcher(item).find();
		}
	}

	static final class DefaultRowColumn implements RowColumn {

		private final int row;
		private final int column;

		DefaultRowColumn(int row, int column) {
			this.row = row;
			this.column = column;
		}

		@Override
		public int row() {
			return row;
		}

		@Override
		public int column() {
			return column;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			return obj instanceof DefaultFilterTableSearchModel.DefaultRowColumn &&
							((DefaultRowColumn) obj).row() == row() &&
							((DefaultRowColumn) obj).column() == column();
		}

		@Override
		public int hashCode() {
			return row + column;
		}

		@Override
		public String toString() {
			return "row: " + row + ", column: " + column;
		}
	}

	private static final class StringSearchCondition implements Predicate<String> {

		private final String searchText;
		private final State caseSensitiveSearch;

		private StringSearchCondition(String searchText, State caseSensitiveSearch) {
			this.searchText = searchText;
			this.caseSensitiveSearch = caseSensitiveSearch;
		}

		@Override
		public boolean test(String item) {
			return item != null && (caseSensitiveSearch.is() ? item : item.toLowerCase())
							.contains((caseSensitiveSearch.is() ? searchText : searchText.toLowerCase()));
		}
	}
}
