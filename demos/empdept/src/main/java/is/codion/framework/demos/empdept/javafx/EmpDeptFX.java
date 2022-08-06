/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.javafx;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.javafx.framework.model.FXEntityModel;
import is.codion.javafx.framework.ui.EntityApplicationView;
import is.codion.javafx.framework.ui.EntityTableView;
import is.codion.javafx.framework.ui.EntityView;

public final class EmpDeptFX extends EntityApplicationView<EmpDeptFXModel> {

  public EmpDeptFX() {
    super("EmpDeptFX");
  }

  @Override
  protected EmpDeptFXModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new EmpDeptFXModel(connectionProvider);
  }

  @Override
  protected User defaultUser() {
    return User.parse("scott:tiger");
  }

  @Override
  protected void initializeEntityViews() {
    FXEntityModel departmentModel = model().entityModel(Department.TYPE);
    EntityView departmentView = new EntityView(departmentModel,
            new DepartmentEditView(departmentModel.editModel()),
            new EntityTableView(departmentModel.tableModel()));

    FXEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);
    EntityView employeeView = new EntityView(employeeModel,
            new EmployeeEditView(employeeModel.editModel()),
            new EntityTableView(employeeModel.tableModel()));

    departmentView.addDetailView(employeeView);

    departmentView.initializePanel();

    addEntityView(departmentView);
  }

  public static void main(String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    launch(args);
  }
}
