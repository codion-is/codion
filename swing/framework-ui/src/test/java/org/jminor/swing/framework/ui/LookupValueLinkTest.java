/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.db.Databases;
import org.jminor.common.user.User;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LookupValueLinkTest {

  private static final Domain DOMAIN = new TestDomain();

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
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