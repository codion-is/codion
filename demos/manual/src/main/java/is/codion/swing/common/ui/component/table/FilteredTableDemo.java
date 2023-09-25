package is.codion.swing.common.ui.component.table;

import is.codion.swing.common.model.component.table.FilteredTableModel;
import is.codion.swing.common.model.component.table.FilteredTableModelDemo.TableRow;
import is.codion.swing.common.ui.control.Control;

import javax.swing.JTable;

import static is.codion.swing.common.model.component.table.FilteredTableModelDemo.createFilteredTableModel;

final class FilteredTableDemo {

  static void demo() {
    // tag::filteredTable[]
    // See FilteredTableModel example
    FilteredTableModel<TableRow, Integer> tableModel = createFilteredTableModel();

    FilteredTable<TableRow, Integer> filteredTable = FilteredTable.builder(tableModel)
            .doubleClickAction(Control.control(() ->
                    tableModel.selectionModel().selectedItem()
                            .ifPresent(System.out::println)))
            .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
            .build();
    // end::filteredTable[]
  }
}
