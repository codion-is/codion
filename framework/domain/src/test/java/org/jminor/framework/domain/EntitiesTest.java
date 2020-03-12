/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntitiesTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void equal() {
    final Entity department1 = domain.entity(TestDomain.T_DEPARTMENT);
    department1.put(TestDomain.DEPARTMENT_ID, 1);
    department1.put(TestDomain.DEPARTMENT_NAME, "name");
    department1.put(TestDomain.DEPARTMENT_LOCATION, "loc");

    final Entity department2 = domain.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 2);
    department2.put(TestDomain.DEPARTMENT_NAME, "name");
    department2.put(TestDomain.DEPARTMENT_LOCATION, "loc");

    assertFalse(Entities.equal(department1, department2,
            TestDomain.DEPARTMENT_ID, TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION));
    assertTrue(Entities.equal(department1, department2,
            TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION));
    department2.remove(TestDomain.DEPARTMENT_LOCATION);
    assertFalse(Entities.equal(department1, department2,
            TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION));
    department1.remove(TestDomain.DEPARTMENT_LOCATION);
    assertTrue(Entities.equal(department1, department2,
            TestDomain.DEPARTMENT_NAME, TestDomain.DEPARTMENT_LOCATION));

    assertThrows(IllegalArgumentException.class, () -> Entities.equal(department1, department2));
  }

  @Test
  public void isKeyModified() {
    assertFalse(Entities.isKeyModified(null));
    assertFalse(Entities.isKeyModified(emptyList()));

    final Entity department = domain.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 1);
    department.put(TestDomain.DEPARTMENT_NAME, "name");
    department.put(TestDomain.DEPARTMENT_LOCATION, "loc");
    assertFalse(Entities.isKeyModified(singletonList(department)));

    department.put(TestDomain.DEPARTMENT_NAME, "new name");
    assertFalse(Entities.isKeyModified(singletonList(department)));

    department.put(TestDomain.DEPARTMENT_ID, 2);
    assertTrue(Entities.isKeyModified(singletonList(department)));

    department.revert(TestDomain.DEPARTMENT_ID);
    assertFalse(Entities.isKeyModified(singletonList(department)));
  }

  @Test
  public void getModifiedColumnProperties() {
    final Entity entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entity.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    entity.put(TestDomain.DEPARTMENT_NAME, "Name");
    entity.put(TestDomain.DEPARTMENT_ACTIVE, true);

    final Entity current = domain.entity(TestDomain.T_DEPARTMENT);
    current.put(TestDomain.DEPARTMENT_ID, 1);
    current.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.put(TestDomain.DEPARTMENT_NAME, "Name");

    final Property departmentId = domain.getDefinition(TestDomain.T_DEPARTMENT).getProperty(TestDomain.DEPARTMENT_ID);
    final Property departmentLocation = domain.getDefinition(TestDomain.T_DEPARTMENT).getProperty(TestDomain.DEPARTMENT_LOCATION);
    final Property departmentName = domain.getDefinition(TestDomain.T_DEPARTMENT).getProperty(TestDomain.DEPARTMENT_NAME);

    assertFalse(Entities.isValueMissingOrModified(current, entity, departmentId));
    assertFalse(Entities.isValueMissingOrModified(current, entity, departmentLocation));
    assertFalse(Entities.isValueMissingOrModified(current, entity, departmentName));

    current.put(departmentId, 2);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, departmentId));
    assertEquals(departmentId, Entities.getModifiedColumnProperties(current, entity).iterator().next());
    final Integer id = (Integer) current.remove(departmentId);
    assertEquals(2, id);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, departmentId));
    assertEquals(departmentId, Entities.getModifiedColumnProperties(current, entity).iterator().next());
    current.put(departmentId, 1);
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, departmentId));
    assertTrue(Entities.getModifiedColumnProperties(current, entity).isEmpty());

    current.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, departmentLocation));
    assertEquals(departmentLocation, Entities.getModifiedColumnProperties(current, entity).iterator().next());
    current.remove(TestDomain.DEPARTMENT_LOCATION);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, departmentLocation));
    assertEquals(departmentLocation, Entities.getModifiedColumnProperties(current, entity).iterator().next());
    current.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, departmentLocation));
    assertTrue(Entities.getModifiedColumnProperties(current, entity).isEmpty());

    entity.put(TestDomain.DEPARTMENT_LOCATION, "new loc");
    entity.put(TestDomain.DEPARTMENT_NAME, "new name");

    assertEquals(2, Entities.getModifiedColumnProperties(current, entity).size());
  }

  @Test
  public void getModifiedColumnPropertiesWithBlob() {
    final Random random = new Random();
    final byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    final byte[] modifiedBytes = new byte[1024];
    random.nextBytes(modifiedBytes);

    final Entity emp1 = domain.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_ID, 1);
    emp1.put(TestDomain.EMP_NAME, "name");
    emp1.put(TestDomain.EMP_SALARY, 1300d);
    emp1.put(TestDomain.EMP_DATA, bytes);

    final Entity emp2 = domain.copyEntity(emp1);
    emp2.put(TestDomain.EMP_DATA, modifiedBytes);

    final List<ColumnProperty> modifiedProperties = Entities.getModifiedColumnProperties(emp1, emp2);
    assertTrue(modifiedProperties.contains(domain.getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_DATA)));
  }

  @Test
  public void getPropertyValues() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = domain.entity(TestDomain.T_DEPARTMENT);
      entity.put(TestDomain.DEPARTMENT_ID, i);
      values.add(i);
      entityList.add(entity);
    }
    final Property property = domain.getDefinition(TestDomain.T_DEPARTMENT).getProperty(TestDomain.DEPARTMENT_ID);
    Collection<Integer> propertyValues = Entities.getValues(TestDomain.DEPARTMENT_ID, entityList);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = Entities.getValues(property.getPropertyId(), entityList);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(Entities.getValues(TestDomain.DEPARTMENT_ID, emptyList()).isEmpty());
  }

  @Test
  public void getDistinctPropertyValues() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    Entity entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, null);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 2);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entity);

    entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 4);
    entityList.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> propertyValues = Entities.getDistinctValues(TestDomain.DEPARTMENT_ID, entityList);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = Entities.getDistinctValuesIncludingNull(TestDomain.DEPARTMENT_ID, entityList);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, Entities.getDistinctValuesIncludingNull(TestDomain.DEPARTMENT_ID, new ArrayList<>()).size());
  }

  @Test
  public void getStringValueList() {
    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    dept1.put(TestDomain.DEPARTMENT_NAME, "name1");
    dept1.put(TestDomain.DEPARTMENT_LOCATION, "loc1");
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);
    dept2.put(TestDomain.DEPARTMENT_NAME, "name2");
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "loc2");

    final List<List<String>> strings =
            Entities.getStringValueList(domain.getDefinition(TestDomain.T_DEPARTMENT)
                    .getColumnProperties(), asList(dept1, dept2));
    assertEquals("1", strings.get(0).get(0));
    assertEquals("name1", strings.get(0).get(1));
    assertEquals("loc1", strings.get(0).get(2));
    assertEquals("2", strings.get(1).get(0));
    assertEquals("name2", strings.get(1).get(1));
    assertEquals("loc2", strings.get(1).get(2));
  }

  @Test
  public void testSetPropertyValue() {
    final Collection<Entity> collection = new ArrayList<>();
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    collection.add(domain.entity(TestDomain.T_DEPARTMENT));
    Entities.put(TestDomain.DEPARTMENT_ID, 1, collection);
    for (final Entity entity : collection) {
      assertEquals(Integer.valueOf(1), entity.getInteger(TestDomain.DEPARTMENT_ID));
    }
    Entities.put(TestDomain.DEPARTMENT_ID, null, collection);
    for (final Entity entity : collection) {
      assertTrue(entity.isNull(TestDomain.DEPARTMENT_ID));
    }
  }

  @Test
  public void mapToPropertyValue() {
    final List<Entity> entityList = new ArrayList<>();

    final Entity entityOne = domain.entity(TestDomain.T_DEPARTMENT);
    entityOne.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entityOne);

    final Entity entityTwo = domain.entity(TestDomain.T_DEPARTMENT);
    entityTwo.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entityTwo);

    final Entity entityThree = domain.entity(TestDomain.T_DEPARTMENT);
    entityThree.put(TestDomain.DEPARTMENT_ID, 2);
    entityList.add(entityThree);

    final Entity entityFour = domain.entity(TestDomain.T_DEPARTMENT);
    entityFour.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entityFour);

    final Entity entityFive = domain.entity(TestDomain.T_DEPARTMENT);
    entityFive.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entityFive);

    final Map<Integer, List<Entity>> map = Entities.mapToValue(TestDomain.DEPARTMENT_ID, entityList);
    final Collection<Entity> ones = map.get(1);
    assertTrue(ones.contains(entityOne));
    assertTrue(ones.contains(entityTwo));

    final Collection<Entity> twos = map.get(2);
    assertTrue(twos.contains(entityThree));

    final Collection<Entity> threes = map.get(3);
    assertTrue(threes.contains(entityFour));
    assertTrue(threes.contains(entityFive));
  }

  @Test
  public void mapToEntitId() {
    final Entity one = domain.entity(TestDomain.T_EMP);
    final Entity two = domain.entity(TestDomain.T_DEPARTMENT);
    final Entity three = domain.entity(TestDomain.T_DETAIL);
    final Entity four = domain.entity(TestDomain.T_EMP);

    final Collection<Entity> entities = asList(one, two, three, four);
    final Map<String, List<Entity>> map = Entities.mapToEntityId(entities);

    Collection<Entity> mapped = map.get(TestDomain.T_EMP);
    assertTrue(mapped.contains(one));
    assertTrue(mapped.contains(four));

    mapped = map.get(TestDomain.T_DEPARTMENT);
    assertTrue(mapped.contains(two));

    mapped = map.get(TestDomain.T_DETAIL);
    assertTrue(mapped.contains(three));
  }

  @Test
  public void putNull() {
    final Entity dept = domain.entity(TestDomain.T_DEPARTMENT);
    for (final Property property : domain.getDefinition(TestDomain.T_DEPARTMENT).getProperties()) {
      assertFalse(dept.containsKey(property));
      assertTrue(dept.isNull(property));
      assertFalse(dept.isNotNull(property));
    }
    for (final Property property : domain.getDefinition(TestDomain.T_DEPARTMENT).getProperties()) {
      dept.put(property, null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (final Property property : domain.getDefinition(TestDomain.T_DEPARTMENT).getProperties()) {
      assertTrue(dept.containsKey(property));
      assertTrue(dept.isNull(property));
      assertFalse(dept.isNotNull(property));
    }
  }

  @Test
  public void getEntitiesByValue() {
    final Entity one = domain.entity(TestDomain.T_DETAIL);
    one.put(TestDomain.DETAIL_ID, 1L);
    one.put(TestDomain.DETAIL_STRING, "b");

    final Entity two = domain.entity(TestDomain.T_DETAIL);
    two.put(TestDomain.DETAIL_ID, 2L);
    two.put(TestDomain.DETAIL_STRING, "zz");

    final Entity three = domain.entity(TestDomain.T_DETAIL);
    three.put(TestDomain.DETAIL_ID, 3L);
    three.put(TestDomain.DETAIL_STRING, "zz");

    final List<Entity> entities = asList(one, two, three);

    final Map<String, Object> values = new HashMap<>();
    values.put(TestDomain.DETAIL_STRING, "b");
    assertEquals(1, Entities.getEntitiesByValue(entities, values).size());
    values.put(TestDomain.DETAIL_STRING, "zz");
    assertEquals(2, Entities.getEntitiesByValue(entities, values).size());
    values.put(TestDomain.DETAIL_ID, 3L);
    assertEquals(1, Entities.getEntitiesByValue(entities, values).size());
  }

  @Test
  public void getReferencedKeys() {
    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);

    final Entity emp1 = domain.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept1);
    final Entity emp2 = domain.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept1);
    final Entity emp3 = domain.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept2);
    final Entity emp4 = domain.entity(TestDomain.T_EMP);

    final Set<Entity.Key> referencedKeys = Entities.getReferencedKeys(asList(emp1, emp2, emp3, emp4),
            domain.getDefinition(TestDomain.T_EMP).getForeignKeyProperty(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals(2, referencedKeys.size());
    referencedKeys.forEach(key -> assertEquals(TestDomain.T_DEPARTMENT, key.getEntityId()));
    final List<Integer> values = Entities.getValues(new ArrayList<>(referencedKeys));
    assertTrue(values.contains(1));
    assertTrue(values.contains(2));
    assertFalse(values.contains(3));
  }

  @Test
  public void noPkEntity() {
    final Entity noPk = domain.entity(TestDomain.T_NO_PK);
    noPk.put(TestDomain.NO_PK_COL1, 1);
    noPk.put(TestDomain.NO_PK_COL2, 2);
    noPk.put(TestDomain.NO_PK_COL3, 3);
    final List<Entity.Key> keys = Entities.getKeys(singletonList(noPk));
    assertEquals(0, keys.get(0).size());
  }
}
