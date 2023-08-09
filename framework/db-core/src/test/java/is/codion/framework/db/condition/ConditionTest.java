/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.db.TestDomain.Department;
import is.codion.framework.db.TestDomain.Detail;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.db.TestDomain.Master;
import is.codion.framework.db.criteria.Criteria;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.framework.db.condition.Condition.where;
import static is.codion.framework.db.criteria.Criteria.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ConditionTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void keyCriteriaKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> keys(emptyList()));
  }

  @Test
  void selectCondition() {
    SelectCondition condition = where(attribute(Department.LOCATION).equalTo("New York"))
            .selectBuilder()
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertEquals(-1, condition.limit());

    condition = where(all(Department.TYPE)).selectBuilder()
            .limit(10)
            .build();
    assertEquals(10, condition.limit());
  }

  @Test
  void customCriteriaTest() {
    SelectCondition condition = where(customCriteria(Department.NAME_NOT_NULL_CRITERIA))
            .selectBuilder()
            .orderBy(OrderBy.ascending(Department.NAME))
            .build();
    assertTrue(condition.criteria().values().isEmpty());
    assertTrue(condition.criteria().attributes().isEmpty());
  }

  @Test
  void updateConditionDuplicate() {
    assertThrows(IllegalArgumentException.class, () -> where(all(Employee.TYPE)).updateBuilder()
            .set(Employee.COMMISSION, 123d)
            .set(Employee.COMMISSION, 123d));
  }

  @Test
  void combinationEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Criteria.combination(Conjunction.AND));
  }

  @Test
  void combinationEntityTypeMismatch() {
    assertThrows(IllegalArgumentException.class, () -> and(
            attribute(Employee.ID).equalTo(8),
            attribute(Department.NAME).equalTo("name")));
  }

  @Test
  void foreignKeyCriteria() {
    Entity master = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .with(Master.CODE, 3)
            .build();
    Criteria criteria = foreignKey(Detail.MASTER_FK).equalTo(master);
    assertEquals("(master_id = ? and master_id_2 = ?)", criteria.toString(ENTITIES.definition(Detail.TYPE)));
    Criteria criteria2 = foreignKey(Detail.MASTER_VIA_CODE_FK).equalTo(master);
    assertEquals("master_code = ?", criteria2.toString(ENTITIES.definition(Detail.TYPE)));
  }

  @Test
  void compositePrimaryKeyCriteriaWithNullValues() {
    Key masterKey = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, null)
            .with(Master.CODE, 3)
            .build();
    key(masterKey);

    Key masterKey2 = ENTITIES.keyBuilder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .with(Master.CODE, 42)
            .build();

    keys(Arrays.asList(masterKey, masterKey2));
  }

  @Test
  void combination() {
    Combination combination1 = and(
            attribute(Detail.STRING).equalTo("value"),
            attribute(Detail.INT).equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    assertEquals("(string = ? and int = ?)", combination1.toString(detailDefinition));
    Combination combination2 = and(
            attribute(Detail.DOUBLE).equalTo(666.666),
            attribute(Detail.STRING).equalToIgnoreCase("valu%e2"));
    Combination combination3 = or(combination1, combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.toString(detailDefinition));
  }

  @Test
  void attributeCriteriaTest() {
    Criteria critOne = attribute(Department.LOCATION).equalTo("New York");
    assertEquals("loc = ?", critOne.toString(ENTITIES.definition(Department.TYPE)));
    assertNotNull(critOne);
  }

  @Test
  void foreignKeyCriteriaNull() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    Criteria criteria = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", criteria.toString(definition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", criteria.toString(definition));
  }

  @Test
  void foreignKeyCriteriaEntity() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Criteria criteria = foreignKey(Employee.DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", criteria.toString(empDefinition));

    Entity department2 = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 11)
            .build();
    criteria = foreignKey(Employee.DEPARTMENT_FK).in(asList(department, department2));
    assertEquals("deptno in (?, ?)", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).notIn(asList(department, department2));
    assertEquals("deptno not in (?, ?)", criteria.toString(empDefinition));
  }

  @Test
  void foreignKeyCriteriaEntityKey() {
    Entity department = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();
    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Criteria criteria = foreignKey(Employee.DEPARTMENT_FK).equalTo(department);
    assertEquals("deptno = ?", criteria.toString(empDefinition));
  }

  @Test
  void compositeForeignKey() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 3)
            .with(Master.ID_2, 4)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    Criteria criteria = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id = ? and master_id_2 = ?)", criteria.toString(detailDefinition));

    criteria = foreignKey(Detail.MASTER_FK).notEqualTo(master1);
    assertEquals("(master_id <> ? and master_id_2 <> ?)", criteria.toString(detailDefinition));

    criteria = foreignKey(Detail.MASTER_FK).in(singletonList(master1));
    assertEquals("(master_id = ? and master_id_2 = ?)", criteria.toString(detailDefinition));

    criteria = foreignKey(Detail.MASTER_FK).in(asList(master1, master2));
    assertEquals("((master_id = ? and master_id_2 = ?) or (master_id = ? and master_id_2 = ?))", criteria.toString(detailDefinition));

    criteria = foreignKey(Detail.MASTER_FK).notIn(asList(master1, master2));
    assertEquals("((master_id <> ? and master_id_2 <> ?) or (master_id <> ? and master_id_2 <> ?))", criteria.toString(detailDefinition));

    criteria = foreignKey(Detail.MASTER_FK).notIn(singletonList(master1));
    assertEquals("(master_id <> ? and master_id_2 <> ?)", criteria.toString(detailDefinition));
  }

  @Test
  void selectConditionCompositeKey() {
    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 1)
            .with(Master.ID_2, 2)
            .build();

    Entity master2 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, 3)
            .with(Master.ID_2, 4)
            .build();

    EntityDefinition masterDefinition = ENTITIES.definition(Master.TYPE);
    Criteria criteria = key(master1.primaryKey());
    assertEquals("(id = ? and id2 = ?)", criteria.toString(masterDefinition));

    criteria = keys(asList(master1.primaryKey(), master2.primaryKey()));
    assertEquals("((id = ? and id2 = ?) or (id = ? and id2 = ?))", criteria.toString(masterDefinition));
  }

  @Test
  void keyNullCriteria() {
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).in((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).in((Collection<Entity>) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).notIn((Entity[]) null));
    assertThrows(NullPointerException.class, () ->
            foreignKey(Employee.DEPARTMENT_FK).notIn((Collection<Entity>) null));

    EntityDefinition empDefinition = ENTITIES.definition(Employee.TYPE);
    Criteria criteria = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).equalTo(null);
    assertEquals("deptno is null", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).in(emptyList());
    assertEquals("deptno in ()", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals("deptno is null", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertEquals("deptno is not null", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).notEqualTo(null);
    assertEquals("deptno is not null", criteria.toString(empDefinition));

    criteria = foreignKey(Employee.DEPARTMENT_FK).notIn(emptyList());
    assertEquals("deptno not in ()", criteria.toString(empDefinition));

    Entity master1 = ENTITIES.builder(Master.TYPE)
            .with(Master.ID_1, null)
            .with(Master.ID_2, null)
            .build();

    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    criteria = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 is null)", criteria.toString(detailDefinition));

    master1.put(Master.ID_2, 1);
    criteria = foreignKey(Detail.MASTER_FK).equalTo(master1);
    assertEquals("(master_id is null and master_id_2 = ?)", criteria.toString(detailDefinition));

    Entity dept = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 42)
            .build();

    criteria = foreignKey(Employee.DEPARTMENT_FK).equalTo(dept);
    assertEquals("deptno = ?", criteria.toString(empDefinition));
  }

  @Test
  void criteriaTest() {
    Entity entity = ENTITIES.builder(Department.TYPE)
            .with(Department.ID, 10)
            .build();

    EntityDefinition deptDefinition = ENTITIES.definition(Department.TYPE);

    Criteria criteria = key(entity.primaryKey());
    assertDepartmentKeyCriteria(criteria, deptDefinition, "deptno = ?");

    criteria = attribute(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCriteria(criteria, deptDefinition, "dname <> ?", 1);

    criteria = attribute(Department.NAME).notIn("DEPT", "DEPT2");
    assertDepartmentCriteria(criteria, deptDefinition, "dname not in (?, ?)", 2);

    criteria = keys(singletonList(entity.primaryKey()));
    assertDepartmentKeyCriteria(criteria, deptDefinition, "deptno in (?)");

    criteria = attribute(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCriteria(criteria, deptDefinition, "dname <> ?", 1);
  }

  @Test
  void selectAllCondition() {
    Condition selectCondition = where(all(Department.TYPE));
    assertTrue(selectCondition.criteria().values().isEmpty());
    assertTrue(selectCondition.criteria().attributes().isEmpty());

    Condition condition = where(all(Department.TYPE));
    assertTrue(condition.criteria().values().isEmpty());
    assertTrue(condition.criteria().attributes().isEmpty());
  }

  @Test
  void attributeCriteriaWithNonColumnProperty() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () ->
            attribute(Employee.DEPARTMENT_LOCATION).isNull().toString(definition));
  }

  @Test
  void criteriaNullOrEmptyValues() {
    assertThrows(NullPointerException.class, () -> attribute(Department.NAME).in((String[]) null));
    assertThrows(NullPointerException.class, () -> attribute(Department.NAME).in((Collection<String>) null));

    assertThrows(NullPointerException.class, () -> attribute(Department.NAME).notIn((String[]) null));
    assertThrows(NullPointerException.class, () -> attribute(Department.NAME).notIn((Collection<String>) null));
  }

  @Test
  void whereClause() {
    EntityDefinition departmentDefinition = ENTITIES.definition(Department.TYPE);
    ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.property(Department.NAME);
    Criteria criteria = attribute(Department.NAME).equalTo("upper%");
    assertEquals(property.columnExpression() + " like ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).equalTo("upper");
    assertEquals(property.columnExpression() + " = ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).isNull();
    assertEquals(property.columnExpression() + " is null", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).equalTo((String) null);
    assertEquals(property.columnExpression() + " is null", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).in(emptyList());
    assertEquals(property.columnExpression() + " in ()", criteria.toString(departmentDefinition));

    criteria = attribute(Department.NAME).notEqualTo("upper%");
    assertEquals(property.columnExpression() + " not like ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).notEqualTo("upper");
    assertEquals(property.columnExpression() + " <> ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).isNotNull();
    assertEquals(property.columnExpression() + " is not null", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).notEqualTo(null);
    assertEquals(property.columnExpression() + " is not null", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).notIn(emptyList());
    assertEquals(property.columnExpression() + " not in ()", criteria.toString(departmentDefinition));

    criteria = attribute(Department.NAME).greaterThan("upper");
    assertEquals(property.columnExpression() + " > ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).greaterThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " >= ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).lessThan("upper");
    assertEquals(property.columnExpression() + " < ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).lessThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " <= ?", criteria.toString(departmentDefinition));

    criteria = attribute(Department.NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " > ? and " + property.columnExpression() + " < ?)", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).between("upper", "lower");
    assertEquals("(" + property.columnExpression() + " >= ? and " + property.columnExpression() + " <= ?)", criteria.toString(departmentDefinition));

    criteria = attribute(Department.NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " <= ? or " + property.columnExpression() + " >= ?)", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).notBetween("upper", "lower");
    assertEquals("(" + property.columnExpression() + " < ? or " + property.columnExpression() + " > ?)", criteria.toString(departmentDefinition));

    criteria = attribute(Department.NAME).equalTo("%upper%");
    assertEquals(property.columnExpression() + " like ?", criteria.toString(departmentDefinition));
    criteria = attribute(Department.NAME).notEqualTo("%upper%");
    assertEquals(property.columnExpression() + " not like ?", criteria.toString(departmentDefinition));
  }

  @Test
  void equals() {
    Condition condition1 = where(all(Department.TYPE));
    Condition condition2 = where(all(Department.TYPE));
    assertEquals(condition1, condition2);
    condition2 = where(all(Employee.TYPE));
    assertNotEquals(condition1, condition2);

    Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
    Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
    condition1 = where(key(key1));
    condition2 = where(key(key1));
    assertEquals(condition1, condition2);
    condition2 = where(key(key2));
    assertNotEquals(condition1, condition2);

    condition1 = where(foreignKey(Employee.DEPARTMENT_FK).isNull());
    condition2 = where(foreignKey(Employee.DEPARTMENT_FK).isNull());
    assertEquals(condition1, condition2);
    condition2 = where(foreignKey(Employee.DEPARTMENT_FK).isNotNull());
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).equalTo(0));
    condition2 = where(attribute(Employee.ID).equalTo(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).equalTo(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.NAME).equalTo("Luke"));
    condition2 = where(attribute(Employee.NAME).equalTo("Luke"));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.NAME).equalToIgnoreCase("Luke"));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).notEqualTo(0));
    condition2 = where(attribute(Employee.ID).notEqualTo(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).equalTo(0));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).notEqualTo(0));
    condition2 = where(attribute(Employee.ID).notEqualTo(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).notEqualTo(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).lessThan(0));
    condition2 = where(attribute(Employee.ID).lessThan(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).lessThan(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).lessThanOrEqualTo(0));
    condition2 = where(attribute(Employee.ID).lessThanOrEqualTo(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).lessThanOrEqualTo(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).greaterThan(0));
    condition2 = where(attribute(Employee.ID).greaterThan(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).greaterThan(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).greaterThanOrEqualTo(0));
    condition2 = where(attribute(Employee.ID).greaterThanOrEqualTo(0));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).greaterThanOrEqualTo(1));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).between(0, 1));
    condition2 = where(attribute(Employee.ID).between(0, 1));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).between(1, 0));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).betweenExclusive(0, 1));
    condition2 = where(attribute(Employee.ID).betweenExclusive(0, 1));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).betweenExclusive(1, 0));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).notBetween(0, 1));
    condition2 = where(attribute(Employee.ID).notBetween(0, 1));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).notBetween(1, 0));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.ID).notBetweenExclusive(0, 1));
    condition2 = where(attribute(Employee.ID).notBetweenExclusive(0, 1));
    assertEquals(condition1, condition2);
    condition2 = where(attribute(Employee.ID).notBetweenExclusive(1, 0));
    assertNotEquals(condition1, condition2);

    condition1 = where(customCriteria(Department.CRITERIA,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test")));
    condition2 = where(customCriteria(Department.CRITERIA,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test")));
    assertEquals(condition1, condition2);

    condition1 = where(or(attribute(Employee.ID).equalTo(0),
            attribute(Employee.ID).equalTo(1)));
    condition2 = where(or(attribute(Employee.ID).equalTo(0),
            attribute(Employee.ID).equalTo(1)));
    assertEquals(condition1, condition2);
    condition2 = where(or(attribute(Employee.ID).equalTo(1),
            attribute(Employee.ID).equalTo(0)));
    assertNotEquals(condition1, condition2);

    condition1 = where(or(attribute(Employee.ID).equalTo(0),
            attribute(Employee.NAME).equalTo("Luke")));
    condition2 = where(or(attribute(Employee.ID).equalTo(0),
            attribute(Employee.NAME).equalTo("Luke")));
    assertEquals(condition1, condition2);
    condition2 = where(or(attribute(Employee.ID).equalTo(0),
            attribute(Employee.NAME).equalTo("Lukas")));
    assertNotEquals(condition1, condition2);

    condition1 = where(attribute(Employee.NAME).in("Luke", "John"));
    condition2 = where(attribute(Employee.NAME).in("Luke", "John"));
    assertEquals(condition1.selectBuilder().build(), condition2.selectBuilder().build());
    assertEquals(condition1.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            condition2.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build());
    assertNotEquals(condition1.selectBuilder()
                    .orderBy(OrderBy.ascending(Employee.NAME))
                    .build(),
            condition2.selectBuilder()
                    .build());

    assertEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build());

    assertEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .offset(10)
                    .build());

    assertNotEquals(condition1.selectBuilder()
                    .selectAttributes(Employee.NAME)
                    .build(),
            condition2.selectBuilder()
                    .selectAttributes(Employee.ID)
                    .build());

    condition1 = where(attribute(Employee.NAME).equalTo("Luke"));
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = where(attribute(Employee.NAME).greaterThanOrEqualTo("Luke"));
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = where(attribute(Employee.NAME).lessThanOrEqualTo("Luke"));
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = where(attribute(Employee.NAME).greaterThanOrEqualTo("Luke"));
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = where(attribute(Employee.NAME).betweenExclusive("John", "Luke"));
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = where(attribute(Employee.NAME).greaterThanOrEqualTo("Luke"));
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);

    condition1 = where(attribute(Employee.NAME).notBetweenExclusive("John", "Luke"));
    condition2 = condition1;
    assertEquals(condition1, condition2);

    condition2 = where(attribute(Employee.NAME).lessThanOrEqualTo("Luke"));
    assertNotEquals(condition1, condition2);
    assertNotEquals(condition2, condition1);
  }

  private static void assertDepartmentKeyCriteria(Criteria criteria, EntityDefinition departmentDefinition,
                                                  String conditionString) {
    assertEquals(conditionString, criteria.toString(departmentDefinition));
    assertEquals(1, criteria.values().size());
    assertEquals(1, criteria.attributes().size());
    assertEquals(10, criteria.values().get(0));
    assertEquals(Department.ID, criteria.attributes().get(0));
  }

  private static void assertDepartmentCriteria(Criteria criteria, EntityDefinition departmentDefinition,
                                               String conditionString, int valueCount) {
    assertEquals(conditionString, criteria.toString(departmentDefinition));
    assertEquals(valueCount, criteria.values().size());
    assertEquals(valueCount, criteria.attributes().size());
    assertEquals("DEPT", criteria.values().get(0));
    assertEquals(Department.NAME, criteria.attributes().get(0));
  }
}
