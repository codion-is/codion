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
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

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
    EntitySearchModel model = EntitySearchModel.entitySearchModel(Department.TYPE, CONNECTION_PROVIDER);
    ComponentValue<Entity, EntitySearchField> value = EntitySearchField.builder(model)
            .buildComponentValue();

    assertNull(value.get());

    Entity dept = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");

    model.setSelectedEntity(dept);
    assertEquals(dept, value.get());
    model.setSelectedEntity(null);
    assertNull(value.get());
  }

  @Test
  void lookupDialog() {
    EntitySearchModel model = EntitySearchModel.entitySearchModel(Employee.TYPE, CONNECTION_PROVIDER);
    EntitySearchField searchField = EntitySearchField.builder(model).build();
    assertThrows(IllegalArgumentException.class, () -> EntitySearchField.lookupDialogBuilder(Department.TYPE, CONNECTION_PROVIDER)
            .owner(null)
            .title("title")
            .searchField(searchField));
  }
}
