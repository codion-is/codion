/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Employee;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Employee.FIRSTNAME);

    createTextField(Employee.FIRSTNAME)
            .columns(16);
    createTextField(Employee.LASTNAME)
            .columns(16);
    createTemporalInputPanel(Employee.BIRTHDATE)
            .columns(16);
    createTextField(Employee.ADDRESS)
            .columns(16);
    createTextField(Employee.CITY)
            .columns(16);
    createTextField(Employee.STATE)
            .columns(16)
            .upperCase();
    createTextField(Employee.COUNTRY)
            .columns(16);
    createTextField(Employee.POSTALCODE)
            .columns(16);
    createTextField(Employee.PHONE)
            .columns(16);
    createTextField(Employee.FAX)
            .columns(16);
    createTextField(Employee.EMAIL)
            .columns(16);
    createForeignKeyComboBox(Employee.REPORTSTO_FK)
            .preferredHeight(getPreferredTextFieldHeight());
    createTemporalInputPanel(Employee.HIREDATE)
            .columns(16);
    createTextField(Employee.TITLE)
            .columns(16);

    setLayout(flexibleGridLayout(4, 4));
    addInputPanel(Employee.FIRSTNAME);
    addInputPanel(Employee.LASTNAME);
    addInputPanel(Employee.BIRTHDATE);
    addInputPanel(Employee.ADDRESS);
    addInputPanel(Employee.CITY);
    addInputPanel(Employee.STATE);
    addInputPanel(Employee.COUNTRY);
    addInputPanel(Employee.POSTALCODE);
    addInputPanel(Employee.PHONE);
    addInputPanel(Employee.FAX);
    addInputPanel(Employee.EMAIL);
    addInputPanel(Employee.REPORTSTO_FK);
    addInputPanel(Employee.HIREDATE);
    addInputPanel(Employee.TITLE);
  }
}