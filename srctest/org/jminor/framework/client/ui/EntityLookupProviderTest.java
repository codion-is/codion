/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityLookupModel;
import org.jminor.framework.client.model.EntityLookupModel;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

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
    final EntityLookupModel model = new DefaultEntityLookupModel(EmpDept.T_DEPARTMENT,
            LocalEntityConnectionTest.CONNECTION_PROVIDER, Entities.getSearchProperties(EmpDept.T_DEPARTMENT));
    final EntityLookupProvider provider = new EntityLookupProvider(model, null);

    assertNull(provider.getValue());

    final Entity dept = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");

    model.setSelectedEntity(dept);
    assertEquals(dept, provider.getValue());
    model.setSelectedEntity(null);
    assertNull(provider.getValue());
  }
}
