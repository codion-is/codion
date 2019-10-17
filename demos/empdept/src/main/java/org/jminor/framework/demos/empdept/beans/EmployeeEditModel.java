/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import java.util.Objects;

import static java.util.Arrays.asList;

public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
    super(EmpDept.T_EMPLOYEE, connectionProvider);
    bindEvents();
  }

  /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    final EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyProperty);
    if (foreignKeyProperty.is(EmpDept.EMPLOYEE_MGR_FK)) {
      //Customize the null value so that it displays the chosen
      //text instead of the default '-' character
      comboBoxModel.setNullValue(getDomain().createToStringEntity(EmpDept.T_EMPLOYEE, "None"));
      //we do not want filtering to remove a value that is selected
      //and thereby change the selection, see bindEvents() below
      comboBoxModel.setFilterSelectedItem(false);
      //Only select the president and managers from the database
      comboBoxModel.setSelectConditionProvider(() ->
              EntityConditions.using(getConnectionProvider().getDomain()).propertyCondition(EmpDept.T_EMPLOYEE,
                      EmpDept.EMPLOYEE_JOB, ConditionType.LIKE, asList("MANAGER", "PRESIDENT")));
    }

    return comboBoxModel;
  }

  //keep event bindings in one place
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated,
    //in case a new manager got hired or promoted
    addEntitiesChangedListener(() -> getForeignKeyComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh());
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed, either when an employee is
    //selected or the department combo box selection changes
    addValueListener(EmpDept.EMPLOYEE_DEPARTMENT_FK, valueChange -> {
      //only show managers from the same department as the selected employee and hide the currently
      //selected employee to prevent an employee from being made her own manager
      getForeignKeyComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCondition(manager ->
              Objects.equals(manager.getForeignKey(EmpDept.EMPLOYEE_DEPARTMENT_FK), valueChange.getCurrentValue())
                      && !Objects.equals(manager, getEntity()));
    });
  }
}
