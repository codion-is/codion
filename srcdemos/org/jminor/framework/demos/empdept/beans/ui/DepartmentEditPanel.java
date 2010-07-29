/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

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
    final JTextField txtDepartmentNumber = createTextField(DEPARTMENT_ID);
    final JTextField txtDepartmentName = (JTextField) UiUtil.makeUpperCase(createTextField(DEPARTMENT_NAME));
    final JTextField txtDepartmentLocation = (JTextField) UiUtil.makeUpperCase(createTextField(DEPARTMENT_LOCATION));

    setInitialFocusComponent(txtDepartmentNumber);
    txtDepartmentNumber.setColumns(10);

    //we don't allow editing of the department number since it's a primary key
    getEditModel().stateEntityNull().addStateListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
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
    add(createPropertyPanel(DEPARTMENT_ID, txtDepartmentNumber));
    add(createPropertyPanel(DEPARTMENT_NAME, txtDepartmentName));
    add(createPropertyPanel(DEPARTMENT_LOCATION, txtDepartmentLocation));
  }
}
