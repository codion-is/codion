/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityComboBoxModel;
import org.jminor.framework.client.model.EntityComboBoxModel;
import org.jminor.framework.db.local.LocalEntityConnectionTest;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityComboProviderTest {

  @Test
  public void test() throws Exception {
    final EntityComboBoxModel model = new DefaultEntityComboBoxModel(TestDomain.T_DEPARTMENT, LocalEntityConnectionTest.CONNECTION_PROVIDER);
    final EntityComboProvider provider = new EntityComboProvider(model, null);

    assertNull(provider.getValue());

    final Entity dept = LocalEntityConnectionTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedItem(dept);
    assertEquals(dept, provider.getValue());
    model.setSelectedItem(null);
    assertNull(provider.getValue());
  }
}