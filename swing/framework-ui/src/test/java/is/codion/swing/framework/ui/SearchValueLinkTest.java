/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.builder.EntityInputComponents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SearchValueLinkTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityEditModel model = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
  private final EntityInputComponents inputComponents = new EntityInputComponents(model.getEntityDefinition());

  @Test
  public void test() throws Exception {
    final EntitySearchModel searchModel = inputComponents.foreignKeySearchFieldBuilder(TestDomain.EMP_DEPARTMENT_FK,
            model.value(TestDomain.EMP_DEPARTMENT_FK),
            model.getForeignKeySearchModel(TestDomain.EMP_DEPARTMENT_FK))
            .build().getModel();
    assertEquals(0, searchModel.getSelectedEntities().size());
    Entity department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    model.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals(searchModel.getSelectedEntities().size(), 1);
    assertEquals(searchModel.getSelectedEntities().iterator().next(), department);
    department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    searchModel.setSelectedEntity(department);
    assertEquals(model.get(TestDomain.EMP_DEPARTMENT_FK), department);
  }
}