/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.framework.client.model.DefaultEntityComboBoxModel;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import static org.jminor.framework.demos.empdept.domain.EmpDept.*;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public final class EmployeeEditModel extends DefaultEntityEditModel {

  public EmployeeEditModel(final EntityDbProvider dbProvider) {
    super(T_EMPLOYEE, dbProvider);
    bindEvents();
  }

  /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
    if (foreignKeyProperty.is(EmpDept.EMPLOYEE_MGR_FK)) {
      final EntityComboBoxModel managerModel = new DefaultEntityComboBoxModel(T_EMPLOYEE, getDbProvider());
      managerModel.setNullValueString(EmpDept.getString(EmpDept.NONE));
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
    eventEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getEntityComboBoxModel(EMPLOYEE_MGR_FK).refresh();
        }
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    getValueChangeEvent(EMPLOYEE_DEPARTMENT_FK).addListener(new ValueChangeListener() {
      @Override
      public void valueChanged(final ValueChangeEvent event) {
        //only show managers in the same department as the active entity
        if (containsComboBoxModel(EMPLOYEE_MGR_FK)) {
          getEntityComboBoxModel(EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria<Entity>() {
            public boolean include(final Entity item) {
              return (Util.equal(item.getForeignKeyValue(EMPLOYEE_DEPARTMENT_FK), event.getNewValue())
                      && !Util.equal(item, getEntityCopy()));
            }
          });
        }
      }
    });
  }
}
