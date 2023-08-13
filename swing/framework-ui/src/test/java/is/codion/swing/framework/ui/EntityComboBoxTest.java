/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.swing.common.ui.component.text.NumberField;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.EntityComboBoxModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.condition.Condition.column;
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
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  @Test
  void inputProvider() throws Exception {
    EntityComboBoxModel model = new EntityComboBoxModel(Department.TYPE, CONNECTION_PROVIDER);
    model.refresh();
    Entity operations = CONNECTION_PROVIDER.connection().selectSingle(column(Department.NAME).equalTo("OPERATIONS"));
    model.setSelectedItem(operations);
    ComponentValue<Entity, EntityComboBox> value = EntityComboBox.builder(model)
            .buildValue();

    assertNotNull(value.get());

    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(
            column(Department.NAME).equalTo("SALES"));

    model.setSelectedItem(sales);
    assertEquals(sales, value.get());
    model.setSelectedItem(null);
    assertNull(value.get());
  }

  @Test
  void integerSelectorField() {
    EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(Employee.TYPE, CONNECTION_PROVIDER);
    comboBoxModel.refresh();
    Key jonesKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 3);
    comboBoxModel.selectByKey(jonesKey);
    EntityComboBox comboBox = new EntityComboBox(comboBoxModel);
    NumberField<Integer> empIdValue = comboBox.integerSelectorField(Employee.ID).build();
    assertEquals(3, empIdValue.getNumber());
    Key blakeKey = comboBoxModel.connectionProvider().entities().primaryKey(Employee.TYPE, 5);
    comboBoxModel.selectByKey(blakeKey);
    assertEquals(5, empIdValue.getNumber());
    comboBoxModel.setSelectedItem(null);
    assertNull(empIdValue.getNumber());
    empIdValue.setNumber(10);
    assertEquals("ADAMS", comboBoxModel.selectedValue().get(Employee.NAME));
    empIdValue.setNumber(null);
    assertNull(comboBoxModel.selectedValue());
  }
}