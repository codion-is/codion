/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.javafx;

import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.demos.empdept.domain.EmpDept;
import dev.codion.framework.model.EntityModel;
import dev.codion.javafx.framework.model.FXEntityEditModel;
import dev.codion.javafx.framework.model.FXEntityListModel;
import dev.codion.javafx.framework.ui.EntityApplicationView;
import dev.codion.javafx.framework.ui.EntityTableView;
import dev.codion.javafx.framework.ui.EntityView;

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
    return Users.parseUser("scott:tiger");
  }

  @Override
  protected void initializeEntityViews() {
    final EntityModel departmentModel = getModel().getEntityModel(EmpDept.T_DEPARTMENT);
    final EntityView departmentView = new EntityView(departmentModel,
            new DepartmentEditView((FXEntityEditModel) departmentModel.getEditModel()),
            new EntityTableView((FXEntityListModel) departmentModel.getTableModel()));
    departmentModel.getTableModel().refresh();

    final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
    final EntityView employeeView = new EntityView(employeeModel,
            new EmployeeEditView((FXEntityEditModel) employeeModel.getEditModel()),
            new EntityTableView((FXEntityListModel) employeeModel.getTableModel()));
    employeeModel.getTableModel().refresh();

    departmentView.addDetailView(employeeView);

    departmentView.initializePanel();

    addEntityView(departmentView);
  }

  public static void main(final String[] args) {
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("dev.codion.framework.demos.empdept.domain.EmpDept");
    launch(args);
  }
}
