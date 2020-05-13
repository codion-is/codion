/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.Domain;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.framework.domain.property.ForeignKeyProperty;
import dev.codion.framework.model.EntityEditModel;
import dev.codion.framework.model.EntityLookupModel;
import dev.codion.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LookupValueLinkTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityEditModel model = new SwingEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER);

  @Test
  public void test() throws Exception {
    final ForeignKeyProperty fkProperty = DOMAIN.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK);
    final EntityLookupModel lookupModel = EntityInputComponents.createForeignKeyLookupField(fkProperty,
            model.value(TestDomain.EMP_DEPARTMENT_FK),
            model.getForeignKeyLookupModel(TestDomain.EMP_DEPARTMENT_FK)).getModel();
    assertEquals(0, lookupModel.getSelectedEntities().size());
    Entity department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    model.put(TestDomain.EMP_DEPARTMENT_FK, department);
    assertEquals(lookupModel.getSelectedEntities().size(), 1);
    assertEquals(lookupModel.getSelectedEntities().iterator().next(), department);
    department = model.getConnectionProvider().getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    lookupModel.setSelectedEntity(department);
    assertEquals(model.get(fkProperty.getPropertyId()), department);
  }
}