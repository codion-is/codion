/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityComboBoxTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  public void inputProvider() throws Exception {
    final SwingEntityComboBoxModel model = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    final EntityComboBox.ComponentValue provider = new EntityComboBox.ComponentValue(model, operations);

    assertNotNull(provider.get());

    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedItem(sales);
    assertEquals(sales, provider.get());
    model.setSelectedItem(null);
    assertNull(provider.get());
  }
}