/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntitySearchFieldTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void componentValue() throws Exception {
    EntitySearchModel searchModel = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
    ComponentValue<Entity, EntitySearchField> value = EntitySearchField.builder(searchModel)
            .buildValue();

    assertNull(value.get());

    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");

    searchModel.setSelectedEntity(sales);
    assertEquals(sales, value.get());
    searchModel.setSelectedEntity(null);
    assertNull(value.get());

    ComponentValue<List<Entity>, EntitySearchField> multiSelectionValue = value.component().multiSelectionValue();
    assertTrue(multiSelectionValue.get().isEmpty());

    ComponentValue<Entity, EntitySearchField> singleSelectionValue = value.component().singleSelectionValue();
    assertNull(singleSelectionValue.get());

    Entity research = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "RESEARCH");

    searchModel.setSelectedEntities(Arrays.asList(sales, research));

    assertTrue(multiSelectionValue.get().containsAll(Arrays.asList(sales, research)));
    assertEquals(singleSelectionValue.get(), sales);

    singleSelectionValue.set(null);

    assertTrue(searchModel.getSelectedEntities().isEmpty());
    assertNull(singleSelectionValue.get());
  }
}
