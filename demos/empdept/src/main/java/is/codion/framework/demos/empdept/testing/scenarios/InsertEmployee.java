/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.domain.entity.test.EntityTestUnit.createRandomEntity;
import static is.codion.swing.framework.tools.loadtest.EntityLoadTestModel.selectRandomRow;

// tag::loadTest[]
public final class InsertEmployee extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  @Override
  protected void perform(final EmpDeptAppPanel.EmpDeptApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityModel departmentModel = application.getEntityModel(Department.TYPE);
      selectRandomRow(departmentModel.getTableModel());
      final SwingEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);
      final Map<EntityType<?>, Entity> references = new HashMap<>();
      references.put(Department.TYPE, departmentModel.getTableModel().getSelectionModel().getSelectedItem());
      employeeModel.getEditModel().setEntity(createRandomEntity(application.getEntities(),
              Employee.TYPE, references));
      employeeModel.getEditModel().insert();
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 3;
  }
}
// end::loadTest[]