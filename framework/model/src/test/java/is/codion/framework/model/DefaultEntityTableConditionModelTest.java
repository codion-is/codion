/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.Operator;
import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.tests.TestDomain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTableConditionModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));
  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(),
          new DefaultPropertyConditionModelProvider());

  @Test
  public void test() {
    assertEquals(TestDomain.T_EMP, conditionModel.getEntityType());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(9, conditionModel.getPropertyFilterModels().size());
    assertEquals(8, conditionModel.getPropertyConditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(conditionModel.isEnabled());
    conditionModel.enable(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(conditionModel.isEnabled());

    conditionModel.clear();
    conditionModel.refresh();
  }

  @Test
  public void noSearchPropertiesDefined() {
    final DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(TestDomain.T_DETAIL,
            CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(), new DefaultPropertyConditionModelProvider());
    //no search properties defined for master entity
    assertThrows(IllegalStateException.class, () ->
            ((DefaultForeignKeyConditionModel) model.getPropertyConditionModel(TestDomain.DETAIL_MASTER_FK)).getEntityLookupModel().performQuery());
  }

  @Test
  public void getPropertyFilterModel() {
    assertNotNull(conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void getPropertyConditionModel() {
    assertNotNull(conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void getPropertyConditionModelNonExisting() {
    final EntityType entityType = Entities.entityType("test");
    assertThrows(IllegalArgumentException.class, () -> assertNull(conditionModel.getPropertyConditionModel(entityType.integerAttribute("bla bla"))));
  }

  @Test
  public void setFilterValue() {
    conditionModel.setFilterValue(TestDomain.EMP_COMMISSION, 1400d);
    final ColumnConditionModel<Entity, Property<?>> propertyConditionModel = conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertTrue(conditionModel.isFilterEnabled(TestDomain.EMP_COMMISSION));
    assertEquals(Operator.LIKE, propertyConditionModel.getOperator());
    assertEquals(1400d, propertyConditionModel.getUpperBound());
  }

  @Test
  public void setConditionValues() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(((ForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK)).getConditionEntities().contains(sales));
    assertTrue(((ForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK)).getConditionEntities().contains(accounting));
    searchStateChanged = conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void clearPropertyConditionModels() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.clearPropertyConditionModels();
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void getCondition() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    final ColumnConditionModel<?, ?> nameConditionModel = conditionModel.getPropertyConditionModel(TestDomain.EMP_NAME);
    nameConditionModel.setLikeValue("SCOTT");
    conditionModel.setAdditionalConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_2_ID));
    assertNotNull(conditionModel.getAdditionalConditionProvider());
  }

  @Test
  public void conditionChangedListener() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener conditionChangedListener = counter::incrementAndGet;
    conditionModel.addConditionChangedListener(conditionChangedListener);
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setEnabled(true);
    assertEquals(1, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setEnabled(false);
    assertEquals(2, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setUpperBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(3, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setOperator(Operator.GREATER_THAN);
    assertEquals(3, counter.get());
    conditionModel.removeConditionChangedListener(conditionChangedListener);
  }

  @Test
  public void testSearchState() {
    assertFalse(conditionModel.hasConditionChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setLikeValue("job");
    assertTrue(conditionModel.hasConditionChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setEnabled(false);
    assertFalse(conditionModel.hasConditionChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setEnabled(true);
    assertTrue(conditionModel.hasConditionChanged());
    conditionModel.rememberCondition();
    assertFalse(conditionModel.hasConditionChanged());
  }

  @Test
  public void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = Property.WILDCARD_CHARACTER.get();
    conditionModel.setSimpleConditionString(value);
    for (final ColumnConditionModel<?, ?> model : conditionModel.getPropertyConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertEquals(wildcard + value + wildcard, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    conditionModel.setSimpleConditionString(null);
    for (final ColumnConditionModel<?, ?> model : conditionModel.getPropertyConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
