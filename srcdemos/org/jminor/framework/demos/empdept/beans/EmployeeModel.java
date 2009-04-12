/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.IFilterCriteria;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.PropertyListener;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.db.criteria.EntityCriteria;
import org.jminor.framework.db.criteria.PropertyCriteria;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EmployeeModel extends EntityModel {

  public EmployeeModel(final IEntityDbProvider dbProvider) throws UserException {
    super(EmpDept.getString(EmpDept.T_EMPLOYEE), dbProvider, EmpDept.T_EMPLOYEE);
    getTableModel().setFilterQueryByMaster(true);
    getTableModel().setShowAllWhenNotFiltered(true);
  }

  /** {@inheritDoc} */
  public EntityComboBoxModel createEntityComboBoxModel(final Property.EntityProperty property) {
    if (property.propertyID.equals(EmpDept.EMPLOYEE_MGR_REF)) {
      final EntityComboBoxModel managerModel = new EntityComboBoxModel(getDbConnectionProvider(), EmpDept.T_EMPLOYEE,
              false, EmpDept.getString(EmpDept.NONE), true);
      //Only show the president and managers
      managerModel.setEntityCriteria(new EntityCriteria(EmpDept.T_EMPLOYEE,
              new PropertyCriteria(EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_JOB),
                      SearchType.IN, "MANAGER", "PRESIDENT")));

      return managerModel;
    }

    return super.createEntityComboBoxModel(property);
  }

  /** {@inheritDoc} */
  protected void bindEvents() {
    super.bindEvents();
    evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_REF).refresh();
        }
        catch (UserException ex) {
          throw ex.getRuntimeException();
        }
      }
    });
    getPropertyChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_REF).addListener(new PropertyListener() {
      protected void propertyChanged(final PropertyChangeEvent e) {
        //only show managers in the same department as the active entity
        getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_REF).setFilterCriteria(new IFilterCriteria() {
          public boolean include(final Object item) {
            return item instanceof String //the item representing null
                    || (Entity.equal(Type.ENTITY,
                    ((Entity)item).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_REF), e.getNewValue())
                    && !Entity.equal(Type.ENTITY, item, getActiveEntityCopy()));
          }
        });
      }
    });
  }
}