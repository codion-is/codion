/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChange;
import org.jminor.framework.client.model.DefaultEntityComboBoxModel;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import static org.jminor.framework.demos.empdept.domain.EmpDept.*;

public final class EmployeeEditModel extends DefaultEntityEditModel {

  public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
    super(T_EMPLOYEE, connectionProvider);
    bindEvents();
  }

  /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.is(EMPLOYEE_MGR_FK)) {
      final EntityComboBoxModel managerModel = new DefaultEntityComboBoxModel(T_EMPLOYEE, getConnectionProvider());
      managerModel.setNullValueString(getString(NONE));
      managerModel.setFilterSelectedItem(false);
      //Only show the president and managers
      managerModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(T_EMPLOYEE, EMPLOYEE_JOB,
              SearchType.LIKE, "MANAGER", "PRESIDENT"));

      return managerModel;
    }

    return super.createEntityComboBoxModel(foreignKeyProperty);
  }

  //keep event bindings in one place
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated
    addEntitiesChangedListener(new EventListener() {
      @Override
      public void eventOccurred() {
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getEntityComboBoxModel(EMPLOYEE_MGR_FK).refresh();
        }
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    addValueListener(EMPLOYEE_DEPARTMENT_FK, new EventInfoListener<ValueChange>() {
      @Override
      public void eventOccurred(final ValueChange info) {
        //only show managers in the same department as the active entity
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getEntityComboBoxModel(EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria<Entity>() {
            @Override
            public boolean include(final Entity item) {
              return (Util.equal(item.getForeignKeyValue(EMPLOYEE_DEPARTMENT_FK), info.getNewValue())
                      && !Util.equal(item, getEntityCopy()));
            }
          });
        }
      }
    });
  }
}
