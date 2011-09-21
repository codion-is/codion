/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityUtilTest {

  @Test
  public void getPropertyValues() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();
    final List<Object> values = new ArrayList<Object>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
      entity.setValue(EmpDept.DEPARTMENT_ID, i);
      values.add(i);
      entities.add(entity);
    }
    final Property property = Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID);
    List<Object> propertyValues = EntityUtil.getPropertyValues(EmpDept.DEPARTMENT_ID, entities);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = EntityUtil.getPropertyValues(property, entities);
    assertTrue(propertyValues.containsAll(values));
  }

  @Test
  public void getDistinctPropertyValues() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();
    final List<Object> values = new ArrayList<Object>();

    Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, null);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 2);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 4);
    entities.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Object> propertyValues = EntityUtil.getDistinctPropertyValues(EmpDept.DEPARTMENT_ID, entities);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = EntityUtil.getDistinctPropertyValues(EmpDept.DEPARTMENT_ID, entities, true);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));
  }

  @Test
  public void testSetPropertyValue() {
    EmpDept.init();
    final Collection<Entity> entities = new ArrayList<Entity>();
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    EntityUtil.setPropertyValue(EmpDept.DEPARTMENT_ID, 1, entities);
    for (final Entity entity : entities) {
      assertEquals(Integer.valueOf(1), entity.getIntValue(EmpDept.DEPARTMENT_ID));
    }
    EntityUtil.setPropertyValue(EmpDept.DEPARTMENT_ID, null, entities);
    for (final Entity entity : entities) {
      assertTrue(entity.isValueNull(EmpDept.DEPARTMENT_ID));
    }
  }

  @Test
  public void hashByPropertyValue() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();

    final Entity entityOne = Entities.entity(EmpDept.T_DEPARTMENT);
    entityOne.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entityOne);

    final Entity entityTwo = Entities.entity(EmpDept.T_DEPARTMENT);
    entityTwo.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entityTwo);

    final Entity entityThree = Entities.entity(EmpDept.T_DEPARTMENT);
    entityThree.setValue(EmpDept.DEPARTMENT_ID, 2);
    entities.add(entityThree);

    final Entity entityFour = Entities.entity(EmpDept.T_DEPARTMENT);
    entityFour.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entityFour);

    final Entity entityFive = Entities.entity(EmpDept.T_DEPARTMENT);
    entityFive.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entityFive);

    final Map<Object, Collection<Entity>> map = EntityUtil.hashByPropertyValue(EmpDept.DEPARTMENT_ID, entities);
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
  public void hashByEntitID() {
    Chinook.init();
    final Entity one = Entities.entity(Chinook.T_ALBUM);
    final Entity two = Entities.entity(Chinook.T_ARTIST);
    final Entity three = Entities.entity(Chinook.T_CUSTOMER);
    final Entity four = Entities.entity(Chinook.T_ALBUM);
    final Entity five = Entities.entity(Chinook.T_ARTIST);

    final Collection<Entity> entities = Arrays.asList(one, two, three, four, five);
    final Map<String, Collection<Entity>> map = EntityUtil.hashByEntityID(entities);

    Collection<Entity> hashed = map.get(Chinook.T_ALBUM);
    assertTrue(hashed.contains(one));
    assertTrue(hashed.contains(four));

    hashed = map.get(Chinook.T_ARTIST);
    assertTrue(hashed.contains(two));
    assertTrue(hashed.contains(five));

    hashed = map.get(Chinook.T_CUSTOMER);
    assertTrue(hashed.contains(three));
  }

  @Test
  public void getProperties() {
    EmpDept.init();
    final List<String> propertyIDs = new ArrayList<String>();
    propertyIDs.add(EmpDept.DEPARTMENT_ID);
    propertyIDs.add(EmpDept.DEPARTMENT_NAME);

    final Collection<Property> properties = EntityUtil.getProperties(EmpDept.T_DEPARTMENT, propertyIDs);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID)));
    assertTrue(properties.contains(Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME)));

    final Collection<Property> noProperties = EntityUtil.getProperties(EmpDept.T_DEPARTMENT, null);
    assertEquals(0, noProperties.size());
  }

  @Test(expected = RuntimeException.class)
  public void getEntitySerializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, null);
    EntityUtil.getEntitySerializer();
  }

  @Test(expected = RuntimeException.class)
  public void getEntityDeserializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, null);
    EntityUtil.getEntityDeserializer();
  }

  @Test
  public void getEntitySerializer() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntitySerializer());
    Configuration.clearValue(Configuration.ENTITY_SERIALIZER_CLASS);
  }

  @Test
  public void getEntityDeserializer() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntityDeserializer());
    Configuration.clearValue(Configuration.ENTITY_DESERIALIZER_CLASS);
  }
}
