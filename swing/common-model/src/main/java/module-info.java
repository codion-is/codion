/**
 * Common Swing model classes, such as:<br>
 * <br>
 * {@link is.codion.swing.common.model.component.combobox.FilteredComboBoxModel}<br>
 * {@link is.codion.swing.common.model.component.combobox.ItemComboBoxModel}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableModel}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableColumn}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableColumnModel}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableSearchModel}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableSelectionModel}<br>
 * {@link is.codion.swing.common.model.component.table.FilteredTableSortModel}<br>
 * {@link is.codion.swing.common.model.worker.ProgressWorker}<br>
 */
module is.codion.swing.common.model {
  requires transitive java.desktop;
  requires transitive is.codion.common.model;
  requires transitive is.codion.common.i18n;

  exports is.codion.swing.common.model.component.button;
  exports is.codion.swing.common.model.component.combobox;
  exports is.codion.swing.common.model.component.table;
  exports is.codion.swing.common.model.component.text;
  exports is.codion.swing.common.model.worker;
}