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
  protected EmpDeptFXModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) {
    return new EmpDeptFXModel(connectionProvider);
  }

  @Override
  protected User getDefaultUser() {
    return User.parseUser("scott:tiger");
  }

  @Override
  protected void initializeEntityViews() {
    FXEntityModel departmentModel = getModel().getEntityModel(Department.TYPE);
    EntityView departmentView = new EntityView(departmentModel,
            new DepartmentEditView(departmentModel.getEditModel()),
            new EntityTableView(departmentModel.getTableModel()));

    FXEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);
    EntityView employeeView = new EntityView(employeeModel,
            new EmployeeEditView(employeeModel.getEditModel()),
            new EntityTableView(employeeModel.getTableModel()));

    departmentView.addDetailView(employeeView);

    departmentView.initializePanel();

    addEntityView(departmentView);
  }

  public static void main(final String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.empdept.domain.EmpDept");
    launch(args);
  }
}
