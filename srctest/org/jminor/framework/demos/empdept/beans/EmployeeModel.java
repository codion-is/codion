/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans;

import org.jminor.common.model.IFilterCriteria;
import org.jminor.common.model.PropertyChangeEvent;
import org.jminor.common.model.PropertyListener;
import org.jminor.common.model.UserException;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.demos.empdept.beans.combo.ManagerComboBoxModel;
import org.jminor.framework.demos.empdept.model.EmpDept;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityRepository;
import org.jminor.framework.model.EntityUtil;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.ComboBoxModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class EmployeeModel extends EntityModel {

  public EmployeeModel(final IEntityDbProvider dbProvider) throws UserException {
    super(EmpDept.getString(EmpDept.T_EMPLOYEE), dbProvider, EmpDept.T_EMPLOYEE);
    getTableModel().setFilterQueryByMaster(true);
    getTableModel().setShowAllWhenNotFiltered(true);
  }

  /** {@inheritDoc} */
  protected HashMap<Property, ComboBoxModel> initializeEntityComboBoxModels() {
    final HashMap<Property, ComboBoxModel> ret = new HashMap<Property, ComboBoxModel>();
    ret.put(EntityRepository.get().getProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_MGR_REF),
            new ManagerComboBoxModel(getDbConnectionProvider()));

    return ret;
  }

  /** {@inheritDoc} */
  protected List<String> getPropertyNotificationOrder(final Collection<Property> properties) {
    final List<String> ret = super.getPropertyNotificationOrder(properties);
    ret.remove(EmpDept.EMPLOYEE_DEPARTMENT_REF);
    ret.remove(EmpDept.EMPLOYEE_MGR_REF);
    ret.add(0, EmpDept.EMPLOYEE_MGR_REF);
    ret.add(0, EmpDept.EMPLOYEE_DEPARTMENT_REF);//notify first, since it filters the manager comboboxmodel

    return ret;
  }

  /** {@inheritDoc} */
  protected void bindEvents() {
    super.bindEvents();
    evtEntitiesChanged.addListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_REF).refresh();
        }
        catch (UserException e1) {
          throw e1.getRuntimeException();
        }
      }
    });
    getPropertyChangeEvent(EmpDept.EMPLOYEE_DEPARTMENT_REF).addListener(new PropertyListener() {
      protected void propertyChanged(final PropertyChangeEvent e) {
        getEntityComboBoxModel(EmpDept.EMPLOYEE_MGR_REF).setFilterCriteria(new IFilterCriteria() {
          public boolean include(final Object item) {
            return item instanceof String //the item representing null
                    || (EntityUtil.equal(Type.ENTITY,
                    ((Entity)item).getEntityValue(EmpDept.EMPLOYEE_DEPARTMENT_REF), e.getNewValue())
                    && !EntityUtil.equal(Type.ENTITY, item, getActiveEntityCopy()));
          }
        });
      }
    });
  }
}