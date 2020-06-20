/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultForeignKeyConditionModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void getSearchEntitiesLookupModel() throws DatabaseException {
    final Entities entities = CONNECTION_PROVIDER.getEntities();
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER,
            singletonList(CONNECTION_PROVIDER.getEntities().getDefinition(TestDomain.T_DEPARTMENT).getColumnProperty(TestDomain.DEPARTMENT_NAME)));
    final ForeignKeyConditionModel conditionModel = new DefaultForeignKeyConditionModel(
            entities.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK), lookupModel);
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    lookupModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = conditionModel.getConditionEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    final List<Entity> salesAccounting = asList(sales, accounting);
    lookupModel.setSelectedEntities(salesAccounting);
    assertTrue(conditionModel.getConditionEntities().contains(sales));
    assertTrue(conditionModel.getConditionEntities().contains(accounting));
    searchEntities = conditionModel.getConditionEntities();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));

    conditionModel.setUpperBound(null);
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
