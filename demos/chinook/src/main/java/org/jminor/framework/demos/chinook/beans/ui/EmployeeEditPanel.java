/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(EMPLOYEE_FIRSTNAME);

    createTextField(EMPLOYEE_FIRSTNAME).setColumns(16);
    createTextField(EMPLOYEE_LASTNAME).setColumns(16);
    createDateInputPanel(EMPLOYEE_BIRTHDATE).getInputField().setColumns(16);
    createTextField(EMPLOYEE_ADDRESS).setColumns(16);
    createTextField(EMPLOYEE_CITY).setColumns(16);
    UiUtil.makeUpperCase(createTextField(EMPLOYEE_STATE)).setColumns(16);
    createTextField(EMPLOYEE_COUNTRY).setColumns(16);
    createTextField(EMPLOYEE_POSTALCODE).setColumns(16);
    createTextField(EMPLOYEE_PHONE).setColumns(16);
    createTextField(EMPLOYEE_FAX).setColumns(16);
    createTextField(EMPLOYEE_EMAIL).setColumns(16);
    createForeignKeyComboBox(EMPLOYEE_REPORTSTO_FK);
    createDateInputPanel(EMPLOYEE_HIREDATE).getInputField().setColumns(16);
    createTextField(EMPLOYEE_TITLE).setColumns(16);

    setLayout(new FlexibleGridLayout(4, 4, 5, 5));
    addPropertyPanel(EMPLOYEE_FIRSTNAME);
    addPropertyPanel(EMPLOYEE_LASTNAME);
    addPropertyPanel(EMPLOYEE_BIRTHDATE);
    addPropertyPanel(EMPLOYEE_ADDRESS);
    addPropertyPanel(EMPLOYEE_CITY);
    addPropertyPanel(EMPLOYEE_STATE);
    addPropertyPanel(EMPLOYEE_COUNTRY);
    addPropertyPanel(EMPLOYEE_POSTALCODE);
    addPropertyPanel(EMPLOYEE_PHONE);
    addPropertyPanel(EMPLOYEE_FAX);
    addPropertyPanel(EMPLOYEE_EMAIL);
    addPropertyPanel(EMPLOYEE_REPORTSTO_FK);
    addPropertyPanel(EMPLOYEE_HIREDATE);
    addPropertyPanel(EMPLOYEE_TITLE);
  }
}