/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
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
    setInitialFocusAttribute(EMPLOYEE_FIRSTNAME);

    createTextField(EMPLOYEE_FIRSTNAME).setColumns(16);
    createTextField(EMPLOYEE_LASTNAME).setColumns(16);
    createTemporalInputPanel(EMPLOYEE_BIRTHDATE).getInputField().setColumns(16);
    createTextField(EMPLOYEE_ADDRESS).setColumns(16);
    createTextField(EMPLOYEE_CITY).setColumns(16);
    upperCase(createTextField(EMPLOYEE_STATE)).setColumns(16);
    createTextField(EMPLOYEE_COUNTRY).setColumns(16);
    createTextField(EMPLOYEE_POSTALCODE).setColumns(16);
    createTextField(EMPLOYEE_PHONE).setColumns(16);
    createTextField(EMPLOYEE_FAX).setColumns(16);
    createTextField(EMPLOYEE_EMAIL).setColumns(16);
    final EntityComboBox reportsToComboBox = createForeignKeyComboBox(EMPLOYEE_REPORTSTO_FK);
    setPreferredHeight(reportsToComboBox, getPreferredTextFieldHeight());
    createTemporalInputPanel(EMPLOYEE_HIREDATE).getInputField().setColumns(16);
    createTextField(EMPLOYEE_TITLE).setColumns(16);

    setLayout(flexibleGridLayout(4, 4));
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