/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityModel;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;
import static is.codion.swing.framework.model.tools.loadtest.EntityLoadTestUtil.selectRandomRow;

// tag::loadTest[]
public final class InsertEmployee implements Performer<EmployeesAppModel> {

  @Override
  public void perform(EmployeesAppModel application) throws Exception {
    SwingEntityModel departmentModel = application.entityModel(Department.TYPE);
    selectRandomRow(departmentModel.tableModel());
    SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    Map<ForeignKey, Entity> foreignKeyEntities = new HashMap<>();
    foreignKeyEntities.put(Employee.DEPARTMENT_FK, departmentModel.tableModel().selectionModel().getSelectedItem());
    employeeModel.editModel().set(createRandomEntity(application.entities(), Employee.TYPE, foreignKeyEntities));
    employeeModel.editModel().insert();
  }
}
// end::loadTest[]