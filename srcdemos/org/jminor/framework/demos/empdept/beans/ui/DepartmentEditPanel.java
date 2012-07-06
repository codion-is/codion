/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.ui;

import org.jminor.common.model.EventListener;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public class DepartmentEditPanel extends EntityEditPanel {

  public DepartmentEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    final JTextField txtDepartmentNumber = createTextField(DEPARTMENT_ID, LinkType.READ_WRITE);
    final JTextField txtDepartmentName = (JTextField) UiUtil.makeUpperCase(createTextField(DEPARTMENT_NAME));
    UiUtil.makeUpperCase(createTextField(DEPARTMENT_LOCATION));

    setInitialFocusComponent(txtDepartmentNumber);
    txtDepartmentNumber.setColumns(10);

    //we don't allow editing of the department number since it's a primary key
    getEditModel().getPrimaryKeyNullObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred(final ActionEvent e) {
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
    addPropertyPanel(DEPARTMENT_ID);
    addPropertyPanel(DEPARTMENT_NAME);
    addPropertyPanel(DEPARTMENT_LOCATION);
  }
}
