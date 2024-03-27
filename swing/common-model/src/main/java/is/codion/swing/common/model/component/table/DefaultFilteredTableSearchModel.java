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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.state.State;
import is.codion.common.value.Value;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilteredTableSearchModel<C> implements FilteredTableSearchModel {

	private static final RowColumn NULL_COORDINATE = new DefaultRowColumn(-1, -1);

	private final FilteredTableModel<?, C> tableModel;
	private final State regularExpression = State.state();
	private final State caseSensitive = State.state();
	private final List<RowColumn> searchResults = new ArrayList<>();
	private final Value<String> searchString = Value.value("", "");
	private final Value<Predicate<String>> searchPredicate = Value.value();
	private final Value<RowColumn> searchResult = Value.value(NULL_COORDINATE, NULL_COORDINATE);

	private int searchResultIndex = -1;

	DefaultFilteredTableSearchModel(FilteredTableModel<?, C> tableModel) {
		this.tableModel = requireNonNull(tableModel);
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
	public Value<Predicate<String>> searchPredicate() {
		return searchPredicate;
	}

	@Override
	public List<RowColumn> searchResults() {
		return unmodifiableList(new ArrayList<>(searchResults));
	}

	@Override
	public RowColumn currentResult() {
		return searchResultIndex == -1 ? NULL_COORDINATE : searchResult.get();
	}

	@Override
	public Optional<RowColumn> nextResult() {
		return nextResult(false);
	}

	@Override
	public Optional<RowColumn> selectNextResult() {
		return nextResult(true);
	}

	@Override
	public Optional<RowColumn> previousResult() {
		return previousResult(false);
	}

	@Override
	public Optional<RowColumn> selectPreviousResult() {
		return previousResult(true);
	}

	@Override
	public void addCurrentResultListener(Consumer<RowColumn> listener) {
		searchResult.addDataListener(listener);
	}

	@Override
	public void removeCurrentResultListener(Consumer<RowColumn> listener) {
		searchResult.removeDataListener(listener);
	}

	private Optional<RowColumn> nextResult(boolean addToSelection) {
		if (searchResults.isEmpty()) {
			return emptyResult(addToSelection);
		}

		searchResultIndex = incrementSearchResultIndex();
		searchResult.set(searchResults.get(searchResultIndex));

		return selectResult(addToSelection);
	}

	private Optional<RowColumn> previousResult(boolean addToSelection) {
		if (searchResults.isEmpty()) {
			return emptyResult(addToSelection);
		}

		searchResultIndex = decrementSearchResultIndex();
		searchResult.set(searchResults.get(searchResultIndex));

		return selectResult(addToSelection);
	}

	private Optional<RowColumn> selectResult(boolean addToSelection) {
		if (addToSelection) {
			tableModel.selectionModel().addSelectedIndex(searchResult.get().row());
		}
		else {
			tableModel.selectionModel().setSelectedIndex(searchResult.get().row());
		}

		return searchResult.optional();
	}

	private Optional<RowColumn> emptyResult(boolean addToSelection) {
		if (!addToSelection) {
			tableModel.selectionModel().clearSelection();
		}

		return Optional.empty();
	}

	private int incrementSearchResultIndex() {
		return searchResultIndex == -1 || searchResultIndex == searchResults.size() - 1 ? 0 : searchResultIndex + 1;
	}

	private int decrementSearchResultIndex() {
		return searchResultIndex == -1 || searchResultIndex == 0 ? searchResults.size() - 1 : searchResultIndex - 1;
	}

	private void performSearch() {
		clearSearchResults();
		if (searchPredicate.isNull() || tableModel.getRowCount() == 0 || tableModel.getColumnCount() == 0) {
			return;
		}
		Predicate<String> predicate = searchPredicate.get();
		for (int row = 0; row < tableModel.getRowCount(); row++) {
			for (int column = 0; column < tableModel.getColumnCount(); column++) {
				if (predicate.test(tableModel.getStringAt(row, tableModel.columnModel().getColumn(column).getIdentifier()))) {
					searchResults.add(new DefaultRowColumn(row, column));
				}
			}
		}
	}

	private void clearSearchResults() {
		searchResults.clear();
		searchResultIndex = -1;
		searchResult.set(null);
	}

	private void bindEvents() {
		searchString.addDataListener(searchText -> searchPredicate.set(createSearchPredicate(searchText)));
		searchPredicate.addListener(this::performSearch);
		regularExpression.addListener(() -> searchString.set(null));
		caseSensitive.addListener(this::performSearch);
		tableModel.columnModel().addColumnModelListener(new ClearSearchListener());
		tableModel.addDataChangedListener(() -> {
			clearSearchResults();
			performSearch();
		});
	}

	private Predicate<String> createSearchPredicate(String searchText) {
		if (searchText.isEmpty()) {
			return null;
		}
		if (regularExpression.get()) {
			try {
				return new RegexSearchCondition(searchText);
			}
			catch (PatternSyntaxException e) {
				return null;
			}
		}

		return new StringSearchCondition(searchText, caseSensitive);
	}

	private final class ClearSearchListener implements TableColumnModelListener {

		@Override
		public void columnAdded(TableColumnModelEvent e) {
			clearSearchResults();
		}

		@Override
		public void columnRemoved(TableColumnModelEvent e) {
			clearSearchResults();
		}

		@Override
		public void columnMoved(TableColumnModelEvent e) {
			clearSearchResults();
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

			return obj instanceof DefaultFilteredTableSearchModel.DefaultRowColumn &&
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
			return item != null && (caseSensitiveSearch.get() ? item : item.toLowerCase())
							.contains((caseSensitiveSearch.get() ? searchText : searchText.toLowerCase()));
		}
	}
}
