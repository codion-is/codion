/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.EntityLookupModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityLookupProviderTest {

  private static final Entities ENTITIES = new TestDomain();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(ENTITIES, new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()), Databases.getInstance());

  @Test
  public void test() throws Exception {
    final EntityLookupModel model = new DefaultEntityLookupModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final EntityLookupProvider provider = new EntityLookupProvider(model, null);

    assertNull(provider.getValue());

    final Entity dept = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedEntity(dept);
    assertEquals(dept, provider.getValue());
    model.setSelectedEntity(null);
    assertNull(provider.getValue());
  }
}
