/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.Operator;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.test.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTableConditionModelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          DatabaseFactory.getDatabase()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory());

  @Test
  void test() {
    assertEquals(TestDomain.T_EMP, conditionModel.getEntityType());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(9, conditionModel.getFilterModels().size());
    assertEquals(10, conditionModel.getConditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(conditionModel.isEnabled());
    conditionModel.enable(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(conditionModel.isEnabled());

    conditionModel.refresh();
  }

  @Test
  void noSearchPropertiesDefined() {
    final DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(TestDomain.T_DETAIL,
            CONNECTION_PROVIDER, new DefaultFilterModelFactory(), new DefaultConditionModelFactory());
    //no search properties defined for master entity
    assertThrows(IllegalStateException.class, () ->
            ((DefaultForeignKeyConditionModel) model.getConditionModel(TestDomain.DETAIL_MASTER_FK)).getEntitySearchModel().performQuery());
  }

  @Test
  void getPropertyFilterModel() {
    assertNotNull(conditionModel.getFilterModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  void getPropertyConditionModel() {
    assertNotNull(conditionModel.getConditionModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  void getPropertyConditionModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> assertNull(conditionModel.getConditionModel(TestDomain.DEPARTMENT_ID)));
  }

  @Test
  void setEqualFilterValue() {
    conditionModel.setEqualFilterValue(TestDomain.EMP_COMMISSION, 1400d);
    final ColumnConditionModel<?, Double> propertyConditionModel = conditionModel.getFilterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertTrue(conditionModel.isFilterEnabled(TestDomain.EMP_COMMISSION));
    assertEquals(Operator.EQUAL, propertyConditionModel.getOperator());
    assertEquals(1400d, propertyConditionModel.getEqualValue());
  }

  @Test
  void setEqualConditionValues() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(conditionModel.getConditionModel(TestDomain.EMP_DEPARTMENT_FK).getEqualValues().contains(sales));
    assertTrue(conditionModel.getConditionModel(TestDomain.EMP_DEPARTMENT_FK).getEqualValues().contains(accounting));
    searchStateChanged = conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void clearPropertyConditionModels() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.clearConditionModels();
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  void getCondition() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isConditionEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    final ColumnConditionModel<?, String> nameConditionModel = conditionModel.getConditionModel(TestDomain.EMP_NAME);
    nameConditionModel.setEqualValue("SCOTT");
    conditionModel.setAdditionalConditionSupplier(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_2_TYPE));
    assertNotNull(conditionModel.getAdditionalConditionSupplier());
  }

  @Test
  void conditionChangedListener() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener conditionChangedListener = counter::incrementAndGet;
    conditionModel.addConditionListener(conditionChangedListener);
    conditionModel.getConditionModel(TestDomain.EMP_COMMISSION).setEnabled(true);
    assertEquals(1, counter.get());
    conditionModel.getConditionModel(TestDomain.EMP_COMMISSION).setEnabled(false);
    assertEquals(2, counter.get());
    conditionModel.getConditionModel(TestDomain.EMP_COMMISSION).setOperator(Operator.GREATER_THAN_OR_EQUAL);
    conditionModel.getConditionModel(TestDomain.EMP_COMMISSION).setLowerBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(3, counter.get());
    conditionModel.removeConditionListener(conditionChangedListener);
  }

  @Test
  void testSearchState() {
    assertFalse(conditionModel.hasConditionChanged());
    conditionModel.getConditionModel(TestDomain.EMP_JOB).setEqualValue("job");
    assertTrue(conditionModel.hasConditionChanged());
    conditionModel.getConditionModel(TestDomain.EMP_JOB).setEnabled(false);
    assertFalse(conditionModel.hasConditionChanged());
    conditionModel.getConditionModel(TestDomain.EMP_JOB).setEnabled(true);
    assertTrue(conditionModel.hasConditionChanged());
    conditionModel.rememberCondition();
    assertFalse(conditionModel.hasConditionChanged());
  }

  @Test
  void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = Property.WILDCARD_CHARACTER.get();
    conditionModel.getSimpleConditionStringValue().set(value);
    for (final ColumnConditionModel<?, ?> model : conditionModel.getConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertEquals(wildcard + value + wildcard, model.getEqualValue());
        assertTrue(model.isEnabled());
      }
    }
    conditionModel.getSimpleConditionStringValue().set(null);
    for (final ColumnConditionModel<?, ?> model : conditionModel.getConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
