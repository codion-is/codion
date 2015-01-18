/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class DefaultForeignKeySearchModelTest {

  @Test
  public void getSearchEntitiesLookupModel() throws DatabaseException {
    EmpDept.init();
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(EmpDept.T_DEPARTMENT, LocalEntityConnectionTest.CONNECTION_PROVIDER,
            Arrays.asList(Entities.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME)));
    final ForeignKeySearchModel searchModel = new DefaultForeignKeySearchModel(
            Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK), lookupModel);
    final Entity sales = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    lookupModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = searchModel.getSearchEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    final Entity accounting = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    lookupModel.setSelectedEntities(Arrays.asList(sales, accounting));
    searchEntities = searchModel.getSearchEntities();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));

    searchModel.setUpperBound((Object) null);
    assertTrue(lookupModel.getSelectedEntities().isEmpty());
    searchModel.setUpperBound(sales);
    assertEquals(lookupModel.getSelectedEntities().iterator().next(), sales);

    lookupModel.setSelectedEntity(null);

    searchEntities = searchModel.getSearchEntities();
    assertTrue(searchEntities.isEmpty());
  }

  @Test
  public void getSearchEntitiesComboBoxModel() throws DatabaseException {
    EmpDept.init();
    final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(EmpDept.T_DEPARTMENT, LocalEntityConnectionTest.CONNECTION_PROVIDER);
    final ForeignKeySearchModel searchModel = new DefaultForeignKeySearchModel(
            Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK), comboBoxModel);
    final Entity sales = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = searchModel.getSearchEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.getSelectedValue());
    searchEntities = searchModel.getSearchEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));

    searchModel.setUpperBound((Object) null);
    assertNull(comboBoxModel.getSelectedItem());
    searchModel.setUpperBound(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = searchModel.getSearchEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
