/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityTest {

  private final Entities entities = new TestDomain().getEntities();

  @Test
  void equal() {
    final Entity department1 = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();

    final Entity department2 = entities.builder(Department.TYPE)
            .with(Department.NO, 2)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();

    assertFalse(Entity.valuesEqual(department1, department2,
            Department.NO, Department.NAME, Department.LOCATION));
    assertTrue(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));
    department2.remove(Department.LOCATION);
    assertFalse(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));
    department1.remove(Department.LOCATION);
    assertTrue(Entity.valuesEqual(department1, department2,
            Department.NAME, Department.LOCATION));

    final Entity employee = entities.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "name")
            .build();

    assertThrows(IllegalArgumentException.class, () -> Entity.valuesEqual(department1, employee));
  }

  @Test
  void isKeyModified() {
    assertFalse(Entity.isKeyModified(emptyList()));

    final Entity department = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .build();
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(Department.NAME, "new name");
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(Department.NO, 2);
    assertTrue(Entity.isKeyModified(singletonList(department)));

    department.revert(Department.NO);
    assertFalse(Entity.isKeyModified(singletonList(department)));
  }

  @Test
  void getModifiedColumnAttributes() {
    final Entity entity = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.LOCATION, "Location")
            .with(Department.NAME, "Name")
            .with(Department.ACTIVE, true)
            .build();

    final EntityDefinition definition = entities.getDefinition(Department.TYPE);

    final Entity current = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.LOCATION, "Location")
            .with(Department.NAME, "Name")
            .build();

    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.NO));
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.NAME));

    current.put(Department.NO, 2);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.NO));
    assertEquals(Department.NO, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    final Integer id = current.remove(Department.NO);
    assertEquals(2, id);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.NO));
    assertEquals(Department.NO, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.put(Department.NO, 1);
    current.saveAll();
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.NO));
    assertTrue(Entity.getModifiedColumnAttributes(definition, current, entity).isEmpty());

    current.put(Department.LOCATION, "New location");
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertEquals(Department.LOCATION, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.remove(Department.LOCATION);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertEquals(Department.LOCATION, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.put(Department.LOCATION, "Location");
    current.saveAll();
    assertFalse(Entity.isValueMissingOrModified(current, entity, Department.LOCATION));
    assertTrue(Entity.getModifiedColumnAttributes(definition, current, entity).isEmpty());

    entity.put(Department.LOCATION, "new loc");
    entity.put(Department.NAME, "new name");

    assertEquals(2, Entity.getModifiedColumnAttributes(definition, current, entity).size());
  }

  @Test
  void getModifiedColumnAttributesWithBlob() {
    final Random random = new Random();
    final byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    final byte[] modifiedBytes = new byte[1024];
    random.nextBytes(modifiedBytes);

    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    //eagerly loaded blob
    final Entity emp1 = entities.builder(Employee.TYPE)
            .with(Employee.ID, 1)
            .with(Employee.NAME, "name")
            .with(Employee.SALARY, 1300d)
            .with(Employee.DATA, bytes)
            .build();

    final Entity emp2 = emp1.copyBuilder()
            .with(Employee.DATA, modifiedBytes)
            .build();

    List<Attribute<?>> modifiedAttributes = Entity.getModifiedColumnAttributes(definition, emp1, emp2);
    assertTrue(modifiedAttributes.contains(Employee.DATA));

    //lazy loaded blob
    final Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.NAME, "name")
            .with(Department.LOCATION, "loc")
            .with(Department.ACTIVE, true)
            .with(Department.DATA, bytes)
            .build();

    final Entity dept2 = dept1.copyBuilder()
            .with(Department.DATA, modifiedBytes)
            .build();

    final EntityDefinition departmentDefinition = entities.getDefinition(Department.TYPE);

    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertFalse(modifiedAttributes.contains(Department.DATA));

    dept2.put(Department.LOCATION, "new loc");
    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertTrue(modifiedAttributes.contains(Department.LOCATION));

    dept2.remove(Department.DATA);
    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertFalse(modifiedAttributes.contains(Department.DATA));
  }

  @Test
  void get() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      entityList.add(entities.builder(Department.TYPE)
              .with(Department.NO, i)
              .build());
      values.add(i);
    }
    Collection<Integer> propertyValues = Entity.get(Department.NO, entityList);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = Entity.get(Department.NO, entityList);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(Entity.get(Department.NO, emptyList()).isEmpty());
  }

  @Test
  void getDistinct() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, null)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 2)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 3)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 3)
            .build());
    entityList.add(entities.builder(Department.TYPE)
            .with(Department.NO, 4)
            .build());

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> propertyValues = Entity.getDistinct(Department.NO, entityList);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = Entity.getDistinctIncludingNull(Department.NO, entityList);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, Entity.getDistinctIncludingNull(Department.NO, new ArrayList<>()).size());
  }

  @Test
  void getStringValueList() {
    final Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.NAME, "name1")
            .with(Department.LOCATION, "loc1")
            .build();
    final Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.NO, 2)
            .with(Department.NAME, "name2")
            .with(Department.LOCATION, "loc2")
            .build();

    final List<Attribute<?>> attributes = entities.getDefinition(Department.TYPE)
            .getColumnProperties().stream().map(Property::getAttribute).collect(Collectors.toList());

    final List<List<String>> strings =
            Entity.getStringValueList(attributes, asList(dept1, dept2));
    assertEquals("1", strings.get(0).get(0));
    assertEquals("name1", strings.get(0).get(1));
    assertEquals("loc1", strings.get(0).get(2));
    assertEquals("2", strings.get(1).get(0));
    assertEquals("name2", strings.get(1).get(1));
    assertEquals("loc2", strings.get(1).get(2));
  }

  @Test
  void testSetPropertyValue() {
    final Collection<Entity> collection = new ArrayList<>();
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    collection.add(entities.entity(Department.TYPE));
    Entity.put(Department.NO, 1, collection);
    for (final Entity entity : collection) {
      assertEquals(Integer.valueOf(1), entity.get(Department.NO));
    }
    Entity.put(Department.NO, null, collection);
    for (final Entity entity : collection) {
      assertTrue(entity.isNull(Department.NO));
    }
  }

  @Test
  void mapToPropertyValue() {
    final List<Entity> entityList = new ArrayList<>();

    final Entity entityOne = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
                    .build();
    entityList.add(entityOne);

    final Entity entityTwo = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
                    .build();
    entityList.add(entityTwo);

    final Entity entityThree = entities.builder(Department.TYPE)
            .with(Department.NO, 2)
                    .build();
    entityList.add(entityThree);

    final Entity entityFour = entities.builder(Department.TYPE)
            .with(Department.NO, 3)
                    .build();
    entityList.add(entityFour);

    final Entity entityFive = entities.builder(Department.TYPE)
            .with(Department.NO, 3)
                    .build();
    entityList.add(entityFive);

    final Map<Integer, List<Entity>> map = Entity.mapToValue(Department.NO, entityList);
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
  void mapToType() {
    final Entity one = entities.entity(Employee.TYPE);
    final Entity two = entities.entity(Department.TYPE);
    final Entity three = entities.entity(Detail.TYPE);
    final Entity four = entities.entity(Employee.TYPE);

    final Collection<Entity> entities = asList(one, two, three, four);
    final Map<EntityType<?>, List<Entity>> map = Entity.mapToType(entities);

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
    final Entity dept = entities.entity(Department.TYPE);
    for (final Property<?> property : entities.getDefinition(Department.TYPE).getProperties()) {
      assertFalse(dept.contains(property.getAttribute()));
      assertTrue(dept.isNull(property.getAttribute()));
      assertFalse(dept.isNotNull(property.getAttribute()));
    }
    for (final Property<?> property : entities.getDefinition(Department.TYPE).getProperties()) {
      dept.put(property.getAttribute(), null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (final Property<?> property : entities.getDefinition(Department.TYPE).getProperties()) {
      assertTrue(dept.contains(property.getAttribute()));
      assertTrue(dept.isNull(property.getAttribute()));
      assertFalse(dept.isNotNull(property.getAttribute()));
    }
  }

  @Test
  void getByValue() {
    final Entity one = entities.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .with(Detail.STRING, "b")
            .build();

    final Entity two = entities.builder(Detail.TYPE)
            .with(Detail.ID, 2L)
            .with(Detail.STRING, "zz")
            .build();

    final Entity three = entities.builder(Detail.TYPE)
            .with(Detail.ID, 3L)
            .with(Detail.STRING, "zz")
            .build();

    final List<Entity> entities = asList(one, two, three);

    final Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(Detail.STRING, "b");
    assertEquals(1, Entity.getByValue(entities, values).size());
    values.put(Detail.STRING, "zz");
    assertEquals(2, Entity.getByValue(entities, values).size());
    values.put(Detail.ID, 3L);
    assertEquals(1, Entity.getByValue(entities, values).size());
  }

  @Test
  void getReferencedKeys() {
    final Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .build();
    final Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.NO, 2)
            .build();

    final Entity emp1 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept1)
            .build();
    final Entity emp2 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept1)
            .build();
    final Entity emp3 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept2)
            .build();
    final Entity emp4 = entities.builder(Employee.TYPE)
            .build();

    final Set<Key> referencedKeys = Entity.getReferencedKeys(asList(emp1, emp2, emp3, emp4),
            Employee.DEPARTMENT_FK);
    assertEquals(2, referencedKeys.size());
    referencedKeys.forEach(key -> assertEquals(Department.TYPE, key.getEntityType()));
    final List<Integer> values = Entity.getValues(new ArrayList<>(referencedKeys));
    assertTrue(values.contains(1));
    assertTrue(values.contains(2));
    assertFalse(values.contains(3));
  }

  @Test
  void noPkEntity() {
    final Entity noPk = entities.builder(NoPk.TYPE)
            .with(NoPk.COL1, 1)
            .with(NoPk.COL2, 2)
            .with(NoPk.COL3, 3)
            .build();
    final List<Key> keys = Entity.getPrimaryKeys(singletonList(noPk));
    assertTrue(keys.get(0).isNull());
  }
}
