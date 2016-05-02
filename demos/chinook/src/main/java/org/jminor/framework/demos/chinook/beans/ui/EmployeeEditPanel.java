/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.common.ui.DateInputPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.chinook.domain.Chinook.*;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(EMPLOYEE_FIRSTNAME);
    final JTextField txtFirstName = createTextField(EMPLOYEE_FIRSTNAME);
    txtFirstName.setColumns(16);
    final JTextField txtLastName = createTextField(EMPLOYEE_LASTNAME);
    txtLastName.setColumns(16);
    final DateInputPanel birthdateInputPanel = createDateInputPanel(EMPLOYEE_BIRTHDATE);
    birthdateInputPanel.getInputField().setColumns(16);
    final JTextField txtAddress = createTextField(EMPLOYEE_ADDRESS);
    txtAddress.setColumns(16);
    final JTextField txtCity = createTextField(EMPLOYEE_CITY);
    txtCity.setColumns(16);
    final JTextField txtState = (JTextField) UiUtil.makeUpperCase(createTextField(EMPLOYEE_STATE));
    txtState.setColumns(16);
    final JTextField txtCountry = createTextField(EMPLOYEE_COUNTRY);
    txtCountry.setColumns(16);
    final JTextField txtPostalcode = createTextField(EMPLOYEE_POSTALCODE);
    txtPostalcode.setColumns(16);
    final JTextField txtPhone = createTextField(EMPLOYEE_PHONE);
    txtPhone.setColumns(16);
    final JTextField txtFax = createTextField(EMPLOYEE_FAX);
    txtFax.setColumns(16);
    final JTextField txtEmail = createTextField(EMPLOYEE_EMAIL);
    txtEmail.setColumns(16);
    createForeignKeyComboBox(EMPLOYEE_REPORTSTO_FK);
    final DateInputPanel hiredateInputPanel = createDateInputPanel(EMPLOYEE_HIREDATE);
    hiredateInputPanel.getInputField().setColumns(16);
    final JTextField txtTitle = createTextField(EMPLOYEE_TITLE);
    txtTitle.setColumns(16);

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