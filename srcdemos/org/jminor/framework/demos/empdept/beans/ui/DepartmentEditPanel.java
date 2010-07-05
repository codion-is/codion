/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    final JTextField txtDepartmentNumber = createTextField(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_ID);
    final JTextField txtDepartmentName = (JTextField) UiUtil.makeUpperCase(createTextField(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_NAME));
    final JTextField txtDepartmentLocation = (JTextField) UiUtil.makeUpperCase(createTextField(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_LOCATION));

    setInitialFocusComponent(txtDepartmentNumber);
    txtDepartmentNumber.setColumns(10);

    //we don't allow editing of the department number since it's a primary key
    getEditModel().stateEntityNull().eventStateChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getEditModel().isEntityNew()) {
          txtDepartmentNumber.setEnabled(true);
          setInitialFocusComponent(txtDepartmentNumber);
        }
        else {
          txtDepartmentNumber.setEnabled(false);
          setInitialFocusComponent(txtDepartmentName);
        }
      }
    });

    setLayout(new GridLayout(3,1,5,5));
    add(createPropertyPanel(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_ID, txtDepartmentNumber));
    add(createPropertyPanel(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_NAME, txtDepartmentName));
    add(createPropertyPanel(org.jminor.framework.demos.empdept.domain.EmpDept.DEPARTMENT_LOCATION, txtDepartmentLocation));
  }
}
