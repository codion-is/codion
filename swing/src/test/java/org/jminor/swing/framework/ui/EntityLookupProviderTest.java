/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.DefaultEntityLookupModel;
import org.jminor.framework.model.EntityLookupModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityLookupProviderTest {

  @Test
  public void test() throws Exception {
    final EntityLookupModel model = new DefaultEntityLookupModel(TestDomain.T_DEPARTMENT,
            EntityConnectionProvidersTest.CONNECTION_PROVIDER, Entities.getSearchProperties(TestDomain.T_DEPARTMENT));
    final EntityLookupProvider provider = new EntityLookupProvider(model, null);

    assertNull(provider.getValue());

    final Entity dept = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedEntity(dept);
    assertEquals(dept, provider.getValue());
    model.setSelectedEntity(null);
    assertNull(provider.getValue());
  }
}
