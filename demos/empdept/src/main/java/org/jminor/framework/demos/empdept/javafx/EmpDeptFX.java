/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.javafx;

import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.javafx.framework.model.EntityEditModel;
import org.jminor.javafx.framework.model.EntityModel;
import org.jminor.javafx.framework.model.ObservableEntityList;
import org.jminor.javafx.framework.ui.EntityTableView;
import org.jminor.javafx.framework.ui.EntityView;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class EmpDeptFX extends Application {

  static {
    EmpDept.init();
  }

  public static void main(final String[] args) {
    launch(args);
  }

  @Override
  public void start(final Stage stage) throws Exception {
    final EntityConnectionProvider connectionProvider =
            EntityConnectionProviders.connectionProvider(new User("scott", "tiger"), "EmpDeptFX");

    final EntityModel departmentModel = new EntityModel(
            new EntityEditModel(EmpDept.T_DEPARTMENT, connectionProvider),
            new ObservableEntityList(EmpDept.T_DEPARTMENT, connectionProvider));
    final EntityView departmentView = new EntityView(departmentModel,
            new DepartmentEditView(departmentModel.getEditModel()),
            new EntityTableView(departmentModel.getEntityList()));
    departmentView.initializePanel();
    departmentModel.getEntityList().refresh();

    final EntityModel employeeModel = new EntityModel(
            new EntityEditModel(EmpDept.T_EMPLOYEE, connectionProvider),
            new ObservableEntityList(EmpDept.T_EMPLOYEE, connectionProvider));
    final EntityView employeeView = new EntityView(employeeModel,
            new EmployeeEditView(employeeModel.getEditModel()),
            new EntityTableView(employeeModel.getEntityList()));
    employeeView.initializePanel();
    employeeModel.getEntityList().refresh();

    final Scene scene = new Scene(departmentView);
    stage.setTitle("Departments");

    stage.setScene(scene);
    stage.show();
  }
}
