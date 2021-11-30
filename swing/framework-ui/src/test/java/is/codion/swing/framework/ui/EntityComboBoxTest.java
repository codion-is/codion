/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.ComponentValues;
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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @Test
  void inputProvider() throws Exception {
    final SwingEntityComboBoxModel model = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    model.refresh();
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    model.setSelectedItem(operations);
    final ComponentValue<Entity, EntityComboBox> value = ComponentValues.comboBox(new EntityComboBox(model));

    assertNotNull(value.get());

    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(
            TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedItem(sales);
    assertEquals(sales, value.get());
    model.setSelectedItem(null);
    assertNull(value.get());
  }

  @Test
  void integerValueSelector() {
    final SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    comboBoxModel.refresh();
    final Key jonesKey = comboBoxModel.getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 3);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    final EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    final IntegerField empIdValue = comboBox.integerFieldSelector(TestDomain.EMP_ID);
    assertEquals(3, empIdValue.getInteger());
    final Key blakeKey = comboBoxModel.getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 5);
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