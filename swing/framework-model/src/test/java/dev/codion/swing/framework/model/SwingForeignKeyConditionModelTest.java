/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.db.database.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.model.DefaultEntityTableConditionModel;
import org.jminor.framework.model.DefaultPropertyFilterModelProvider;
import org.jminor.framework.model.EntityTableConditionModel;
import org.jminor.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class SwingForeignKeyConditionModelTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(),
          new SwingPropertyConditionModelProvider());

  @Test
  public void refresh() {
    conditionModel.refresh();
    assertTrue(((SwingForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize() > 1);
    conditionModel.clear();
    assertEquals(0, ((SwingForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK))
            .getEntityComboBoxModel().getSize());
  }

  @Test
  public void getSearchEntitiesComboBoxModel() throws DatabaseException {
    final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final SwingForeignKeyConditionModel conditionModel = new SwingForeignKeyConditionModel(
            DOMAIN.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK), comboBoxModel);
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

    conditionModel.setUpperBound(null);
    assertNull(comboBoxModel.getSelectedItem());
    conditionModel.setUpperBound(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = conditionModel.getConditionEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
