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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.empdept.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

// tag::applicationModel[]
public final class EmpDeptAppModel extends SwingEntityApplicationModel {

  public EmpDeptAppModel(EntityConnectionProvider connectionProvider) {
    super(connectionProvider);
    SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
    departmentModel.addDetailModel(new SwingEntityModel(new EmployeeEditModel(connectionProvider)));
    departmentModel.tableModel().refresh();
    addEntityModel(departmentModel);
  }
}
// end::applicationModel[]