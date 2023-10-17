/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import javax.swing.JPanel;

import static is.codion.framework.demos.chinook.domain.Chinook.Employee;
import static is.codion.swing.common.ui.component.Components.flexibleGridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.layout.Layouts.flexibleGridLayout;

public final class EmployeeEditPanel extends EntityEditPanel {

  public EmployeeEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
    setDefaultTextFieldColumns(12);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(Employee.FIRSTNAME);

    createTextField(Employee.FIRSTNAME)
            .columns(6);
    createTextField(Employee.LASTNAME)
            .columns(6);
    createTemporalInputPanel(Employee.BIRTHDATE)
            .columns(6);
    createTemporalInputPanel(Employee.HIREDATE)
            .columns(6);
    createTextField(Employee.TITLE)
            .columns(8);
    createTextField(Employee.ADDRESS);
    createTextField(Employee.CITY)
            .columns(8);
    createTextField(Employee.POSTALCODE)
            .columns(4);
    createTextField(Employee.STATE)
            .columns(4)
            .upperCase(true);
    createTextField(Employee.COUNTRY)
            .columns(8);
    createTextField(Employee.PHONE);
    createTextField(Employee.FAX);
    createTextField(Employee.EMAIL);
    createForeignKeyComboBox(Employee.REPORTSTO_FK)
            .preferredWidth(120);

    JPanel firstLastNamePanel = gridLayoutPanel(1, 2)
            .add(createInputPanel(Employee.FIRSTNAME))
            .add(createInputPanel(Employee.LASTNAME))
            .build();

    JPanel birthHireDatePanel = gridLayoutPanel(1, 2)
            .add(createInputPanel(Employee.BIRTHDATE))
            .add(createInputPanel(Employee.HIREDATE))
            .build();

    JPanel cityPostalCodePanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Employee.CITY))
            .add(createInputPanel(Employee.POSTALCODE))
            .build();

    JPanel stateCountryPanel = flexibleGridLayoutPanel(1, 2)
            .add(createInputPanel(Employee.STATE))
            .add(createInputPanel(Employee.COUNTRY))
            .build();

    setLayout(flexibleGridLayout(4, 3));
    add(firstLastNamePanel);
    add(birthHireDatePanel);
    addInputPanel(Employee.TITLE);
    addInputPanel(Employee.ADDRESS);
    add(cityPostalCodePanel);
    add(stateCountryPanel);
    addInputPanel(Employee.PHONE);
    addInputPanel(Employee.FAX);
    addInputPanel(Employee.EMAIL);
    addInputPanel(Employee.REPORTSTO_FK);
  }
}