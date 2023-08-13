/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.criteria;

import is.codion.common.Conjunction;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static is.codion.framework.db.TestDomain.*;
import static is.codion.framework.db.criteria.Criteria.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class CriteriaTest {

  private static final Entities ENTITIES = new TestDomain().entities();

  @Test
  void customCriteriaTest() {
    Criteria criteria = customCriteria(Department.NAME_NOT_NULL_CRITERIA);
    assertTrue(criteria.values().isEmpty());
    assertTrue(criteria.columns().isEmpty());
  }

  @Test
  void keyCriteriaKeyNoKeys() {
    assertThrows(IllegalArgumentException.class, () -> keys(emptyList()));
  }

  @Test
  void combinationEmpty() {
    assertThrows(IllegalArgumentException.class, () -> Criteria.combination(Conjunction.AND));
  }

  @Test
  void combinationEntityTypeMismatch() {
    assertThrows(IllegalArgumentException.class, () -> and(
            column(Employee.ID).equalTo(8),
            column(Department.NAME).equalTo("name")));
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
  void combination() {
    Combination combination1 = and(
            column(Detail.STRING).equalTo("value"),
            column(Detail.INT).equalTo(666));
    EntityDefinition detailDefinition = ENTITIES.definition(Detail.TYPE);
    assertEquals("(string = ? and int = ?)", combination1.toString(detailDefinition));
    Combination combination2 = and(
            column(Detail.DOUBLE).equalTo(666.666),
            column(Detail.STRING).likeIgnoreCase("valu%e2"));
    Combination combination3 = or(combination1, combination2);
    assertEquals("((string = ? and int = ?) or (double = ? and upper(string) like upper(?)))",
            combination3.toString(detailDefinition));
  }

  @Test
  void columnCriteriaTest() {
    Criteria critOne = column(Department.LOCATION).equalTo("New York");
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
  void keyCriteriaCompositeKey() {
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

    criteria = column(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCriteria(criteria, deptDefinition, "dname <> ?", 1);

    criteria = column(Department.NAME).notIn("DEPT", "DEPT2");
    assertDepartmentCriteria(criteria, deptDefinition, "dname not in (?, ?)", 2);

    criteria = keys(singletonList(entity.primaryKey()));
    assertDepartmentKeyCriteria(criteria, deptDefinition, "deptno in (?)");

    criteria = column(Department.NAME).notEqualTo("DEPT");
    assertDepartmentCriteria(criteria, deptDefinition, "dname <> ?", 1);
  }

  @Test
  void allCriteria() {
    Criteria criteria = all(Department.TYPE);
    assertTrue(criteria.values().isEmpty());
    assertTrue(criteria.columns().isEmpty());
  }

  @Test
  void attributeCriteriaWithNonColumnProperty() {
    EntityDefinition definition = ENTITIES.definition(Employee.TYPE);
    assertThrows(IllegalArgumentException.class, () ->
            column(Employee.DEPARTMENT_LOCATION).isNull().toString(definition));
  }

  @Test
  void criteriaNullOrEmptyValues() {
    assertThrows(NullPointerException.class, () -> column(Department.NAME).in((String[]) null));
    assertThrows(NullPointerException.class, () -> column(Department.NAME).in((Collection<String>) null));

    assertThrows(NullPointerException.class, () -> column(Department.NAME).notIn((String[]) null));
    assertThrows(NullPointerException.class, () -> column(Department.NAME).notIn((Collection<String>) null));
  }

  @Test
  void whereClause() {
    EntityDefinition departmentDefinition = ENTITIES.definition(Department.TYPE);
    ColumnProperty<?> property = (ColumnProperty<?>) departmentDefinition.property(Department.NAME);
    Criteria criteria = column(Department.NAME).equalTo("upper");
    assertEquals(property.columnExpression() + " = ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).like("upper%");
    assertEquals(property.columnExpression() + " like ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).equalTo("upper");
    assertEquals(property.columnExpression() + " = ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).isNull();
    assertEquals(property.columnExpression() + " is null", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).equalTo((String) null);
    assertEquals(property.columnExpression() + " is null", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).in(emptyList());
    assertEquals(property.columnExpression() + " in ()", criteria.toString(departmentDefinition));

    criteria = column(Department.NAME).notEqualTo("upper");
    assertEquals(property.columnExpression() + " <> ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notLike("upper%");
    assertEquals(property.columnExpression() + " not like ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notEqualTo("upper");
    assertEquals(property.columnExpression() + " <> ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).isNotNull();
    assertEquals(property.columnExpression() + " is not null", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notEqualTo(null);
    assertEquals(property.columnExpression() + " is not null", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notIn(emptyList());
    assertEquals(property.columnExpression() + " not in ()", criteria.toString(departmentDefinition));

    criteria = column(Department.NAME).greaterThan("upper");
    assertEquals(property.columnExpression() + " > ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).greaterThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " >= ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).lessThan("upper");
    assertEquals(property.columnExpression() + " < ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).lessThanOrEqualTo("upper");
    assertEquals(property.columnExpression() + " <= ?", criteria.toString(departmentDefinition));

    criteria = column(Department.NAME).betweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " > ? and " + property.columnExpression() + " < ?)", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).between("upper", "lower");
    assertEquals("(" + property.columnExpression() + " >= ? and " + property.columnExpression() + " <= ?)", criteria.toString(departmentDefinition));

    criteria = column(Department.NAME).notBetweenExclusive("upper", "lower");
    assertEquals("(" + property.columnExpression() + " <= ? or " + property.columnExpression() + " >= ?)", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notBetween("upper", "lower");
    assertEquals("(" + property.columnExpression() + " < ? or " + property.columnExpression() + " > ?)", criteria.toString(departmentDefinition));

    criteria = column(Department.NAME).equalTo("upper");
    assertEquals(property.columnExpression() + " = ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).like("%upper%");
    assertEquals(property.columnExpression() + " like ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notEqualTo("upper");
    assertEquals(property.columnExpression() + " <> ?", criteria.toString(departmentDefinition));
    criteria = column(Department.NAME).notLike("%upper%");
    assertEquals(property.columnExpression() + " not like ?", criteria.toString(departmentDefinition));
  }

  private static void assertDepartmentKeyCriteria(Criteria criteria, EntityDefinition departmentDefinition,
                                                  String conditionString) {
    assertEquals(conditionString, criteria.toString(departmentDefinition));
    assertEquals(1, criteria.values().size());
    assertEquals(1, criteria.columns().size());
    assertEquals(10, criteria.values().get(0));
    assertEquals(Department.ID, criteria.columns().get(0));
  }

  private static void assertDepartmentCriteria(Criteria criteria, EntityDefinition departmentDefinition,
                                               String conditionString, int valueCount) {
    assertEquals(conditionString, criteria.toString(departmentDefinition));
    assertEquals(valueCount, criteria.values().size());
    assertEquals(valueCount, criteria.columns().size());
    assertEquals("DEPT", criteria.values().get(0));
    assertEquals(Department.NAME, criteria.columns().get(0));
  }

  @Test
  void equals() {
    Criteria criteria1 = all(Department.TYPE);
    Criteria criteria2 = all(Department.TYPE);
    assertEquals(criteria1, criteria2);
    criteria2 = all(Employee.TYPE);
    assertNotEquals(criteria1, criteria2);

    Key key1 = ENTITIES.primaryKey(Employee.TYPE, 1);
    Key key2 = ENTITIES.primaryKey(Employee.TYPE, 2);
    criteria1 = key(key1);
    criteria2 = key(key1);
    assertEquals(criteria1, criteria2);
    criteria2 = key(key2);
    assertNotEquals(criteria1, criteria2);

    criteria1 = foreignKey(Employee.DEPARTMENT_FK).isNull();
    criteria2 = foreignKey(Employee.DEPARTMENT_FK).isNull();
    assertEquals(criteria1, criteria2);
    criteria2 = foreignKey(Employee.DEPARTMENT_FK).isNotNull();
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).equalTo(0);
    criteria2 = column(Employee.ID).equalTo(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).equalTo(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.NAME).equalTo("Luke");
    criteria2 = column(Employee.NAME).equalTo("Luke");
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.NAME).equalToIgnoreCase("Luke");
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).notEqualTo(0);
    criteria2 = column(Employee.ID).notEqualTo(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).equalTo(0);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).notEqualTo(0);
    criteria2 = column(Employee.ID).notEqualTo(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).notEqualTo(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).lessThan(0);
    criteria2 = column(Employee.ID).lessThan(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).lessThan(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).lessThanOrEqualTo(0);
    criteria2 = column(Employee.ID).lessThanOrEqualTo(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).lessThanOrEqualTo(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).greaterThan(0);
    criteria2 = column(Employee.ID).greaterThan(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).greaterThan(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).greaterThanOrEqualTo(0);
    criteria2 = column(Employee.ID).greaterThanOrEqualTo(0);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).greaterThanOrEqualTo(1);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).between(0, 1);
    criteria2 = column(Employee.ID).between(0, 1);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).between(1, 0);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).betweenExclusive(0, 1);
    criteria2 = column(Employee.ID).betweenExclusive(0, 1);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).betweenExclusive(1, 0);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).notBetween(0, 1);
    criteria2 = column(Employee.ID).notBetween(0, 1);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).notBetween(1, 0);
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.ID).notBetweenExclusive(0, 1);
    criteria2 = column(Employee.ID).notBetweenExclusive(0, 1);
    assertEquals(criteria1, criteria2);
    criteria2 = column(Employee.ID).notBetweenExclusive(1, 0);
    assertNotEquals(criteria1, criteria2);

    criteria1 = customCriteria(Department.CRITERIA,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    criteria2 = customCriteria(Department.CRITERIA,
            Collections.singletonList(Department.NAME), Collections.singletonList("Test"));
    assertEquals(criteria1, criteria2);

    criteria1 = or(column(Employee.ID).equalTo(0),
            column(Employee.ID).equalTo(1));
    criteria2 = or(column(Employee.ID).equalTo(0),
            column(Employee.ID).equalTo(1));
    assertEquals(criteria1, criteria2);
    criteria2 = or(column(Employee.ID).equalTo(1),
            column(Employee.ID).equalTo(0));
    assertNotEquals(criteria1, criteria2);

    criteria1 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Luke"));
    criteria2 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Luke"));
    assertEquals(criteria1, criteria2);
    criteria2 = or(column(Employee.ID).equalTo(0),
            column(Employee.NAME).equalTo("Lukas"));
    assertNotEquals(criteria1, criteria2);

    criteria1 = column(Employee.NAME).equalTo("Luke");
    criteria2 = criteria1;
    assertEquals(criteria1, criteria2);

    criteria2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(criteria1, criteria2);
    assertNotEquals(criteria2, criteria1);

    criteria1 = column(Employee.NAME).lessThanOrEqualTo("Luke");
    criteria2 = criteria1;
    assertEquals(criteria1, criteria2);

    criteria2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(criteria1, criteria2);
    assertNotEquals(criteria2, criteria1);

    criteria1 = column(Employee.NAME).betweenExclusive("John", "Luke");
    criteria2 = criteria1;
    assertEquals(criteria1, criteria2);

    criteria2 = column(Employee.NAME).greaterThanOrEqualTo("Luke");
    assertNotEquals(criteria1, criteria2);
    assertNotEquals(criteria2, criteria1);

    criteria1 = column(Employee.NAME).notBetweenExclusive("John", "Luke");
    criteria2 = criteria1;
    assertEquals(criteria1, criteria2);

    criteria2 = column(Employee.NAME).lessThanOrEqualTo("Luke");
    assertNotEquals(criteria1, criteria2);
    assertNotEquals(criteria2, criteria1);
  }
}
