/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Employee;
import static is.codion.swing.common.ui.Components.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;
import static is.codion.swing.common.ui.textfield.TextFields.getPreferredTextFieldHeight;
import static is.codion.swing.common.ui.textfield.TextFields.upperCase;

public class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(Employee.FIRSTNAME);

    createTextField(Employee.FIRSTNAME).setColumns(16);
    createTextField(Employee.LASTNAME).setColumns(16);
    createTemporalInputPanel(Employee.BIRTHDATE).getInputField().setColumns(16);
    createTextField(Employee.ADDRESS).setColumns(16);
    createTextField(Employee.CITY).setColumns(16);
    upperCase(createTextField(Employee.STATE)).setColumns(16);
    createTextField(Employee.COUNTRY).setColumns(16);
    createTextField(Employee.POSTALCODE).setColumns(16);
    createTextField(Employee.PHONE).setColumns(16);
    createTextField(Employee.FAX).setColumns(16);
    createTextField(Employee.EMAIL).setColumns(16);
    final EntityComboBox reportsToComboBox = createForeignKeyComboBox(Employee.REPORTSTO_FK);
    setPreferredHeight(reportsToComboBox, getPreferredTextFieldHeight());
    createTemporalInputPanel(Employee.HIREDATE).getInputField().setColumns(16);
    createTextField(Employee.TITLE).setColumns(16);

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