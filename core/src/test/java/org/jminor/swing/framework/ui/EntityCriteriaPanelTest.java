/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.EntityTableModel;
import org.jminor.swing.framework.model.DefaultEntityTableModel;

import org.junit.Test;

public class EntityCriteriaPanelTest {

  @Test
  public void test() {
    TestDomain.init();
    final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityCriteriaPanel(tableModel);
  }
}
