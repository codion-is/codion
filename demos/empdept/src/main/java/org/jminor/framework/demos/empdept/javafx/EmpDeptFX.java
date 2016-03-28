/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityModel;
import org.jminor.javafx.framework.ui.EntityApplicationView;
import org.jminor.javafx.framework.ui.EntityTableView;
import org.jminor.javafx.framework.ui.EntityView;

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
    return new User("scott", "tiger");
  }

  @Override
  protected void initializeEntitieViews() {
    final EntityModel departmentModel = getModel().getEntityModel(EmpDept.T_DEPARTMENT);
    final EntityView departmentView = new EntityView(departmentModel,
            new DepartmentEditView(departmentModel.getEditModel()),
            new EntityTableView(departmentModel.getTableModell()));
    departmentModel.getTableModell().refresh();

    final EntityModel employeeModel = departmentModel.getDetailModel(EmpDept.T_EMPLOYEE);
    final EntityView employeeView = new EntityView(employeeModel,
            new EmployeeEditView(employeeModel.getEditModel()),
            new EntityTableView(employeeModel.getTableModell()));
    employeeModel.getTableModell().refresh();

    departmentView.addDetailView(employeeView);

    departmentView.initializePanel();

    addEntityView(departmentView);
  }

  public static void main(final String[] args) {
    launch(args);
  }
}
