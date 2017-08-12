/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.testing.TestDomain;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class SwingForeignKeyConditionModelTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(),
          new SwingPropertyConditionModelProvider());

  @BeforeClass
  public static void setUp() {
    TestDomain.init();
  }

  @Test
  public void refresh() {
    conditionModel.refresh();
    assertTrue(((SwingForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize() > 1);
    conditionModel.clear();
    assertTrue(((SwingForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize() == 0);
  }

  @Test
  public void getSearchEntitiesComboBoxModel() throws DatabaseException {
    final EntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final SwingForeignKeyConditionModel conditionModel = new SwingForeignKeyConditionModel(
            Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK), comboBoxModel);
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.getSelectedValue());
    searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));

    conditionModel.setUpperBound((Object) null);
    assertNull(comboBoxModel.getSelectedItem());
    conditionModel.setUpperBound(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = conditionModel.getConditionEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
