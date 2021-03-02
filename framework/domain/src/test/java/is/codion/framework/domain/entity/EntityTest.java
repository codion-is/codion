/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
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
  public void equal() {
    final Entity department1 = entities.entity(TestDomain.Department.TYPE);
    department1.put(TestDomain.Department.NO, 1);
    department1.put(TestDomain.Department.NAME, "name");
    department1.put(TestDomain.Department.LOCATION, "loc");

    final Entity department2 = entities.entity(TestDomain.Department.TYPE);
    department2.put(TestDomain.Department.NO, 2);
    department2.put(TestDomain.Department.NAME, "name");
    department2.put(TestDomain.Department.LOCATION, "loc");

    assertFalse(Entity.valuesEqual(department1, department2,
            TestDomain.Department.NO, TestDomain.Department.NAME, TestDomain.Department.LOCATION));
    assertTrue(Entity.valuesEqual(department1, department2,
            TestDomain.Department.NAME, TestDomain.Department.LOCATION));
    department2.remove(TestDomain.Department.LOCATION);
    assertFalse(Entity.valuesEqual(department1, department2,
            TestDomain.Department.NAME, TestDomain.Department.LOCATION));
    department1.remove(TestDomain.Department.LOCATION);
    assertTrue(Entity.valuesEqual(department1, department2,
            TestDomain.Department.NAME, TestDomain.Department.LOCATION));

    final Entity employee = entities.entity(TestDomain.Employee.TYPE);
    employee.put(TestDomain.Employee.ID, 1);
    employee.put(TestDomain.Employee.NAME, "name");

    assertThrows(IllegalArgumentException.class, () -> Entity.valuesEqual(department1, employee));
  }

  @Test
  public void isKeyModified() {
    assertFalse(Entity.isKeyModified(emptyList()));

    final Entity department = entities.entity(TestDomain.Department.TYPE);
    department.put(TestDomain.Department.NO, 1);
    department.put(TestDomain.Department.NAME, "name");
    department.put(TestDomain.Department.LOCATION, "loc");
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(TestDomain.Department.NAME, "new name");
    assertFalse(Entity.isKeyModified(singletonList(department)));

    department.put(TestDomain.Department.NO, 2);
    assertTrue(Entity.isKeyModified(singletonList(department)));

    department.revert(TestDomain.Department.NO);
    assertFalse(Entity.isKeyModified(singletonList(department)));
  }

  @Test
  public void getModifiedColumnAttributes() {
    final Entity entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 1);
    entity.put(TestDomain.Department.LOCATION, "Location");
    entity.put(TestDomain.Department.NAME, "Name");
    entity.put(TestDomain.Department.ACTIVE, true);

    final EntityDefinition definition = entities.getDefinition(TestDomain.Department.TYPE);

    final Entity current = entities.entity(TestDomain.Department.TYPE);
    current.put(TestDomain.Department.NO, 1);
    current.put(TestDomain.Department.LOCATION, "Location");
    current.put(TestDomain.Department.NAME, "Name");

    assertFalse(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.NO));
    assertFalse(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.LOCATION));
    assertFalse(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.NAME));

    current.put(TestDomain.Department.NO, 2);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.NO));
    assertEquals(TestDomain.Department.NO, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    final Integer id = current.remove(TestDomain.Department.NO);
    assertEquals(2, id);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.NO));
    assertEquals(TestDomain.Department.NO, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.put(TestDomain.Department.NO, 1);
    current.saveAll();
    assertFalse(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.NO));
    assertTrue(Entity.getModifiedColumnAttributes(definition, current, entity).isEmpty());

    current.put(TestDomain.Department.LOCATION, "New location");
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.LOCATION));
    assertEquals(TestDomain.Department.LOCATION, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.remove(TestDomain.Department.LOCATION);
    current.saveAll();
    assertTrue(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.LOCATION));
    assertEquals(TestDomain.Department.LOCATION, Entity.getModifiedColumnAttributes(definition, current, entity).iterator().next());
    current.put(TestDomain.Department.LOCATION, "Location");
    current.saveAll();
    assertFalse(Entity.isValueMissingOrModified(current, entity, TestDomain.Department.LOCATION));
    assertTrue(Entity.getModifiedColumnAttributes(definition, current, entity).isEmpty());

    entity.put(TestDomain.Department.LOCATION, "new loc");
    entity.put(TestDomain.Department.NAME, "new name");

    assertEquals(2, Entity.getModifiedColumnAttributes(definition, current, entity).size());
  }

  @Test
  public void getModifiedColumnAttributesWithBlob() {
    final Random random = new Random();
    final byte[] bytes = new byte[1024];
    random.nextBytes(bytes);
    final byte[] modifiedBytes = new byte[1024];
    random.nextBytes(modifiedBytes);

    final EntityDefinition definition = entities.getDefinition(TestDomain.Employee.TYPE);
    //eagerly loaded blob
    final Entity emp1 = entities.entity(TestDomain.Employee.TYPE);
    emp1.put(TestDomain.Employee.ID, 1);
    emp1.put(TestDomain.Employee.NAME, "name");
    emp1.put(TestDomain.Employee.SALARY, 1300d);
    emp1.put(TestDomain.Employee.DATA, bytes);

    final Entity emp2 = entities.copyEntity(emp1);
    emp2.put(TestDomain.Employee.DATA, modifiedBytes);

    List<Attribute<?>> modifiedAttributes = Entity.getModifiedColumnAttributes(definition, emp1, emp2);
    assertTrue(modifiedAttributes.contains(TestDomain.Employee.DATA));

    //lazy loaded blob
    final Entity dept1 = entities.entity(TestDomain.Department.TYPE);
    dept1.put(TestDomain.Department.NAME, "name");
    dept1.put(TestDomain.Department.LOCATION, "loc");
    dept1.put(TestDomain.Department.ACTIVE, true);
    dept1.put(TestDomain.Department.DATA, bytes);

    final Entity dept2 = entities.copyEntity(dept1);
    dept2.put(TestDomain.Department.DATA, modifiedBytes);

    final EntityDefinition departmentDefinition = entities.getDefinition(TestDomain.Department.TYPE);

    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertFalse(modifiedAttributes.contains(TestDomain.Department.DATA));

    dept2.put(TestDomain.Department.LOCATION, "new loc");
    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertTrue(modifiedAttributes.contains(TestDomain.Department.LOCATION));

    dept2.remove(TestDomain.Department.DATA);
    modifiedAttributes = Entity.getModifiedColumnAttributes(departmentDefinition, dept1, dept2);
    assertFalse(modifiedAttributes.contains(TestDomain.Department.DATA));
  }

  @Test
  public void get() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = entities.entity(TestDomain.Department.TYPE);
      entity.put(TestDomain.Department.NO, i);
      values.add(i);
      entityList.add(entity);
    }
    Collection<Integer> propertyValues = Entity.get(TestDomain.Department.NO, entityList);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = Entity.get(TestDomain.Department.NO, entityList);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(Entity.get(TestDomain.Department.NO, emptyList()).isEmpty());
  }

  @Test
  public void getDistinct() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    Entity entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, null);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 1);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 1);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 2);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 3);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 3);
    entityList.add(entity);

    entity = entities.entity(TestDomain.Department.TYPE);
    entity.put(TestDomain.Department.NO, 4);
    entityList.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> propertyValues = Entity.getDistinct(TestDomain.Department.NO, entityList);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = Entity.getDistinctIncludingNull(TestDomain.Department.NO, entityList);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, Entity.getDistinctIncludingNull(TestDomain.Department.NO, new ArrayList<>()).size());
  }

  @Test
  public void getStringValueList() {
    final Entity dept1 = entities.entity(TestDomain.Department.TYPE);
    dept1.put(TestDomain.Department.NO, 1);
    dept1.put(TestDomain.Department.NAME, "name1");
    dept1.put(TestDomain.Department.LOCATION, "loc1");
    final Entity dept2 = entities.entity(TestDomain.Department.TYPE);
    dept2.put(TestDomain.Department.NO, 2);
    dept2.put(TestDomain.Department.NAME, "name2");
    dept2.put(TestDomain.Department.LOCATION, "loc2");

    final List<Attribute<?>> attributes = entities.getDefinition(TestDomain.Department.TYPE)
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
  public void testSetPropertyValue() {
    final Collection<Entity> collection = new ArrayList<>();
    collection.add(entities.entity(TestDomain.Department.TYPE));
    collection.add(entities.entity(TestDomain.Department.TYPE));
    collection.add(entities.entity(TestDomain.Department.TYPE));
    collection.add(entities.entity(TestDomain.Department.TYPE));
    collection.add(entities.entity(TestDomain.Department.TYPE));
    collection.add(entities.entity(TestDomain.Department.TYPE));
    Entity.put(TestDomain.Department.NO, 1, collection);
    for (final Entity entity : collection) {
      assertEquals(Integer.valueOf(1), entity.get(TestDomain.Department.NO));
    }
    Entity.put(TestDomain.Department.NO, null, collection);
    for (final Entity entity : collection) {
      assertTrue(entity.isNull(TestDomain.Department.NO));
    }
  }

  @Test
  public void mapToPropertyValue() {
    final List<Entity> entityList = new ArrayList<>();

    final Entity entityOne = entities.entity(TestDomain.Department.TYPE);
    entityOne.put(TestDomain.Department.NO, 1);
    entityList.add(entityOne);

    final Entity entityTwo = entities.entity(TestDomain.Department.TYPE);
    entityTwo.put(TestDomain.Department.NO, 1);
    entityList.add(entityTwo);

    final Entity entityThree = entities.entity(TestDomain.Department.TYPE);
    entityThree.put(TestDomain.Department.NO, 2);
    entityList.add(entityThree);

    final Entity entityFour = entities.entity(TestDomain.Department.TYPE);
    entityFour.put(TestDomain.Department.NO, 3);
    entityList.add(entityFour);

    final Entity entityFive = entities.entity(TestDomain.Department.TYPE);
    entityFive.put(TestDomain.Department.NO, 3);
    entityList.add(entityFive);

    final Map<Integer, List<Entity>> map = Entity.mapToValue(TestDomain.Department.NO, entityList);
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
  public void mapToType() {
    final Entity one = entities.entity(TestDomain.Employee.TYPE);
    final Entity two = entities.entity(TestDomain.Department.TYPE);
    final Entity three = entities.entity(TestDomain.Detail.TYPE);
    final Entity four = entities.entity(TestDomain.Employee.TYPE);

    final Collection<Entity> entities = asList(one, two, three, four);
    final Map<EntityType<?>, List<Entity>> map = Entity.mapToType(entities);

    Collection<Entity> mapped = map.get(TestDomain.Employee.TYPE);
    assertTrue(mapped.contains(one));
    assertTrue(mapped.contains(four));

    mapped = map.get(TestDomain.Department.TYPE);
    assertTrue(mapped.contains(two));

    mapped = map.get(TestDomain.Detail.TYPE);
    assertTrue(mapped.contains(three));
  }

  @Test
  public void putNull() {
    final Entity dept = entities.entity(TestDomain.Department.TYPE);
    for (final Property<?> property : entities.getDefinition(TestDomain.Department.TYPE).getProperties()) {
      assertFalse(dept.containsValue(property.getAttribute()));
      assertTrue(dept.isNull(property.getAttribute()));
      assertFalse(dept.isNotNull(property.getAttribute()));
    }
    for (final Property<?> property : entities.getDefinition(TestDomain.Department.TYPE).getProperties()) {
      dept.put(property.getAttribute(), null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (final Property<?> property : entities.getDefinition(TestDomain.Department.TYPE).getProperties()) {
      assertTrue(dept.containsValue(property.getAttribute()));
      assertTrue(dept.isNull(property.getAttribute()));
      assertFalse(dept.isNotNull(property.getAttribute()));
    }
  }

  @Test
  public void getByValue() {
    final Entity one = entities.entity(TestDomain.Detail.TYPE);
    one.put(TestDomain.Detail.ID, 1L);
    one.put(TestDomain.Detail.STRING, "b");

    final Entity two = entities.entity(TestDomain.Detail.TYPE);
    two.put(TestDomain.Detail.ID, 2L);
    two.put(TestDomain.Detail.STRING, "zz");

    final Entity three = entities.entity(TestDomain.Detail.TYPE);
    three.put(TestDomain.Detail.ID, 3L);
    three.put(TestDomain.Detail.STRING, "zz");

    final List<Entity> entities = asList(one, two, three);

    final Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(TestDomain.Detail.STRING, "b");
    assertEquals(1, Entity.getByValue(entities, values).size());
    values.put(TestDomain.Detail.STRING, "zz");
    assertEquals(2, Entity.getByValue(entities, values).size());
    values.put(TestDomain.Detail.ID, 3L);
    assertEquals(1, Entity.getByValue(entities, values).size());
  }

  @Test
  public void getReferencedKeys() {
    final Entity dept1 = entities.entity(TestDomain.Department.TYPE);
    dept1.put(TestDomain.Department.NO, 1);
    final Entity dept2 = entities.entity(TestDomain.Department.TYPE);
    dept2.put(TestDomain.Department.NO, 2);

    final Entity emp1 = entities.entity(TestDomain.Employee.TYPE);
    emp1.put(TestDomain.Employee.DEPARTMENT_FK, dept1);
    final Entity emp2 = entities.entity(TestDomain.Employee.TYPE);
    emp2.put(TestDomain.Employee.DEPARTMENT_FK, dept1);
    final Entity emp3 = entities.entity(TestDomain.Employee.TYPE);
    emp3.put(TestDomain.Employee.DEPARTMENT_FK, dept2);
    final Entity emp4 = entities.entity(TestDomain.Employee.TYPE);

    final Set<Key> referencedKeys = Entity.getReferencedKeys(asList(emp1, emp2, emp3, emp4),
            TestDomain.Employee.DEPARTMENT_FK);
    assertEquals(2, referencedKeys.size());
    referencedKeys.forEach(key -> assertEquals(TestDomain.Department.TYPE, key.getEntityType()));
    final List<Integer> values = Entity.getValues(new ArrayList<>(referencedKeys));
    assertTrue(values.contains(1));
    assertTrue(values.contains(2));
    assertFalse(values.contains(3));
  }

  @Test
  public void noPkEntity() {
    final Entity noPk = entities.entity(TestDomain.T_NO_PK);
    noPk.put(TestDomain.NO_PK_COL1, 1);
    noPk.put(TestDomain.NO_PK_COL2, 2);
    noPk.put(TestDomain.NO_PK_COL3, 3);
    final List<Key> keys = Entity.getPrimaryKeys(singletonList(noPk));
    assertTrue(keys.get(0).isNull());
  }
}
