/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultForeignKeyConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void getSearchEntitiesLookupModel() throws DatabaseException {
    EntitySearchModel searchModel = new DefaultEntitySearchModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER,
            singletonList(TestDomain.DEPARTMENT_NAME));
    ForeignKeyConditionModel conditionModel = new DefaultForeignKeyConditionModel(TestDomain.EMP_DEPARTMENT_FK, searchModel);
    Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    searchModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = conditionModel.getEqualValues();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    List<Entity> salesAccounting = asList(sales, accounting);
    searchModel.setSelectedEntities(salesAccounting);
    assertTrue(conditionModel.getEqualValues().contains(sales));
    assertTrue(conditionModel.getEqualValues().contains(accounting));
    searchEntities = conditionModel.getEqualValues();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));

    conditionModel.setEqualValue(null);
    assertTrue(searchModel.getSelectedEntities().isEmpty());
    conditionModel.setEqualValue(sales);
    assertEquals(searchModel.getSelectedEntities().iterator().next(), sales);
    assertTrue(conditionModel.getEqualValues().contains(sales));

    searchModel.setSelectedEntity(null);

    searchEntities = conditionModel.getEqualValues();
    assertTrue(searchEntities.isEmpty());

    conditionModel.refresh();
  }
}
