/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.client.model.exception.ValidationException;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmployeeModel extends EntityModel {

  public EmployeeModel(final EntityDbProvider dbProvider) {
    super(EmpDept.T_EMPLOYEE, dbProvider);
    getTableModel().setShowAllWhenNotFiltered(true);
    getTableModel().getPropertySummaryModel(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.AVERAGE);
  }

  @Override
  protected EntityEditModel intializeEditModel() {
    return new EntityEditModel(getEntityID(), getDbProvider(), evtEntitiesChanged) {
      @Override
      public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty property) {
        if (property.getPropertyID().equals(EmpDept.EMPLOYEE_MGR_FK)) {
          final EntityComboBoxModel managerModel = new EntityComboBoxModel(EmpDept.T_EMPLOYEE,
                  getDbProvider(), false, EmpDept.getString(EmpDept.NONE), true);
          //Only show the president and managers
          managerModel.setEntityCriteria(new EntityCriteria(EmpDept.T_EMPLOYEE,
                  new PropertyCriteria(EntityRepository.getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB),
                          SearchType.IN, "MANAGER", "PRESIDENT")));

          return managerModel;
        }

        return super.createEntityComboBoxModel(property);
      }

      @Override
      public void validate(final Property property, final Object value) throws ValidationException {
        if (property.getPropertyID().equals(EmpDept.EMPLOYEE_SALARY)) {
          final Double salary = (Double) value;
          if (salary != null && (salary < 1000 || salary > 10000))
            throw new ValidationException(property, value, EmpDept.getString(EmpDept.EMPLOYEE_SALARY_DESCRIPTION));
        }
        super.validate(property, value);
      }
    };
  }

  /** {@inheritDoc} */
  @Override
  protected void bindEvents() {
    super.bindEvents();
    evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        getEditModel().getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
      }
    });
    getEditModel().getPropertyChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_FK).addListener(new Property.Listener() {
      @Override
      public void propertyChanged(final Property.Event e) {
        //only show managers in the same department as the active entity
        getEditModel().initializeEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria() {
          public boolean include(final Object item) {
            return item instanceof String //the item representing null
                    || (Entity.isEqual(Type.ENTITY,
                    ((Entity)item).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), e.getNewValue())
                    && !Entity.isEqual(Type.ENTITY, item, getEditModel().getEntityCopy()));
          }
        });
      }
    });
  }
}