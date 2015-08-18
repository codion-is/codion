/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class DefaultForeignKeySearchModelTest {

  @Test
  public void getSearchEntitiesLookupModel() throws DatabaseException {
    TestDomain.init();
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER,
            Collections.singletonList(Entities.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
    final ForeignKeySearchModel searchModel = new DefaultForeignKeySearchModel(
            Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK), lookupModel);
    final Entity sales = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    lookupModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = searchModel.getSearchEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    final Entity accounting = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
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
    TestDomain.init();
    final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final ForeignKeySearchModel searchModel = new DefaultForeignKeySearchModel(
            Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK), comboBoxModel);
    final Entity sales = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
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
