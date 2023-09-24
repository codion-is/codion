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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
 */
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