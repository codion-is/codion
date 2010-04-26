/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.valuemap.ValueMap;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmployeeModel extends EntityModel {

  public EmployeeModel(final EntityDbProvider dbProvider) {
    super(EmpDept.T_EMPLOYEE, dbProvider);
    getTableModel().setShowAllWhenNotFiltered(true);
    getTableModel().getPropertySummaryModel(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.AVERAGE);
  }

  @Override
  protected EntityEditModel initializeEditModel() {
    return new EntityEditModel(getEntityID(), getDbProvider()) {
      /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
      @Override
      public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty property) {
        if (property.is(EmpDept.EMPLOYEE_MGR_FK)) {
          final EntityComboBoxModel managerModel = new EntityComboBoxModel(EmpDept.T_EMPLOYEE,
                  getDbProvider(), false, EmpDept.getString(EmpDept.NONE), true);
          //Only show the president and managers
          managerModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB,
                  SearchType.LIKE, "MANAGER", "PRESIDENT"));

          return managerModel;
        }

        return super.createEntityComboBoxModel(property);
      }
    };
  }

  @Override
  protected void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is either added or updated
    eventEntitiesChanged().addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getEditModel().containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK))
          getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
      }
    });
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed
    getEditModel().getPropertyChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_FK).addListener(new Property.Listener() {
      @Override
      public void propertyChanged(final Property.Event e) {
        //only show managers in the same department as the active entity
        if (getEditModel().containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)) {
          getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria<Object>() {
            public boolean include(final Object item) {
              return item instanceof String //the item representing null
                      || (Entity.isEqual(ValueMap.class,
                      ((Entity)item).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), e.getNewValue())
                      && !Entity.isEqual(ValueMap.class, item, getEditModel().getEntityCopy()));
            }
          });
        }
      }
    });
  }
}