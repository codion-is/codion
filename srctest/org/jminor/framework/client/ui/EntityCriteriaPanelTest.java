/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.framework.client.model.DefaultEntityTableModel;
import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

public class EntityCriteriaPanelTest {

  @Test
  public void test() {
    TestDomain.init();
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityCriteriaPanel(tableModel);
  }
}
