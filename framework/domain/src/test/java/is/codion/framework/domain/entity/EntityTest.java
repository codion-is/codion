/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityTest {

  private final Entities entities = new TestDomain().entities();

  @Test
  void equal() {
    Entity department1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();

    Entity department2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();

    assertFalse(Entity.valuesEqual(department1, department2,
            Department.ID, Department.NAME, Department.LOCATION));
    assertTrue(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));
    department2.remove(Department.LOCATION);
    assertFalse(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));
    department1.remove(Department.LOCATION);
    assertTrue(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));

    Entity employee = entities.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "name")
            .build();

    assertThrows(IllegalArgumentException.class, () -> Entity.valuesEqual(department1, employee));
  }

  @Test
  void isKeyModified() {
    assertFalse(Entity.isKeyModified(emptyList()));

    Entity department = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(Department.NAME, "new name");
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(Department.ID, 2);
    assertTrue(Entity.isKeyModified(singletonList(department)));

    department.revert(Department.ID);
    assertFalse(Entity.isKeyModified(singletonList(department)));
  }

  @Test
  void modifiedColumnAttributes() {
    Entity entity = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.LOCATION, "Location")
            .with(Department.NAME, "Name")
            .with(Department.ACTIVE, true)
            .build();

    Entity current = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.LOCATION, "Location")
            .with(Department.NAME, "Name")
            .build();

    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.ID));
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.NAME));

    current.put(Department.ID, 2);
    current.save();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.ID));
    assertEquals(Department.ID, Entity.modifiedColumnAttributes(current, entity).iterator().next());
    Integer id = current.remove(Department.ID);
    assertEquals(2, id);
    current.save();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.ID));
    assertEquals(Department.ID, Entity.modifiedColumnAttributes(current, entity).iterator().next());
    current.put(Department.ID, 1);
    current.save();
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.ID));
    assertTrue(Entity.modifiedColumnAttributes(current, entity).isEmpty());

    current.put(Department.LOCATION, "New location");
    current.save();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertEquals(Department.LOCATION, Entity.modifiedColumnAttributes(current, entity).iterator().next());
    current.remove(Department.LOCATION);
    current.save();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertEquals(Department.LOCATION, Entity.modifiedColumnAttributes(current, entity).iterator().next());
    current.put(Department.LOCATION, "Location");
    current.save();
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertTrue(Entity.modifiedColumnAttributes(current, entity).isEmpty());

    entity.put(Department.LOCATION, "new loc");
    entity.put(Department.NAME, "new name");

    assertEquals(2, Entity.modifiedColumnAttributes(current, entity).size());
  }

  @Test
  void modifiedColumnAttributesWithBlob() {
    Random random = new Random();
    byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    byte[] modifiedBytes = new byte[1024];
    random.nextBytes(modifiedBytes);

    //eagerly loaded blob
    Entity emp1 = entities.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "name")
            .with(Employee.SALARY, 1300d)
            .with(Employee.DATA, bytes)
            .build();

    Entity emp2 = emp1.copyBuilder()
            .with(Employee.DATA, modifiedBytes)
            .build();

    Collection<Attribute<?>> modifiedAttributes = Entity.modifiedColumnAttributes(emp1, emp2);
    assertTrue(modifiedAttributes.contains(Employee.DATA));

    //lazy loaded blob
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .with(Department.ACTIVE, true)
            .with(Department.DATA, bytes)
            .build();

    Entity dept2 = dept1.copyBuilder()
            .with(Department.DATA, modifiedBytes)
            .build();

    modifiedAttributes = Entity.modifiedColumnAttributes(dept1, dept2);
    assertFalse(modifiedAttributes.contains(Department.DATA));

    dept2.put(Department.LOCATION, "new loc");
    modifiedAttributes = Entity.modifiedColumnAttributes(dept1, dept2);
    assertTrue(modifiedAttributes.contains(Department.LOCATION));

    dept2.remove(Department.DATA);
    modifiedAttributes = Entity.modifiedColumnAttributes(dept1, dept2);
    assertFalse(modifiedAttributes.contains(Department.DATA));
  }

  @Test
  void values() {
    List<Entity> entityList = new ArrayList<>();
    List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      entityList.add(entities.builder(Department.TYPE)
              .with(Department.ID, i == 5 ? null : i)
              .build());
      if (i != 5) {
        values.add(i);
      }
    }
    Collection<Integer> attributeValues = Entity.values(Department.ID, entityList);
    assertTrue(attributeValues.containsAll(values));
    assertTrue(Entity.values(Department.ID, emptyList()).isEmpty());

    values.add(null);
    attributeValues = Entity.valuesIncludingNull(Department.ID, entityList);
    assertTrue(attributeValues.containsAll(values));
  }

  @Test
  void distinct() {
    List<Entity> entityList = new ArrayList<>();
    List<Object> values = new ArrayList<>();

    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, null)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 3)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 3)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.ID, 4)
            .build());

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> attributeValues = Entity.distinct(Department.ID, entityList);
    assertEquals(4, attributeValues.size());
    assertTrue(attributeValues.containsAll(values));

    attributeValues = Entity.distinctIncludingNull(Department.ID, entityList);
    assertEquals(5, attributeValues.size());
    values.add(null);
    assertTrue(attributeValues.containsAll(values));

    assertEquals(0, Entity.distinctIncludingNull(Department.ID, new ArrayList<>()).size());
  }

  @Test
  void modified() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "name1")
            .with(Department.LOCATION, "loc1")
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .with(Department.NAME, "name2")
            .with(Department.LOCATION, "loc2")
            .build();
    dept2.put(Department.NAME, "newname");

    assertTrue(Entity.modified(asList(dept1, dept2)).contains(dept2));
  }

  @Test
  void originalPrimaryKeys() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build();
    dept1.put(Department.ID, 3);
    dept2.put(Department.ID, 4);

    Collection<Entity.Key> originalPrimaryKeys = Entity.originalPrimaryKeys(asList(dept1, dept2));
    assertTrue(originalPrimaryKeys.contains(entities.primaryKey(Department.TYPE, 1)));
    assertTrue(originalPrimaryKeys.contains(entities.primaryKey(Department.TYPE, 2)));
  }

  @Test
  void immutable() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build();
    List<Entity> entityList = asList(dept1, dept2);
    Collection<Entity> immutables = Entity.immutable(entityList);
    entityList.forEach(entity -> assertTrue(immutables.contains(entity)));
    immutables.forEach(entity -> assertTrue(entity.isImmutable()));
  }

  @Test
  void copy() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build();
    List<Entity> entityList = asList(dept1, dept2);
    Collection<Entity> copied = Entity.copy(entityList);
    entityList.forEach(entity -> assertTrue(copied.contains(entity)));
  }

  @Test
  void mapToPrimaryKey() {
    Entity dept = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    Entity emp = entities.builder(Employee.TYPE)
            .with(Employee.ID, 3)
            .build();
    Map<Entity.Key, Entity> entityMap = Entity.mapToPrimaryKey(asList(dept, emp));
    assertEquals(dept, entityMap.get(dept.primaryKey()));
    assertEquals(emp, entityMap.get(emp.primaryKey()));

    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    assertThrows(IllegalStateException.class, () -> Entity.mapToPrimaryKey(asList(dept, dept2, emp)));
  }

  @Test
  void mapKeysToType() {
    Entity.Key dept = entities.primaryKey(Department.TYPE, 1);
    Entity.Key emp = entities.primaryKey(Employee.TYPE, 3);

    LinkedHashMap<EntityType, List<Entity.Key>> mapped = Entity.mapKeysToType(asList(dept, emp));
    assertEquals(dept, mapped.get(Department.TYPE).get(0));
    assertEquals(emp, mapped.get(Employee.TYPE).get(0));
  }

  @Test
  void valuesAsString() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "name1")
            .with(Department.LOCATION, "loc1")
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .with(Department.NAME, "name2")
            .with(Department.LOCATION, "loc2")
            .build();

    List<Attribute<?>> attributes = entities.definition(Department.TYPE)
            .columnDefinitions().stream().map(AttributeDefinition::attribute).collect(Collectors.toList());

    List<List<String>> strings =
            Entity.valuesAsString(attributes, asList(dept1, dept2));
    assertEquals("1", strings.get(0).get(0));
    assertEquals("name1", strings.get(0).get(1));
    assertEquals("loc1", strings.get(0).get(2));
    assertEquals("2", strings.get(1).get(0));
    assertEquals("name2", strings.get(1).get(1));
    assertEquals("loc2", strings.get(1).get(2));
  }

  @Test
  void testSetAttributeValue() {
    Collection<Entity> collection = new ArrayList<>();
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    Entity.put(Department.ID, 1, collection);
    for (Entity entity : collection) {
      assertEquals(Integer.valueOf(1), entity.get(Department.ID));
    }
    Entity.put(Department.ID, null, collection);
    for (Entity entity : collection) {
      assertTrue(entity.isNull(Department.ID));
    }
  }

  @Test
  void mapToAttributeValue() {
    List<Entity> entityList = new ArrayList<>();

    Entity entityOne = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    entityList.add(entityOne);

    Entity entityTwo = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    entityList.add(entityTwo);

    Entity entityThree = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build();
    entityList.add(entityThree);

    Entity entityFour = entities.builder(Department.TYPE)
            .with(Department.ID, 3)
            .build();
    entityList.add(entityFour);

    Entity entityFive = entities.builder(Department.TYPE)
            .with(Department.ID, 3)
            .build();
    entityList.add(entityFive);

    Map<Integer, List<Entity>> map = Entity.mapToValue(Department.ID, entityList);
    Collection<Entity> ones = map.get(1);
    assertTrue(ones.contains(entityOne));
    assertTrue(ones.contains(entityTwo));

    Collection<Entity> twos = map.get(2);
    assertTrue(twos.contains(entityThree));

    Collection<Entity> threes = map.get(3);
    assertTrue(threes.contains(entityFour));
    assertTrue(threes.contains(entityFive));
  }

  @Test
  void mapToType() {
    Entity one = entities.entity(Employee.TYPE);
    Entity two = entities.entity(Department.TYPE);
    Entity three = entities.entity(Detail.TYPE);
    Entity four = entities.entity(Employee.TYPE);

    Collection<Entity> entities = asList(one, two, three, four);
    Map<EntityType, List<Entity>> map = Entity.mapToType(entities);

    Collection<Entity> mapped = map.get(Employee.TYPE);
    assertTrue(mapped.contains(one));
    assertTrue(mapped.contains(four));

    mapped = map.get(Department.TYPE);
    assertTrue(mapped.contains(two));

    mapped = map.get(Detail.TYPE);
    assertTrue(mapped.contains(three));
  }

  @Test
  void putNull() {
    Entity dept = entities.entity(Department.TYPE);
    for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributeDefinitions()) {
      assertFalse(dept.contains(definition.attribute()));
      assertTrue(dept.isNull(definition.attribute()));
      assertFalse(dept.isNotNull(definition.attribute()));
    }
    for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributeDefinitions()) {
      dept.put(definition.attribute(), null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributeDefinitions()) {
      assertTrue(dept.contains(definition.attribute()));
      assertTrue(dept.isNull(definition.attribute()));
      assertFalse(dept.isNotNull(definition.attribute()));
    }
  }

  @Test
  void entitiesByValue() {
    Entity one = entities.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .with(Detail.STRING, "b")
            .build();

    Entity two = entities.builder(Detail.TYPE)
            .with(Detail.ID, 2L)
            .with(Detail.STRING, "zz")
            .build();

    Entity three = entities.builder(Detail.TYPE)
            .with(Detail.ID, 3L)
            .with(Detail.STRING, "zz")
            .build();

    List<Entity> entities = asList(one, two, three);

    Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(Detail.STRING, "b");
    assertEquals(1, Entity.entitiesByValue(values, entities).size());
    values.put(Detail.STRING, "zz");
    assertEquals(2, Entity.entitiesByValue(values, entities).size());
    values.put(Detail.ID, 3L);
    assertEquals(1, Entity.entitiesByValue(values, entities).size());
  }

  @Test
  void referencedKeys() {
    Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.ID, 1)
            .build();
    Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.ID, 2)
            .build();

    Entity emp1 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept1)
            .build();
    Entity emp2 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept1)
            .build();
    Entity emp3 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept2)
            .build();
    Entity emp4 = entities.builder(Employee.TYPE)
            .build();

    Collection<Entity.Key> referencedKeys = Entity.referencedKeys(Employee.DEPARTMENT_FK, asList(emp1, emp2, emp3, emp4));
    assertEquals(2, referencedKeys.size());
    referencedKeys.forEach(key -> assertEquals(Department.TYPE, key.entityType()));
    Collection<Integer> values = Entity.values(new ArrayList<>(referencedKeys));
    assertTrue(values.contains(1));
    assertTrue(values.contains(2));
    assertFalse(values.contains(3));
  }

  @Test
  void noPkEntity() {
    Entity noPk = entities.builder(NoPk.TYPE)
            .with(NoPk.COL1, 1)
            .with(NoPk.COL2, 2)
            .with(NoPk.COL3, 3)
            .build();
    Collection<Entity.Key> keys = Entity.primaryKeys(singletonList(noPk));
    assertTrue(keys.iterator().next().isNull());
  }
}
