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
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.model.EmpDeptAppModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class SelectDepartment extends AbstractEntityUsageScenario<EmpDeptAppModel> {

  @Override
  protected void perform(EmpDeptAppModel application) {
    selectRandomRow(application.entityModel(Department.TYPE).tableModel());
  }

  @Override
  public int defaultWeight() {
    return 10;
  }
}
// end::loadTest[]