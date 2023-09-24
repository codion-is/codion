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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Employee;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.SwingEntityTreeModel;

import static is.codion.swing.framework.model.SwingEntityTreeModel.swingEntityTreeModel;

public final class EmployeeTableModel extends SwingEntityTableModel {

  private final SwingEntityTreeModel treeModel;

  public EmployeeTableModel(EntityConnectionProvider connectionProvider) {
    super(Employee.TYPE, connectionProvider);
    this.treeModel = swingEntityTreeModel(this, Employee.REPORTSTO_FK);
  }

  public SwingEntityTreeModel treeModel() {
    return treeModel;
  }
}
