/*
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static is.codion.framework.model.EntitySearchConditionModel.entitySearchConditionModel;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EntitySearchConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void searchModel() throws DatabaseException {
    EntitySearchModel searchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER)
            .columns(singletonList(Department.NAME))
            .build();
    EntitySearchConditionModel conditionModel = entitySearchConditionModel(Employee.DEPARTMENT_FK, searchModel);
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
    searchModel.entity().set(sales);
    Collection<Entity> searchEntities = conditionModel.getEqualValues();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
    List<Entity> salesAccounting = asList(sales, accounting);
    searchModel.entities().set(salesAccounting);
    assertTrue(conditionModel.getEqualValues().contains(sales));
    assertTrue(conditionModel.getEqualValues().contains(accounting));
    searchEntities = conditionModel.getEqualValues();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));

    conditionModel.setEqualValue(null);
    assertTrue(searchModel.entities().get().isEmpty());
    conditionModel.setEqualValue(sales);
    assertEquals(searchModel.entity().get(), sales);
    assertTrue(conditionModel.getEqualValues().contains(sales));

    searchModel.entities().set(null);

    searchEntities = conditionModel.getEqualValues();
    assertTrue(searchEntities.isEmpty());

    conditionModel.setEqualValue(sales);
    assertEquals("SALES", conditionModel.searchModel().searchString().get());
    sales.put(Department.NAME, "sales");
    conditionModel.searchModel().entity().set(sales);
    sales.put(Department.NAME, "SAles");
    conditionModel.setEqualValue(sales);
    assertEquals("SAles", conditionModel.searchModel().searchString().get());
  }
}
