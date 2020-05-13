/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.ui;

import dev.codion.common.db.database.Databases;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.entity.Entity;
import dev.codion.swing.common.ui.textfield.IntegerField;
import dev.codion.swing.framework.model.SwingEntityComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityComboBoxTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("jminor.test.user", "scott:tiger"));
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

  @Test
  public void integerValueSelector() {
    final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    comboBoxModel.refresh();
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    final IntegerField empIdValue = comboBox.integerFieldSelector(TestDomain.EMP_ID);
    assertNull(empIdValue.getInteger());
    final Entity.Key jonesKey = comboBoxModel.getConnectionProvider().getEntities().key(TestDomain.T_EMP, 5);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    assertEquals(5, empIdValue.getInteger());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.getInteger());
    empIdValue.setInteger(10);
    assertEquals("ADAMS", comboBoxModel.getSelectedValue().getString(TestDomain.EMP_NAME));
    empIdValue.setInteger(null);
    assertNull(comboBoxModel.getSelectedValue());
  }
}