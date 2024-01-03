/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public final class EntityComboBoxPanelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void test() throws DatabaseException {
    EntityComboBoxModel model = new EntityComboBoxModel(TestDomain.Department.TYPE, CONNECTION_PROVIDER);
    model.refresh();
    ComponentValue<Entity, EntityComboBoxPanel> value = EntityComboBoxPanel.builder(model, () -> null)
            .buildValue();
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(
            TestDomain.Department.NAME.equalTo("SALES"));
    model.setSelectedItem(sales);
    assertEquals(sales, value.get());
    value.set(null);
    Entity entity = model.selectedValue();
    assertNull(entity);
    value.set(sales);
    assertEquals(sales, model.selectedValue());
  }
}
