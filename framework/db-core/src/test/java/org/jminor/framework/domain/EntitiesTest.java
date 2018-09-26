/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class EntitiesTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void isPrimaryKeyModified() {
    assertFalse(domain.isKeyModified(null));
    assertFalse(domain.isKeyModified(Collections.<Entity>emptyList()));

    final Entity department = domain.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 1);
    department.put(TestDomain.DEPARTMENT_NAME, "name");
    department.put(TestDomain.DEPARTMENT_LOCATION, "loc");
    assertFalse(domain.isKeyModified(Collections.singletonList(department)));

    department.put(TestDomain.DEPARTMENT_NAME, "new name");
    assertFalse(domain.isKeyModified(Collections.singletonList(department)));

    department.put(TestDomain.DEPARTMENT_ID, 2);
    assertTrue(domain.isKeyModified(Collections.singletonList(department)));

    department.revert(TestDomain.DEPARTMENT_ID);
    assertFalse(domain.isKeyModified(Collections.singletonList(department)));
  }

  @Test
  public void getSortedProperties() {
    final List<Property> properties = domain.getSortedProperties(TestDomain.T_EMP,
            Arrays.asList(TestDomain.EMP_HIREDATE, TestDomain.EMP_COMMISSION,
                    TestDomain.EMP_SALARY, TestDomain.EMP_JOB));
    assertEquals(TestDomain.EMP_COMMISSION, properties.get(0).getPropertyId());
    assertEquals(TestDomain.EMP_HIREDATE, properties.get(1).getPropertyId());
    assertEquals(TestDomain.EMP_JOB, properties.get(2).getPropertyId());
    assertEquals(TestDomain.EMP_SALARY, properties.get(3).getPropertyId());
  }

  @Test
  public void getUpdatableProperties() {
    final List<Property> properties = domain.getUpdatableProperties(TestDomain.T_DETAIL);
    assertEquals(9, properties.size());
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_NAME)));
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_CODE)));
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void getSelectedProperties() {
    final List<String> propertyIds = new ArrayList<>();
    propertyIds.add(TestDomain.DEPARTMENT_ID);
    propertyIds.add(TestDomain.DEPARTMENT_NAME);

    final Collection<Property> properties = domain.getProperties(TestDomain.T_DEPARTMENT, propertyIds);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID)));
    assertTrue(properties.contains(domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));

    final Collection<Property> noProperties = domain.getProperties(TestDomain.T_DEPARTMENT, Collections.emptyList());
    assertEquals(0, noProperties.size());
  }

  @Test
  public void getEntitySerializerUnconfigured() {
    domain.ENTITY_SERIALIZER_CLASS.set(null);
    assertThrows(RuntimeException.class, domain::getEntitySerializer);
  }

  @Test
  public void getModifiedColumnProperties() {
    final Entity entity = domain.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entity.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    entity.put(TestDomain.DEPARTMENT_NAME, "Name");

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
    assertEquals(Entities.getModifiedColumnProperties(current, entity).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_ID);
    current.remove(TestDomain.DEPARTMENT_ID);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertEquals(Entities.getModifiedColumnProperties(current, entity).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_ID);
    current.put(TestDomain.DEPARTMENT_ID, 1);
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertTrue(Entities.getModifiedColumnProperties(current, entity).isEmpty());

    current.put(TestDomain.DEPARTMENT_LOCATION, "New location");
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(Entities.getModifiedColumnProperties(current, entity).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_LOCATION);
    current.remove(TestDomain.DEPARTMENT_LOCATION);
    current.saveAll();
    assertTrue(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(Entities.getModifiedColumnProperties(current, entity).iterator().next().getPropertyId(), TestDomain.DEPARTMENT_LOCATION);
    current.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.saveAll();
    assertFalse(Entities.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertTrue(Entities.getModifiedColumnProperties(current, entity).isEmpty());

    entity.put(TestDomain.DEPARTMENT_LOCATION, "new loc");
    entity.put(TestDomain.DEPARTMENT_NAME, "new name");

    assertEquals(Entities.getModifiedColumnProperties(current, entity).size(), 2);
  }

  @Test
  public void key() {
    final String entityId = "EntitiesTest.key";
    final String propertyId1 = "id1";
    final String propertyId2 = "id2";
    final String propertyId3 = "id3";
    domain.define(entityId,
            Properties.primaryKeyProperty(propertyId1),
            Properties.primaryKeyProperty(propertyId2).setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty(propertyId3).setPrimaryKeyIndex(2).setNullable(true));

    final Entity.Key key = domain.key(entityId);
    assertEquals(0, key.hashCode());
    assertTrue(key.isCompositeKey());
    assertTrue(key.isNull());

    key.put(propertyId1, 1);
    key.put(propertyId2, 2);
    key.put(propertyId3, 3);
    assertFalse(key.isNull());
    assertEquals(6, key.hashCode());

    key.put(propertyId2, 3);
    assertEquals(7, key.hashCode());

    key.put(propertyId3, null);
    assertFalse(key.isNull());
    assertEquals(4, key.hashCode());
    key.put(propertyId2, null);
    assertTrue(key.isNull());
    assertEquals(0, key.hashCode());
    key.put(propertyId2, 4);
    assertFalse(key.isNull());
    assertEquals(5, key.hashCode());

    key.put(propertyId2, 42);
    assertFalse(key.isNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> domain.key((String) null));
  }

  @Test
  public void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("keyWithSameIndex",
            Properties.primaryKeyProperty("1").setPrimaryKeyIndex(0),
            Properties.primaryKeyProperty("2").setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty("3").setPrimaryKeyIndex(1)));
  }

  @Test
  public void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("keyWithSameIndex2",
            Properties.primaryKeyProperty("1"),
            Properties.primaryKeyProperty("2"),
            Properties.primaryKeyProperty("3")));
  }

  @Test
  public void entity() {
    final Entity.Key key = domain.key(TestDomain.T_MASTER);
    key.put(TestDomain.MASTER_ID, 10L);

    final Entity master = domain.entity(key);
    assertEquals(TestDomain.T_MASTER, master.getEntityId());
    assertTrue(master.containsKey(TestDomain.MASTER_ID));
    assertEquals(10L, master.get(TestDomain.MASTER_ID));

    assertThrows(NullPointerException.class, () -> domain.entity((String) null));
  }

  @Test
  public void getProperties() {
    final Property id = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    final Property location = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_LOCATION);
    final Property name = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final List<Property> properties = domain.getProperties(TestDomain.T_DEPARTMENT, Arrays.asList(TestDomain.DEPARTMENT_LOCATION, TestDomain.DEPARTMENT_NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    final Collection<Property> visibleProperties = domain.getProperties(TestDomain.T_DEPARTMENT, false);
    assertEquals(3, visibleProperties.size());
    assertTrue(visibleProperties.contains(id));
    assertTrue(visibleProperties.contains(location));
    assertTrue(visibleProperties.contains(name));

    final Collection<Property> allProperties = domain.getProperties(TestDomain.T_DEPARTMENT, true);
    assertTrue(visibleProperties.containsAll(allProperties));
  }

  @Test
  public void getPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getProperty(TestDomain.T_MASTER, "unknown property"));
  }

  @Test
  public void getColumnProperties() {
    List<Property.ColumnProperty> properties = domain.getColumnProperties(TestDomain.T_MASTER);
    assertEquals(3, properties.size());
    properties = domain.getColumnProperties(TestDomain.T_MASTER, null);
    assertTrue(properties.isEmpty());
    properties = domain.getColumnProperties(TestDomain.T_MASTER, Collections.emptyList());
    assertTrue(properties.isEmpty());
  }

  @Test
  public void getForeignKeyProperties() {
    List<Property.ForeignKeyProperty> foreignKeyProperties = domain.getForeignKeyProperties(TestDomain.T_DETAIL, TestDomain.T_EMP);
    assertEquals(0, foreignKeyProperties.size());
    foreignKeyProperties = domain.getForeignKeyProperties(TestDomain.T_DETAIL, TestDomain.T_MASTER);
    assertEquals(1, foreignKeyProperties.size());
    assertTrue(foreignKeyProperties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK)));
  }

  @Test
  public void getForeignKeyProperty() {
    assertNotNull(domain.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getForeignKeyPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getForeignKeyProperty(TestDomain.T_DETAIL, "bla bla"));
  }

  @Test
  public void getDomainEntityIds() {
    final Entities domain = Entities.getDomain(new TestDomain().getDomainId());
    assertNotNull(domain.getDefinition(TestDomain.T_DEPARTMENT));
    assertNotNull(domain.getDefinition(TestDomain.T_EMP));
  }

  @Test
  public void hasDerivedProperties() {
    assertFalse(domain.hasDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN));
    assertTrue(domain.hasDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_INT));
  }

  @Test
  public void getDerivedProperties() {
    Collection<Property.DerivedProperty> derivedProperties = domain.getDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN);
    assertTrue(derivedProperties.isEmpty());
    derivedProperties = domain.getDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_INT);
    assertEquals(1, derivedProperties.size());
    assertTrue(derivedProperties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void hasDenormalizedProperties() {
    assertFalse(domain.hasDenormalizedProperties(TestDomain.T_DEPARTMENT));
    assertTrue(domain.hasDenormalizedProperties(TestDomain.T_DETAIL));
    assertTrue(domain.hasDenormalizedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getDenormalizedProperties() {
    final List<Property.DenormalizedProperty> denormalized = domain.getDenormalizedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK);
    assertFalse(denormalized.isEmpty());
    assertEquals(TestDomain.DETAIL_MASTER_CODE_DENORM, denormalized.get(0).getPropertyId());
  }

  @Test
  public void isSmallDataset() {
    assertTrue(domain.isSmallDataset(TestDomain.T_DETAIL));
  }

  @Test
  public void getStringProvider() {
    assertNotNull(domain.getStringProvider(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void redefine() {
    final String entityId = "entityId";
    domain.define(entityId, Properties.primaryKeyProperty("propertyId"));
    assertThrows(IllegalArgumentException.class, () -> domain.define(entityId, Properties.primaryKeyProperty("propertyId")));
  }

  @Test
  public void redefineAllowed() {
    final String entityId = "entityId2";
    domain.define(entityId, Properties.primaryKeyProperty("id"));
    assertEquals("id", domain.getPrimaryKeyProperties(entityId).get(0).getPropertyId());
    domain.ALLOW_REDEFINE_ENTITY.set(true);
    domain.define(entityId, Properties.primaryKeyProperty("id2"));
    assertEquals("id2", domain.getPrimaryKeyProperties(entityId).get(0).getPropertyId());
    domain.ALLOW_REDEFINE_ENTITY.set(false);
  }

  @Test
  public void nullValidation() {
    final Entity emp = domain.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, new Date());
    emp.put(TestDomain.EMP_SALARY, 1200.0);

    final Entities.Validator validator = new Entities.Validator();
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_DEPARTMENT_FK, e.getKey());
    }
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    try {
      validator.validate(emp);
    }
    catch (final ValidationException e) {
      fail();
    }
    emp.put(TestDomain.EMP_SALARY, null);
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_SALARY, e.getKey());
    }
  }

  @Test
  public void getSearchProperties() {
    Collection<Property.ColumnProperty> searchProperties = domain.getSearchProperties(TestDomain.T_EMP);
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB)));
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    searchProperties = domain.getSearchProperties(TestDomain.T_EMP, TestDomain.EMP_NAME);
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    searchProperties = domain.getSearchProperties(TestDomain.T_DEPARTMENT);
    //should contain all string based properties
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  public void getSearchPropertyIds() {
    Collection<String> searchPropertyIDs = domain.getSearchPropertyIds(TestDomain.T_EMP);
    assertTrue(searchPropertyIDs.contains(TestDomain.EMP_JOB));
    assertTrue(searchPropertyIDs.contains(TestDomain.EMP_NAME));

    searchPropertyIDs = domain.getSearchPropertyIds(TestDomain.T_DEPARTMENT);
    assertTrue(searchPropertyIDs.contains(TestDomain.DEPARTMENT_NAME));
  }

  @Test
  public void stringProvider() {
    final Entity department = domain.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    department.put(TestDomain.DEPARTMENT_LOCATION, "Reykjavik");
    department.put(TestDomain.DEPARTMENT_NAME, "Sales");

    final Entity employee = domain.entity(TestDomain.T_EMP);
    final Date hiredate = new Date();
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_NAME, "Darri");
    employee.put(TestDomain.EMP_HIREDATE, hiredate);

    final DateFormat dateFormat = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    Entities.StringProvider employeeToString = new Entities.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(domain.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK),
                    TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));

    department.put(TestDomain.DEPARTMENT_LOCATION, null);
    department.put(TestDomain.DEPARTMENT_NAME, null);

    employee.put(TestDomain.EMP_DEPARTMENT_FK, null);
    employee.put(TestDomain.EMP_NAME, null);
    employee.put(TestDomain.EMP_HIREDATE, null);

    employeeToString = new Entities.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(domain.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK),
                    TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat).addText(")");

    assertEquals(" (department: , location: , hiredate: )", employeeToString.toString(employee));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id"))));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntityNonStrict() {
    Entity.Definition.STRICT_FOREIGN_KEYS.set(false);
    domain.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id")));
    Entity.Definition.STRICT_FOREIGN_KEYS.set(true);
  }

  @Test
  public void setSearchPropertyIdsInvalidProperty() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("spids",
            Properties.primaryKeyProperty("1"),
            Properties.columnProperty("test"))
            .setSearchPropertyIds("invalid"));
  }

  @Test
  public void hasSingleIntegerPrimaryKey() {
    String entityId = "hasSingleIntegerPrimaryKey";
    domain.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0));
    assertTrue(domain.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey2";
    domain.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0),
            Properties.columnProperty("test2")
                    .setPrimaryKeyIndex(1));
    assertFalse(domain.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey3";
    domain.define(entityId,
            Properties.columnProperty("test", Types.VARCHAR)
                    .setPrimaryKeyIndex(0));
    assertFalse(domain.hasSingleIntegerPrimaryKey(entityId));
  }

  @Test
  public void havingClause() {
    final String havingClause = "p1 > 1";
    domain.define("entityId3",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertEquals(havingClause, domain.getHavingClause("entityId3"));
  }

  @Test
  public void validateTypeEntity() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    final Entity entity1 = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_MASTER_FK, entity1));
  }

  @Test
  public void setValueDerived() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_DERIVED, 10));
  }

  @Test
  public void setValueValueList() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_VALUE_LIST, -10));
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
    assertTrue(Entities.getValues(TestDomain.DEPARTMENT_ID, Collections.<Entity>emptyList()).isEmpty());
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

    final String[][] strings = Entities.getStringValueArray(domain.getColumnProperties(TestDomain.T_DEPARTMENT), Arrays.asList(dept1, dept2));
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

    final List<Entity> copies = Entities.copyEntities(Arrays.asList(dept1, dept2));
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

    final Map<Integer, Collection<Entity>> map = Entities.mapToValue(TestDomain.DEPARTMENT_ID, entityList);
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

    final Collection<Entity> entities = Arrays.asList(one, two, three, four);
    final Map<String, Collection<Entity>> map = Entities.mapToEntityId(entities);

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
  public void addOperationExisting() {
    final DatabaseConnection.Operation operation = new AbstractProcedure<DatabaseConnection>("operationId", "test") {
      @Override
      public void execute(final DatabaseConnection databaseConnection, final Object... arguments) {}
    };
    domain.addOperation(operation);
    assertThrows(IllegalArgumentException.class, () -> domain.addOperation(operation));
  }

  @Test
  public void getFunctionNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> domain.getFunction("nonexistingfunctionid"));
  }

  @Test
  public void getProcedureNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> domain.getProcedure("nonexistingprocedureid"));
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

    final List<Entity> entities = Arrays.asList(one, two, three);

    final Map<String, Object> values = new HashMap<>();
    values.put(TestDomain.DETAIL_STRING, "b");
    assertEquals(1, Entities.getEntitiesByValue(entities, values).size());
    values.put(TestDomain.DETAIL_STRING, "zz");
    assertEquals(2, Entities.getEntitiesByValue(entities, values).size());
    values.put(TestDomain.DETAIL_ID, 3L);
    assertEquals(1, Entities.getEntitiesByValue(entities, values).size());
  }
}
