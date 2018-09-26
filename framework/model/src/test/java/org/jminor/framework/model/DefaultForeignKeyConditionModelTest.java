/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultForeignKeyConditionModelTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new TestDomain(), new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  @Test
  public void getSearchEntitiesLookupModel() throws DatabaseException {
    final Entities domain = CONNECTION_PROVIDER.getDomain();
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER,
            Collections.singletonList(CONNECTION_PROVIDER.getDomain().getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
    final ForeignKeyConditionModel conditionModel = new DefaultForeignKeyConditionModel(
            CONNECTION_PROVIDER.getConditions(),
            domain.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK), lookupModel);
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    lookupModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertEquals("deptno = ?", conditionModel.getCondition().getWhereClause());
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final List<Entity> salesAccounting = Arrays.asList(sales, accounting);
    lookupModel.setSelectedEntities(salesAccounting);
    assertTrue(conditionModel.getConditionEntities().contains(sales));
    assertTrue(conditionModel.getConditionEntities().contains(accounting));
    searchEntities = conditionModel.getConditionEntities();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));
    assertEquals("(deptno in (?, ?))", conditionModel.getCondition().getWhereClause());

    assertEquals("dept_fkLIKEdeptno:30deptno:10null", conditionModel.toString());

    conditionModel.setUpperBound((Object) null);
    assertTrue(lookupModel.getSelectedEntities().isEmpty());
    conditionModel.setUpperBound(sales);
    assertEquals(lookupModel.getSelectedEntities().iterator().next(), sales);
    assertTrue(conditionModel.getConditionEntities().contains(sales));

    lookupModel.setSelectedEntity(null);

    searchEntities = conditionModel.getConditionEntities();
    assertTrue(searchEntities.isEmpty());

    conditionModel.refresh();
    conditionModel.clear();
  }
}
