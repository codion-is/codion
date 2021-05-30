/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.framework.ui.TestDomain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class EntityInputComponentsTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void test() {
    final EntityDefinition definition = CONNECTION_PROVIDER.getEntities().getDefinition(TestDomain.T_DETAIL);
    final EntityInputComponents inputComponents = new EntityInputComponents(definition);
    final State enabledState = State.state();
    definition.getColumnProperties()
            .forEach(property -> inputComponents.createInputComponent(property.getAttribute(), enabledState));

    assertThrows(IllegalArgumentException.class, () -> inputComponents.createInputComponent(TestDomain.DETAIL_MASTER_FK));
  }
}
