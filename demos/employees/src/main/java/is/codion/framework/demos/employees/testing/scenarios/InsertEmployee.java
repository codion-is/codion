/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityPerformer;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUtil.createRandomEntity;

// tag::loadTest[]
public final class InsertEmployee extends AbstractEntityPerformer<EmployeesAppModel> {

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