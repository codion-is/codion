/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.table;

import is.codion.common.event.EventDataListener;
import is.codion.common.state.State;
import is.codion.common.value.Value;

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

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultFilteredTableSearchModel<C> implements FilteredTableSearchModel {

  private static final RowColumn NULL_COORDINATE = new DefaultRowColumn(-1, -1);

  private final FilteredTableModel<?, C> tableModel;
  private final State regularExpressionSearch = State.state();
  private final State caseSensitiveSearch = State.state();
  private final List<RowColumn> searchResults = new ArrayList<>();
  private final Value<String> searchStringValue = Value.value("", "");
  private final Value<Predicate<String>> searchPredicateValue = Value.value();
  private final Value<RowColumn> searchResultValue = Value.value(NULL_COORDINATE, NULL_COORDINATE);

  private int searchResultIndex = -1;

  DefaultFilteredTableSearchModel(FilteredTableModel<?, C> tableModel) {
    this.tableModel = requireNonNull(tableModel);
    bindEvents();
  }

  @Override
  public State regularExpressionSearchState() {
    return regularExpressionSearch;
  }

  @Override
  public State caseSensitiveSearchState() {
    return caseSensitiveSearch;
  }

  @Override
  public Value<String> searchStringValue() {
    return searchStringValue;
  }

  @Override
  public Value<Predicate<String>> searchPredicateValue() {
    return searchPredicateValue;
  }

  @Override
  public List<RowColumn> searchResults() {
    return unmodifiableList(new ArrayList<>(searchResults));
  }

  @Override
  public RowColumn currentResult() {
    return searchResultIndex == -1 ? NULL_COORDINATE : searchResultValue.get();
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
  public void addCurrentResultListener(EventDataListener<RowColumn> listener) {
    searchResultValue.addDataListener(listener);
  }

  private Optional<RowColumn> nextResult(boolean addToSelection) {
    if (searchResults.isEmpty()) {
      return emptyResult(addToSelection);
    }

    searchResultIndex = incrementSearchResultIndex();
    searchResultValue.set(searchResults.get(searchResultIndex));

    return selectResult(addToSelection);
  }

  private Optional<RowColumn> previousResult(boolean addToSelection) {
    if (searchResults.isEmpty()) {
      return emptyResult(addToSelection);
    }

    searchResultIndex = decrementSearchResultIndex();
    searchResultValue.set(searchResults.get(searchResultIndex));

    return selectResult(addToSelection);
  }

  private Optional<RowColumn> selectResult(boolean addToSelection) {
    if (addToSelection) {
      tableModel.selectionModel().addSelectedIndex(searchResultValue.get().row());
    }
    else {
      tableModel.selectionModel().setSelectedIndex(searchResultValue.get().row());
    }

    return searchResultValue.toOptional();
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
    if (searchPredicateValue.isNull() || tableModel.getRowCount() == 0 || tableModel.getColumnCount() == 0) {
      return;
    }
    Predicate<String> predicate = searchPredicateValue.get();
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
    searchResultValue.set(null);
  }

  private void bindEvents() {
    searchStringValue.addDataListener(searchString -> searchPredicateValue.set(createSearchPredicate(searchString)));
    searchPredicateValue.addListener(this::performSearch);
    regularExpressionSearch.addListener(() -> searchStringValue.set(null));
    caseSensitiveSearch.addListener(this::performSearch);
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
    if (regularExpressionSearch.get()) {
      try {
        return new RegexSearchCondition(searchText);
      }
      catch (PatternSyntaxException e) {
        return null;
      }
    }

    return new StringSearchCondition(searchText, caseSensitiveSearch);
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
