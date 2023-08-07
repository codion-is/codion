/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.Conjunction;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
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
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private final EntityTableConditionModel<Attribute<?>> conditionModel = new DefaultEntityTableConditionModel<>(Employee.TYPE,
          CONNECTION_PROVIDER, new EntityConditionModelFactory(CONNECTION_PROVIDER));

  @Test
  void test() {
    assertEquals(Employee.TYPE, conditionModel.entityType());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(10, conditionModel.conditionModels().size());

    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));

    assertFalse(conditionModel.isEnabled());
    conditionModel.conditionModel(Employee.DEPARTMENT_FK).setEnabled(true);
    assertTrue(conditionModel.isEnabled());
  }

  @Test
  void noSearchPropertiesDefined() {
    DefaultEntityTableConditionModel<Attribute<?>> model = new DefaultEntityTableConditionModel<>(Detail.TYPE,
            CONNECTION_PROVIDER, new EntityConditionModelFactory(CONNECTION_PROVIDER));
    //no search properties defined for master entity
    ColumnConditionModel<? extends Attribute<Entity>, Entity> masterModel =
            model.attributeModel(Detail.MASTER_FK);
    assertThrows(IllegalStateException.class, () ->
            ((EntitySearchModelConditionModel) masterModel).entitySearchModel().performQuery());
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
  void setEqualConditionValues() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
    ColumnConditionModel<ForeignKey, Entity> deptModel =
            conditionModel.attributeModel(Employee.DEPARTMENT_FK);
    assertTrue(deptModel.getEqualValues().contains(sales));
    assertTrue(deptModel.getEqualValues().contains(accounting));
    searchStateChanged = conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
  }

  @Test
  void clearPropertyConditionModels() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
    conditionModel.clear();
    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
  }

  @Test
  void condition() throws DatabaseException {
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(Employee.DEPARTMENT_FK));
    conditionModel.setEqualConditionValues(Employee.DEPARTMENT_FK, asList(sales, accounting));
    ColumnConditionModel<?, String> nameConditionModel = conditionModel.attributeModel(Employee.NAME);
    nameConditionModel.setEqualValue("SCOTT");
    conditionModel.setAdditionalCriteriaSupplier(() -> Condition.customCriteria(Employee.CRITERIA_2_TYPE));
    assertNotNull(conditionModel.getAdditionalCriteriaSupplier());
  }
}
