/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.ui.TestDomain;
import is.codion.swing.framework.ui.TestDomain.Department;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class EntitySearchFieldPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() throws DatabaseException {
    EntitySearchModel model = EntitySearchModel.builder(Department.TYPE, CONNECTION_PROVIDER).build();
    ComponentValue<Entity, EntitySearchFieldPanel> value = EntitySearchFieldPanel.builder(model, () -> null)
            .buildValue();
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(
            Department.NAME.equalTo("SALES"));
    model.entity().set(sales);
    assertEquals(sales, value.get());
    value.set(null);
    Entity entity = model.entity().get();
    assertNull(entity);
    value.set(sales);
    assertEquals(sales, model.entity().get());
  }
}
