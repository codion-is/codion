/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.EntityUtil;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import java.util.Arrays;
import java.util.Objects;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
    super(T_EMPLOYEE, connectionProvider);
    bindEvents();
  }

  /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.is(EMPLOYEE_MGR_FK)) {
      final EntityComboBoxModel managerModel = new SwingEntityComboBoxModel(T_EMPLOYEE, getConnectionProvider());
      managerModel.setNullValue(EntityUtil.createToStringEntity(T_EMPLOYEE, getString(NONE)));
      managerModel.setFilterSelectedItem(false);
      //Only show the president and managers
      managerModel.setEntitySelectCondition(EntityConditions.selectCondition(T_EMPLOYEE, EMPLOYEE_JOB,
              Condition.Type.LIKE, Arrays.asList("MANAGER", "PRESIDENT")));

      return managerModel;
    }

    return super.createForeignKeyComboBoxModel(foreignKeyProperty);
  }

  //keep event bindings in one place
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated
    addEntitiesChangedListener(() -> {
      if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
        getForeignKeyComboBoxModel(EMPLOYEE_MGR_FK).refresh();
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    addValueListener(EMPLOYEE_DEPARTMENT_FK, info -> {
      //only show managers in the same department as the active entity
      if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
        getForeignKeyComboBoxModel(EMPLOYEE_MGR_FK).setFilterCondition(item -> Objects.equals(item.getForeignKey(EMPLOYEE_DEPARTMENT_FK), info.getNewValue())
                && !Objects.equals(item, getEntityCopy()));
      }
    });
  }
}
