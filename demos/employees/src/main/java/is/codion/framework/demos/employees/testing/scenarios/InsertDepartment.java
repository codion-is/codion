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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;

// tag::loadTest[]
public final class InsertDepartment extends AbstractEntityUsageScenario<EmployeesAppModel> {

  @Override
  protected void perform(EmployeesAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Employees.Department.TYPE);
    departmentModel.editModel().set(createRandomEntity(application.entities(),
            Employees.Department.TYPE, null));
    departmentModel.editModel().insert();
  }
}
// end::loadTest[]