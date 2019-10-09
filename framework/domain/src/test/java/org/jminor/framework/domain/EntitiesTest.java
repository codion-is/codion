/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_NAME));

    current.put(TestDomain.DEPARTMENT_ID, 2);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertEquals(Entities.getModifiedColumnProperties(current, entity, false).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_ID);
    final Integer id = (Integer) current.remove(TestDomain.DEPARTMENT_ID);
    assertEquals(2, id);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertEquals(Entities.getModifiedColumnProperties(current, entity, false).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_ID);
    current.put(TestDomain.DEPARTMENT_ID, 1);
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertTrue(Entities.getModifiedColumnProperties(current, entity, false).isEmpty());

    current.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(Entities.getModifiedColumnProperties(current, entity, false).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_LOCATION);
    current.remove(TestDomain.DEPARTMENT_LOCATION);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(Entities.getModifiedColumnProperties(current, entity, false).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_LOCATION);
    current.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertTrue(Entities.getModifiedColumnProperties(current, entity, false).isEmpty());

    entity.put(TestDomain.DEPARTMENT_LOCATION, "new loc");
    entity.put(TestDomain.DEPARTMENT_NAME, "new name");
    entity.put(TestDomain.DEPARTMENT_ACTIVE, false);

    assertEquals(Entities.getModifiedColumnProperties(current, entity, false).size(), 2);
    assertEquals(Entities.getModifiedColumnProperties(current, entity, true).size(), 3);
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
    final Property property = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    Collection<Integer> propertyValues = Entities.getValues(TestDomain.DEPARTMENT_ID, entityList);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = Entities.getValues(property.getPropertyId(), entityList);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(Entities.getValues(TestDomain.DEPARTMENT_ID, null).isEmpty());
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

    propertyValues = Entities.getDistinctValues(TestDomain.DEPARTMENT_ID, entityList, true);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, Entities.getDistinctValues(TestDomain.DEPARTMENT_ID, null, true).size());
    assertEquals(0, Entities.getDistinctValues(TestDomain.DEPARTMENT_ID, new ArrayList<>(), true).size());
  }

  @Test
  public void getStringValueArray() {
    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    dept1.put(TestDomain.DEPARTMENT_NAME, "name1");
    dept1.put(TestDomain.DEPARTMENT_LOCATION, "loc1");
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);
    dept2.put(TestDomain.DEPARTMENT_NAME, "name2");
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "loc2");

    final String[][] strings = Entities.getStringValueArray(domain.getColumnProperties(TestDomain.T_DEPARTMENT), asList(dept1, dept2));
    assertEquals("1", strings[0][0]);
    assertEquals("name1", strings[0][1]);
    assertEquals("loc1", strings[0][2]);
    assertEquals("2", strings[1][0]);
    assertEquals("name2", strings[1][1]);
    assertEquals("loc2", strings[1][2]);
  }

  @Test
  public void copyEntities() {
    final Entity dept1 = domain.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    dept1.put(TestDomain.DEPARTMENT_LOCATION, "location");
    dept1.put(TestDomain.DEPARTMENT_NAME, "name");
    final Entity dept2 = domain.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "location2");
    dept2.put(TestDomain.DEPARTMENT_NAME, "name2");

    final List<Entity> copies = Entities.copyEntities(asList(dept1, dept2));
    assertNotSame(copies.get(0), dept1);
    assertTrue(copies.get(0).valuesEqual(dept1));
    assertNotSame(copies.get(1), dept2);
    assertTrue(copies.get(1).valuesEqual(dept2));
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
      assertTrue(entity.isValueNull(TestDomain.DEPARTMENT_ID));
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
    for (final Property property : domain.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertFalse(dept.containsKey(property));
      assertTrue(dept.isValueNull(property));
    }
    for (final Property property : domain.getProperties(TestDomain.T_DEPARTMENT, true)) {
      dept.put(property, null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (final Property property : domain.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertTrue(dept.containsKey(property));
      assertTrue(dept.isValueNull(property));
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
}
