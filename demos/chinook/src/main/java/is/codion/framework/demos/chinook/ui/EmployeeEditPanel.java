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

    textField(Employee.FIRSTNAME)
            .columns(16)
            .build();
    textField(Employee.LASTNAME)
            .columns(16)
            .build();
    temporalInputPanel(Employee.BIRTHDATE)
            .columns(16)
            .build();
    textField(Employee.ADDRESS)
            .columns(16)
            .build();
    textField(Employee.CITY)
            .columns(16)
            .build();
    textField(Employee.STATE)
            .columns(16)
            .upperCase()
            .build();
    textField(Employee.COUNTRY)
            .columns(16)
            .build();
    textField(Employee.POSTALCODE)
            .columns(16)
            .build();
    textField(Employee.PHONE)
            .columns(16)
            .build();
    textField(Employee.FAX)
            .columns(16)
            .build();
    textField(Employee.EMAIL)
            .columns(16)
            .build();
    foreignKeyComboBox(Employee.REPORTSTO_FK)
            .preferredHeight(getPreferredTextFieldHeight())
            .build();
    temporalInputPanel(Employee.HIREDATE)
            .columns(16)
            .build();
    textField(Employee.TITLE)
            .columns(16)
            .build();

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