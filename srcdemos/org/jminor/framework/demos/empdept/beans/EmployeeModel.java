/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.valuemap.ValueChangeEvent;
import org.jminor.common.model.valuemap.ValueChangeListener;
import org.jminor.framework.client.model.DefaultEntityComboBoxModel;
import org.jminor.framework.client.model.DefaultEntityEditModel;
import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmployeeModel extends DefaultEntityModel {

  public EmployeeModel(final EntityDbProvider dbProvider) {
    super(new EmployeeEditModel(dbProvider));
    getTableModel().setQueryCriteriaRequired(false);
    getTableModel().getPropertySummaryModel(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.AVERAGE);
  }

  static class EmployeeEditModel extends DefaultEntityEditModel {

    EmployeeEditModel(final EntityDbProvider dbProvider) {
      super(EmpDept.T_EMPLOYEE, dbProvider);
    }

    /** Providing a custom ComboBoxModel for the manager property, which only shows managers and the president */
    @Override
    public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty foreignKeyProperty) {
      if (foreignKeyProperty.is(EmpDept.EMPLOYEE_MGR_FK)) {
        final EntityComboBoxModel managerModel = new DefaultEntityComboBoxModel(EmpDept.T_EMPLOYEE, getDbProvider());
        managerModel.setNullValueString(EmpDept.getString(EmpDept.NONE));
        //Only show the president and managers
        managerModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB,
                SearchType.LIKE, "MANAGER", "PRESIDENT"));

        return managerModel;
      }

      return super.createEntityComboBoxModel(foreignKeyProperty);
    }

    @Override
    protected void bindEvents() {
      //Refresh the manager ComboBoxModel when an employee is either added or updated
      eventEntitiesChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)) {
            getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
          }
        }
      });
      //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
      //this filtering happens each time the department value is changed
      getValueChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_FK).addListener(new ValueChangeListener() {
        @Override
        public void valueChanged(final ValueChangeEvent event) {
          //only show managers in the same department as the active entity
          if (containsComboBoxModel(EmpDept.EMPLOYEE_MGR_FK)) {
            getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria<Object>() {
              public boolean include(final Object item) {
                return item instanceof String //the item representing null
                        || (Entity.isEqual(((Entity) item).getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), event.getNewValue())
                        && !Entity.isEqual(item, getEntityCopy()));
              }
            });
          }
        }
      });
    }
  }
}