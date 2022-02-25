/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Employee;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(16);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Employee.FIRSTNAME);

    createTextField(Employee.FIRSTNAME);
    createTextField(Employee.LASTNAME);
    createTemporalInputPanel(Employee.BIRTHDATE);
    createTextField(Employee.ADDRESS);
    createTextField(Employee.CITY);
    createTextField(Employee.STATE)
            .upperCase(true);
    createTextField(Employee.COUNTRY);
    createTextField(Employee.POSTALCODE);
    createTextField(Employee.PHONE);
    createTextField(Employee.FAX);
    createTextField(Employee.EMAIL);
    createForeignKeyComboBox(Employee.REPORTSTO_FK);
    createTemporalInputPanel(Employee.HIREDATE);
    createTextField(Employee.TITLE);

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