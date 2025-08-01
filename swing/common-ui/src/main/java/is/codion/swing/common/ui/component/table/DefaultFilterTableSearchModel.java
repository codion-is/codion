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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.table;

import is.codion.common.event.Event;
import is.codion.common.observer.Observable;
import is.codion.common.observer.Observer;
import is.codion.common.state.State;
import is.codion.common.value.Value;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilterTableSearchModel<C> implements FilterTableSearchModel {

	private static final RowColumn NULL_COORDINATE = new DefaultRowColumn(-1, -1);

	private final FilterTableModel<?, C> tableModel;
	private final FilterTableColumnModel<C> columnModel;
	private final DefaultResults results = new DefaultResults();
	private final State caseSensitive = State.builder()
					.listener(this::performSearch)
					.build();
	private final Value<Predicate<String>> predicate = Value.builder()
					.<Predicate<String>>nullable()
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
		if (predicate.isNull() || tableModel.items().visible().count() == 0 || tableModel.getColumnCount() == 0) {
			return;
		}
		Predicate<String> searchPredicate = predicate.getOrThrow();
		List<FilterTableColumn<C>> visibleColumns = columnModel.visible().columns();
		for (int row = 0; row < tableModel.items().visible().count(); row++) {
			for (int columnIndex = 0; columnIndex < visibleColumns.size(); columnIndex++) {
				FilterTableColumn<C> column = visibleColumns.get(columnIndex);
				if (searchPredicate.test(tableModel.values().string(row, column.identifier()))) {
					results.add(new DefaultRowColumn(row, columnIndex));
				}
			}
		}
	}

	private void bindEvents() {
		columnModel.addColumnModelListener(new ClearSearchListener());
		tableModel.items().visible().addListener(this::performSearch);
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
		private final Value<RowColumn> searchResult = Value.nonNull(NULL_COORDINATE);

		private int searchResultIndex = -1;

		@Override
		public Optional<RowColumn> next() {
			return next(false);
		}

		@Override
		public Optional<RowColumn> selectNext() {
			return next(true);
		}

		@Override
		public Optional<RowColumn> previous() {
			return previous(false);
		}

		@Override
		public Optional<RowColumn> selectPrevious() {
			return previous(true);
		}

		@Override
		public Observable<RowColumn> current() {
			return searchResult.observable();
		}

		@Override
		public List<RowColumn> get() {
			return unmodifiableList(searchResults);
		}

		@Override
		public Observer<List<RowColumn>> observer() {
			return resultsChanged.observer();
		}

		private Optional<RowColumn> next(boolean addToSelection) {
			if (searchResults.isEmpty()) {
				return empty(addToSelection);
			}

			searchResultIndex = incrementSearchResultIndex();
			searchResult.set(searchResults.get(searchResultIndex));

			return select(addToSelection);
		}

		private Optional<RowColumn> previous(boolean addToSelection) {
			if (searchResults.isEmpty()) {
				return empty(addToSelection);
			}

			searchResultIndex = decrementSearchResultIndex();
			searchResult.set(searchResults.get(searchResultIndex));

			return select(addToSelection);
		}

		private Optional<RowColumn> select(boolean addToSelection) {
			if (addToSelection) {
				tableModel.selection().indexes().add(searchResult.getOrThrow().row());
			}
			else {
				tableModel.selection().index().set(searchResult.getOrThrow().row());
			}

			return searchResult.optional();
		}

		private Optional<RowColumn> empty(boolean addToSelection) {
			if (!addToSelection) {
				tableModel.selection().clearSelection();
			}

			return Optional.empty();
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
			searchResult.clear();
			resultsChanged.accept(emptyList());
		}

		private void add(DefaultRowColumn rowColumn) {
			searchResults.add(rowColumn);
			resultsChanged.accept(get());
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
