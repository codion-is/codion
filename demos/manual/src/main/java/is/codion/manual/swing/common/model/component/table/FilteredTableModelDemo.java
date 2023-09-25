package is.codion.manual.swing.common.model.component.table;

import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnFactory;
import is.codion.swing.common.model.component.table.FilteredTableModel.ColumnValueProvider;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel;
import is.codion.swing.common.model.component.table.FilteredTableSearchModel.RowColumn;
import is.codion.swing.common.model.component.table.FilteredTableSelectionModel;

import java.util.Collection;
import java.util.function.Supplier;

import static is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.TableRow.INTEGER_COLUMN_INDEX;
import static is.codion.manual.swing.common.model.component.table.FilteredTableModelDemo.TableRow.STRING_COLUMN_INDEX;
import static java.util.Arrays.asList;

public final class FilteredTableModelDemo {
  // tag::filteredTableModel[]
  // Define a class representing the table rows
  public static final class TableRow {
    
    public static final int STRING_COLUMN_INDEX = 0;
    public static final int INTEGER_COLUMN_INDEX = 1;

    private final String stringValue;

    private final Integer integerValue;

    TableRow(String stringValue, Integer integerValue) {
      this.stringValue = stringValue;
      this.integerValue = integerValue;
    }

    String stringValue() {
      return stringValue;
    }

    Integer integerValue() {
      return integerValue;
    }
  }

  public static FilteredTableModel<TableRow, Integer> createFilteredTableModel() {
    // Define a factory for the table columns
    ColumnFactory<Integer> columnFactory = () -> asList(
            FilteredTableColumn.builder(STRING_COLUMN_INDEX)
                    .headerValue("String")
                    .columnClass(String.class)
                    .build(),
            FilteredTableColumn.builder(INTEGER_COLUMN_INDEX)
                    .headerValue("Integer")
                    .columnClass(Integer.class)
                    .build());

    // Define a column value provider, providing the table column values
    ColumnValueProvider<TableRow, Integer> columnValueProvider = (row, columnIdentifier) -> {
      switch (columnIdentifier) {
        case STRING_COLUMN_INDEX:
          return row.stringValue();
        case INTEGER_COLUMN_INDEX:
          return row.integerValue();
        default:
          throw new IllegalArgumentException();
      }
    };

    // Define an item supplier responsible for supplying the table row items,
    // without one the table can be populated by adding items manually
    Supplier<Collection<TableRow>> itemSupplier = () -> asList(
            new TableRow("A string", 42),
            new TableRow("Another string", 43));

    // Create the table model
    FilteredTableModel<TableRow, Integer> tableModel =
            FilteredTableModel.builder(columnFactory, columnValueProvider)
                    .itemSupplier(itemSupplier)
                    // if true then the item supplier is called in a
                    // background thread when the model is refreshed
                    .asyncRefresh(false)
                    .build();

    // Populate the model
    tableModel.refresh();

    // Select the first row
    FilteredTableSelectionModel<TableRow> selectionModel = tableModel.selectionModel();
    selectionModel.setSelectedIndex(0);

    // With async refresh enabled
    // tableModel.refreshThen(items ->
    //        selectionModel.setSelectedIndex(0));

    // Search for the value "43" in the table
    FilteredTableSearchModel searchModel = tableModel.searchModel();
    searchModel.searchPredicate().set(value -> value.equals("43"));

    RowColumn searchResult = searchModel.currentResult();
    System.out.println(searchResult); // row: 1, column: 1

    return tableModel;
  }
  // end::filteredTableModel[]
}
