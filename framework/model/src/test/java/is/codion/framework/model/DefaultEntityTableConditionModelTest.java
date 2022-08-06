/*
 * Copyright (c) 2016 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.Text;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTableConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory(CONNECTION_PROVIDER));

  @Test
  void test() {
    assertEquals(TestDomain.T_EMP, conditionModel.entityType());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(7, conditionModel.filterModels().size());
    assertEquals(10, conditionModel.conditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(conditionModel.isConditionEnabled());
    conditionModel.conditionModel(TestDomain.EMP_DEPARTMENT_FK).setEnabled(true);
    assertTrue(conditionModel.isConditionEnabled());

    conditionModel.refresh();
  }

  @Test
  void noSearchPropertiesDefined() {
    DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(TestDomain.T_DETAIL,
            CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory(CONNECTION_PROVIDER));
    //no search properties defined for master entity
    ColumnConditionModel<? extends Attribute<Entity>, Entity> masterModel =
            model.conditionModel(TestDomain.DETAIL_MASTER_FK);
    assertThrows(IllegalStateException.class, () ->
            ((DefaultForeignKeyConditionModel) masterModel).entitySearchModel().performQuery());
  }

  @Test
  void getPropertyFilterModel() {
    assertNotNull(conditionModel.filterModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  void getPropertyConditionModel() {
    assertNotNull(conditionModel.conditionModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  void getPropertyConditionModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> conditionModel.conditionModel(TestDomain.DEPARTMENT_ID));
  }

  @Test
  void getPropertyFilterModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> conditionModel.filterModel(TestDomain.EMP_DEPARTMENT_FK));
    assertThrows(IllegalArgumentException.class, () -> conditionModel.filterModel(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void setEqualFilterValue() {
    conditionModel.setEqualFilterValue(TestDomain.EMP_COMMISSION, 1400d);
    ColumnConditionModel<?, Double> propertyConditionModel = conditionModel.filterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertTrue(conditionModel.isFilterEnabled(TestDomain.EMP_COMMISSION));
    assertEquals(Operator.EQUAL, propertyConditionModel.getOperator());
    assertEquals(1400d, propertyConditionModel.getEqualValue());
  }

  @Test
  void setEqualConditionValues() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    ColumnConditionModel<? extends Attribute<Entity>, Entity> deptModel =
            conditionModel.conditionModel(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(deptModel.getEqualValues().contains(sales));
    assertTrue(deptModel.getEqualValues().contains(accounting));
    searchStateChanged = conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void clearPropertyConditionModels() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.clearConditions();
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void clearPropertyFilterModels() throws DatabaseException {
    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_NAME));
    conditionModel.setEqualFilterValue(TestDomain.EMP_NAME, "SCOTT");
    assertTrue(conditionModel.isFilterEnabled(TestDomain.EMP_NAME));
    conditionModel.clearFilters();
    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_NAME));
  }

  @Test
  void getCondition() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    ColumnConditionModel<?, String> nameConditionModel = conditionModel.conditionModel(TestDomain.EMP_NAME);
    nameConditionModel.setEqualValue("SCOTT");
    conditionModel.setAdditionalConditionSupplier(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_2_TYPE));
    assertNotNull(conditionModel.getAdditionalConditionSupplier());
  }

  @Test
  void conditionChangedListener() {
    AtomicInteger counter = new AtomicInteger();
    EventListener conditionChangedListener = counter::incrementAndGet;
    conditionModel.addConditionChangedListener(conditionChangedListener);
    ColumnConditionModel<? extends Attribute<Double>, Double> commissionModel =
            conditionModel.conditionModel(TestDomain.EMP_COMMISSION);
    commissionModel.setEnabled(true);
    assertEquals(1, counter.get());
    commissionModel.setEnabled(false);
    assertEquals(2, counter.get());
    commissionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    commissionModel.setLowerBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(3, counter.get());
    this.conditionModel.removeConditionChangedListener(conditionChangedListener);
  }

  @Test
  void testSearchState() {
    assertFalse(conditionModel.hasConditionChanged());
    ColumnConditionModel<? extends Attribute<String>, String> jobModel =
            conditionModel.conditionModel(TestDomain.EMP_JOB);
    jobModel.setEqualValue("job");
    assertTrue(conditionModel.hasConditionChanged());
    jobModel.setEnabled(false);
    assertFalse(conditionModel.hasConditionChanged());
    jobModel.setEnabled(true);
    assertTrue(conditionModel.hasConditionChanged());
    this.conditionModel.rememberCondition();
    assertFalse(conditionModel.hasConditionChanged());
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
