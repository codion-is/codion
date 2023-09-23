/**
 * Shared model classes, such as:<br>
 * <br>
 * {@link is.codion.common.model.UserPreferences}<br>
 * {@link is.codion.common.model.CancelException}<br>
 * {@link is.codion.common.model.FilteredModel}<br>
 * {@link is.codion.common.model.table.ColumnConditionModel}<br>
 * {@link is.codion.common.model.table.ColumnSummaryModel}<br>
 * {@link is.codion.common.model.table.TableSelectionModel}<br>
 * {@link is.codion.common.model.table.TableConditionModel}<br>
 * {@link is.codion.common.model.table.TableSummaryModel}<br>
 */
module is.codion.common.model {
  requires java.prefs;
  requires transitive is.codion.common.core;

  exports is.codion.common.model;
  exports is.codion.common.model.table;
}