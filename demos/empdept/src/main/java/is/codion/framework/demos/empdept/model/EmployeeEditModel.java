/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.ForeignKeyAttribute;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

// tag::constructor[]
public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
    super(Employee.TYPE, connectionProvider);
    bindEvents();
  }
  // end::constructor[]

  // tag::createForeignKeyComboBox[]
  /** Providing a custom ComboBoxModel for the manager attribute, which only shows managers and the president */
  @Override
  public SwingEntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKeyAttribute foreignKeyAttribute) {
    final SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyAttribute);
    if (foreignKeyAttribute.equals(Employee.MGR_FK)) {
      //Customize the null value so that it displays the chosen
      //text instead of the default '-' character
      comboBoxModel.setNullString("None");
      //we do not want filtering to remove a value that is selected
      //and thereby change the selection, see bindEvents() below
      comboBoxModel.setFilterSelectedItem(false);
      //Only select the president and managers from the database
      comboBoxModel.setSelectConditionProvider(() ->
              Conditions.condition(Employee.JOB).equalTo("MANAGER", "PRESIDENT"));
    }

    return comboBoxModel;
  }
  // end::createForeignKeyComboBox[]

  // tag::bindEvents[]
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is added, deleted or updated,
    //in case a new manager got hired, fired or promoted
    addEntitiesEditedListener(() -> getForeignKeyComboBoxModel(Employee.MGR_FK).refresh());
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed, either when an employee is
    //selected or the department combo box selection changes
    addValueListener(Employee.DEPARTMENT_FK, valueChange -> {
      //only show managers from the same department as the selected employee and hide the currently
      //selected employee to prevent an employee from being made her own manager
      getForeignKeyComboBoxModel(Employee.MGR_FK).setIncludeCondition(manager ->
              Objects.equals(manager.getForeignKey(Employee.DEPARTMENT_FK), valueChange.getValue())
                      && !Objects.equals(manager, getEntity()));
    });
  }
}
// end::bindEvents[]