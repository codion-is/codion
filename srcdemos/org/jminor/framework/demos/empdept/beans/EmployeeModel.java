/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.FilterCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.PropertyEvent;
import org.jminor.framework.domain.PropertyListener;
import org.jminor.framework.domain.Type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmployeeModel extends EntityModel {

  public EmployeeModel(final EntityDbProvider dbProvider) throws UserException {
    super(EmpDept.T_EMPLOYEE, dbProvider);
    getTableModel().setShowAllWhenNotFiltered(true);
    getTableModel().getPropertySummaryModel(EmpDept.EMPLOYEE_SALARY).setSummaryType(PropertySummaryModel.AVERAGE);
  }

  /** {@inheritDoc} */
  @Override
  public EntityComboBoxModel createEntityComboBoxModel(final Property.ForeignKeyProperty property) {
    if (property.propertyID.equals(EmpDept.EMPLOYEE_MGR_FK)) {
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

  /** {@inheritDoc} */
  @Override
  protected void bindEvents() {
    super.bindEvents();
    evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).refresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    getPropertyChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_FK).addListener(new PropertyListener() {
      @Override
      public void propertyChanged(final PropertyEvent e) {
        //only show managers in the same department as the active entity
        getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_FK).setFilterCriteria(new FilterCriteria() {
          public boolean include(final Object item) {
            return item instanceof String //the item representing null
                    || (Entity.isEqual(Type.ENTITY,
                    ((Entity)item).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_FK), e.getNewValue())
                    && !Entity.isEqual(Type.ENTITY, item, getActiveEntityCopy()));
          }
        });
      }
    });
  }
}