/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.component.text.NumberField;
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
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void inputProvider() throws Exception {
    SwingEntityComboBoxModel model = new SwingEntityComboBoxModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    model.refresh();
    Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");
    model.setSelectedItem(operations);
    ComponentValue<Entity, EntityComboBox> value = EntityComboBox.builder(model)
            .buildComponentValue();

    assertNotNull(value.get());

    Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(
            TestDomain.DEPARTMENT_NAME, "SALES");

    model.setSelectedItem(sales);
    assertEquals(sales, value.get());
    model.setSelectedItem(null);
    assertNull(value.get());
  }

  @Test
  void integerValueSelector() {
    SwingEntityComboBoxModel comboBoxModel = new SwingEntityComboBoxModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
    comboBoxModel.refresh();
    Key jonesKey = comboBoxModel.getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 3);
    comboBoxModel.setSelectedEntityByKey(jonesKey);
    EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    NumberField<Integer> empIdValue = comboBox.integerFieldSelector(TestDomain.EMP_ID).build();
    assertEquals(3, empIdValue.getValue());
    Key blakeKey = comboBoxModel.getConnectionProvider().getEntities().primaryKey(TestDomain.T_EMP, 5);
    comboBoxModel.setSelectedEntityByKey(blakeKey);
    assertEquals(5, empIdValue.getValue());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.getValue());
    empIdValue.setValue(10);
    assertEquals("ADAMS", comboBoxModel.getSelectedValue().get(TestDomain.EMP_NAME));
    empIdValue.setValue(null);
    assertNull(comboBoxModel.getSelectedValue());
  }
}