/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(EmpDept.DEPARTMENT_ID);

    final JTextField departmentIdField = createTextField(EmpDept.DEPARTMENT_ID);
    departmentIdField.setColumns(10);
    UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_NAME));
    UiUtil.makeUpperCase(createTextField(EmpDept.DEPARTMENT_LOCATION));

    //we don't allow editing of the department number since it's a primary key
    getEditModel().getPrimaryKeyNullObserver().addListener(() -> {
      if (getEditModel().isEntityNew()) {
        departmentIdField.setEnabled(true);
        setInitialFocusProperty(EmpDept.DEPARTMENT_ID);
      }
      else {
        departmentIdField.setEnabled(false);
        setInitialFocusProperty(EmpDept.DEPARTMENT_NAME);
      }
    });

    setLayout(new GridLayout(3, 1, 5, 5));

    addPropertyPanel(EmpDept.DEPARTMENT_ID);
    addPropertyPanel(EmpDept.DEPARTMENT_NAME);
    addPropertyPanel(EmpDept.DEPARTMENT_LOCATION);
  }
}
