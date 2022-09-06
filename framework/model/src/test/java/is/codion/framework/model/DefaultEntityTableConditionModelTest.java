/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTableConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(Employee.TYPE,
          CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory(CONNECTION_PROVIDER));

  @Test
  void test() {
    assertEquals(Employee.TYPE, conditionModel.entityType());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(7, conditionModel.filterModels().size());
    assertEquals(10, conditionModel.conditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(Employee.DEPARTMENT_FK));
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));

    assertFalse(conditionModel.isConditionEnabled());
    conditionModel.conditionModel(Employee.DEPARTMENT_FK).setEnabled(true);
    assertTrue(conditionModel.isConditionEnabled());

    conditionModel.refresh();
  }

  @Test
  void noSearchPropertiesDefined() {
    DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(Detail.TYPE,
            CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory(CONNECTION_PROVIDER));
    //no search properties defined for master entity
    ColumnConditionModel<? extends Attribute<Entity>, Entity> masterModel =
            model.conditionModel(Detail.MASTER_FK);
    assertThrows(IllegalStateException.class, () ->
            ((DefaultForeignKeyConditionModel) masterModel).entitySearchModel().performQuery());
  }

  @Test
  void filterModel() {
    assertNotNull(conditionModel.filterModel(Employee.COMMISSION));
  }

  @Test
  void conditionModel() {
    assertNotNull(conditionModel.conditionModel(Employee.COMMISSION));
  }

  @Test
  void conditionModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> conditionModel.conditionModel(Department.ID));
  }

  @Test
  void filterModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> conditionModel.filterModel(Employee.DEPARTMENT_FK));
    assertThrows(IllegalArgumentException.class, () -> conditionModel.filterModel(Employee.DEPARTMENT_FK));
  }

  @Test
  void setEqualFilterValue() {
    conditionModel.setEqualFilterValue(Employee.COMMISSION, 1400d);
    ColumnConditionModel<?, Double> propertyConditionModel = conditionModel.filterModel(Employee.COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertTrue(conditionModel.isFilterEnabled(Employee.COMMISSION));
    assertEquals(Operator.EQUAL, propertyConditionModel.getOperator());
    assertEquals(1400d, propertyConditionModel.getEqualValue());
  }

  @Test
  void setEqualConditionValues() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
    ColumnConditionModel<? extends Attribute<Entity>, Entity> deptModel =
            conditionModel.conditionModel(Employee.DEPARTMENT_FK);
    assertTrue(deptModel.getEqualValues().contains(sales));
    assertTrue(deptModel.getEqualValues().contains(accounting));
    searchStateChanged = conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
  }

  @Test
  void clearPropertyConditionModels() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
    conditionModel.clearConditions();
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
  }

  @Test
  void clearPropertyFilterModels() throws DatabaseException {
    assertFalse(conditionModel.isFilterEnabled(Employee.NAME));
    conditionModel.setEqualFilterValue(Employee.NAME, "SCOTT");
    assertTrue(conditionModel.isFilterEnabled(Employee.NAME));
    conditionModel.clearFilters();
    assertFalse(conditionModel.isFilterEnabled(Employee.NAME));
  }

  @Test
  void condition() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(Employee.DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    ColumnConditionModel<?, String> nameConditionModel = conditionModel.conditionModel(Employee.NAME);
    nameConditionModel.setEqualValue("SCOTT");
    conditionModel.setAdditionalConditionSupplier(() -> Conditions.customCondition(Employee.CONDITION_2_TYPE));
    assertNotNull(conditionModel.getAdditionalConditionSupplier());
  }

  @Test
  void testSimpleSearchString() {
    final String value = "test";
    char wildcard = Text.WILDCARD_CHARACTER.get();
    conditionModel.simpleConditionStringValue().set(value);
    for (ColumnConditionModel<?, ?> model : conditionModel.conditionModels().values()) {
      if (model.columnClass().equals(String.class)) {
        assertEquals(wildcard + value + wildcard, model.getEqualValue());
        assertTrue(model.isEnabled());
      }
    }
    conditionModel.simpleConditionStringValue().set(null);
    for (ColumnConditionModel<?, ?> model : conditionModel.conditionModels().values()) {
      if (model.columnClass().equals(String.class)) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
