/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import java.util.Arrays;
import java.util.Objects;

public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
    super(EmpDept.T_EMPLOYEE, connectionProvider);
    bindEvents();
  }

  /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.is(EmpDept.EMPLOYEE_MGR_FK)) {
      final EntityComboBoxModel managerModel = new SwingEntityComboBoxModel(EmpDept.T_EMPLOYEE, getConnectionProvider());
      managerModel.setNullValue(getEntities().createToStringEntity(EmpDept.T_EMPLOYEE, EmpDept.getString(EmpDept.NONE)));
      managerModel.setFilterSelectedItem(false);
      //Only show the president and managers
      managerModel.setSelectConditionProvider(() ->
              getConnectionProvider().getConditions().propertyCondition(EmpDept.T_EMPLOYEE,
                      EmpDept.EMPLOYEE_JOB, Condition.Type.LIKE, Arrays.asList("MANAGER", "PRESIDENT")));

      return managerModel;
    }

    return super.createForeignKeyComboBoxModel(foreignKeyProperty);
  }

  //keep event bindings in one place
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated
    addEntitiesChangedListener(() -> {
      if (containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)) {
        getForeignKeyComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    addValueListener(EmpDept.EMPLOYEE_DEPARTMENT_FK, valueChange -> {
      //only show managers in the same department as the active entity
      if (containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)) {
        getForeignKeyComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCondition(item ->
                Objects.equals(item.getForeignKey(EmpDept.EMPLOYEE_DEPARTMENT_FK), valueChange.getNewValue())
                        && !Objects.equals(item, getEntityCopy()));
      }
    });
  }
}
