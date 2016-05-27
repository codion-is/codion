/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.EventInfoListener;
import org.jminor.common.EventListener;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entity;
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
      managerModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(T_EMPLOYEE, EMPLOYEE_JOB,
              SearchType.LIKE, Arrays.asList("MANAGER", "PRESIDENT")));

      return managerModel;
    }

    return super.createForeignKeyComboBoxModel(foreignKeyProperty);
  }

  //keep event bindings in one place
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated
    addEntitiesChangedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getForeignKeyComboBoxModel(EMPLOYEE_MGR_FK).refresh();
        }
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    addValueListener(EMPLOYEE_DEPARTMENT_FK, new EventInfoListener<ValueChange<String, ?>>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        //only show managers in the same department as the active entity
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getForeignKeyComboBoxModel(EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria<Entity>() {
            @Override
            public boolean include(final Entity item) {
              return Objects.equals(item.getForeignKey(EMPLOYEE_DEPARTMENT_FK), info.getNewValue())
                      && !Objects.equals(item, getEntityCopy());
            }
          });
        }
      }
    });
  }
}
