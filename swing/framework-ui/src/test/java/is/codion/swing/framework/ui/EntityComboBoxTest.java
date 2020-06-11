/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Databases;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.swing.common.ui.textfield.IntegerField;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * User: Björn Darri
 * Date: 17.4.2010
 * Time: 12:06:44
 */
public class EntityComboBoxTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
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
    final Key jonesKey = comboBoxModel.getConnectionProvider().getEntities().key(TestDomain.T_EMP, 3);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    final IntegerField empIdValue = comboBox.integerFieldSelector(TestDomain.EMP_ID);
    assertEquals(3, empIdValue.getInteger());
    final Key blakeKey = comboBoxModel.getConnectionProvider().getEntities().key(TestDomain.T_EMP, 5);
    comboBoxModel.setSelectedEntityByKey(blakeKey);
    assertEquals(5, empIdValue.getInteger());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.getInteger());
    empIdValue.setInteger(10);
    assertEquals("ADAMS", comboBoxModel.getSelectedValue().get(TestDomain.EMP_NAME));
    empIdValue.setInteger(null);
    assertNull(comboBoxModel.getSelectedValue());
  }
}