/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Collection;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  private final Domain domain = new Domain();

  @Test
  public void test() {
    final Domain.StringProvider stringProvider = new Domain.StringProvider("name");
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR))
            .setSelectQuery("select * from dual", false)
            .setOrderBy(Domain.orderBy().descending("name"))
            .setReadOnly(true).setSelectTableName("selectTableName").setGroupByClause("name")
            .setStringProvider(stringProvider).setComparator(comparator);
    final Entity.Definition definition = domain.getDefinition("entityId");
    assertEquals("entityId", definition.toString());
    assertEquals("entityId", definition.getEntityId());
    assertEquals("tableName", definition.getTableName());
    assertNotNull(definition.getKeyGenerator());
    assertEquals("select * from dual", definition.getSelectQuery());
    assertFalse(definition.isSmallDataset());
    assertEquals("name", definition.getOrderBy().getOrderByProperties().get(0).getPropertyId());
    assertTrue(definition.getOrderBy().getOrderByProperties().get(0).isDescending());
    assertTrue(definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("name", definition.getGroupByClause());
    assertEquals(stringProvider, definition.getStringProvider());
    assertEquals(comparator, definition.getComparator());
  }

  @Test
  public void foreignKeyPropertyCountMismatch() {
    domain.define("test.composite_key_master",
            Properties.columnProperty("first").setPrimaryKeyIndex(0),
            Properties.columnProperty("second").setPrimaryKeyIndex(1));
    assertThrows(IllegalArgumentException.class, () -> domain.define("test.composite_reference",
            Properties.foreignKeyProperty("reference_fk", null, "test.composite_key_master",
                    Properties.columnProperty("reference")
                            .setPrimaryKeyIndex(0))));
  }

  @Test
  public void duplicatePropertyIds() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("id")));
  }

  @Test
  public void duplicateForeignKeyPropertyIds() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.foreignKeyProperty("fkProperty", null, "entityId",
                    Properties.columnProperty("id"))));
  }

  @Test
  public void setSearchPropertyIds() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR)).setSearchPropertyIds("id"));
  }

  @Test
  public void derivedProperty() {
    domain.define("entityId",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("info", Types.VARCHAR),
            Properties.derivedProperty("derived", Types.VARCHAR, null, linkedValues ->
                    ((String) linkedValues.get("name")) + linkedValues.get("info"), "name", "info"));
    final Entity.Definition definition = domain.getDefinition("entityId");
    Collection<Property.DerivedProperty> linked = definition.getDerivedProperties("name");
    assertTrue(linked.contains(definition.getPropertyMap().get("derived")));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties("info");
    assertTrue(linked.contains(definition.getPropertyMap().get("derived")));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    domain.define("entityId",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    final Entity.Definition definition = domain.getDefinition("entityId");
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    assertThrows(IllegalStateException.class, () -> domain.define("entityId",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true)).setGroupByClause("p1, p2"));
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    domain.define("entityId",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    final Entity.Definition definition = domain.getDefinition("entityId");
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    assertThrows(IllegalStateException.class, () -> domain.define("entityId",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause)
            .setHavingClause(havingClause));
  }

  @Test
  public void testNoPrimaryKey() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId",
            Properties.columnProperty("propertyId", Types.INTEGER)));
  }

  @Test
  public void testForeignPrimaryKey() {
    Entity.Definition.STRICT_FOREIGN_KEYS.set(false);
    domain.define("entityId",
            Properties.foreignKeyProperty("fkPropertyID", "caption", "parent",
                    Properties.primaryKeyProperty("propertyId")));
    Entity.Definition.STRICT_FOREIGN_KEYS.set(true);
  }

  @Test
  public void testPropertyIDConflict() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.columnProperty("col")));
  }

  @Test
  public void testPropertyIDConflictInForeignKey() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.foreignKeyProperty("fk", "cap", "par",
                    Properties.columnProperty("col"))));
  }

  @Test
  public void testLinkedProperties() {
    domain.define("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("1"),
            Properties.columnProperty("2"),
            Properties.derivedProperty("der", Types.INTEGER, "cap", linkedValues -> null, "1", "2"));
    final Entity.Definition definition = domain.getDefinition("entityId");
    assertTrue(definition.hasDerivedProperties("1"));
    assertTrue(definition.hasDerivedProperties("2"));
  }

  @Test
  public void getBackgroundColor() {
    final String colorBlue = "blue";
    domain.define("entity",
            Properties.primaryKeyProperty("propertyId"))
             .setBackgroundColorProvider((entity1, property) -> colorBlue);;
    final Entity entity = domain.entity("entity");
    final Entity.Definition definition = domain.getDefinition("entity");
    assertEquals(colorBlue, definition.getBackgroundColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test void testDefaultStringProvider() {
    domain.define("entityToString",
            Properties.primaryKeyProperty("propertyId"));
    final Entity entity = domain.entity("entityToString");
    entity.put("propertyId", 1);
    assertEquals("entityToString: propertyId:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    assertThrows(NullPointerException.class, () -> domain.define("entityToString",
            Properties.primaryKeyProperty("propertyId")).setStringProvider(null));
  }

  @Test
  public void setToStringProvider() {
    domain.define("entityToString",
            Properties.primaryKeyProperty("propertyId")).setStringProvider(entity -> "test");
    final Entity entity = domain.entity("entityToString");
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    domain.define("nullKeyGenerator",
            Properties.primaryKeyProperty("propertyId"));
    assertEquals(Entity.KeyGenerator.Type.NONE, domain.getKeyGeneratorType("nullKeyGenerator"));
  }

  @Test
  public void nullKeyGenerator() {
    assertThrows(NullPointerException.class, () -> domain.define("nullKeyGenerator",
            Properties.primaryKeyProperty("propertyId")).setKeyGenerator(null));
  }

  @Test
  public void keyGenerator() {
    domain.define("nullKeyGenerator",
            Properties.primaryKeyProperty("propertyId"))
            .setKeyGenerator(domain.automaticKeyGenerator("table"));
    assertEquals(Entity.KeyGenerator.Type.AUTOMATIC, domain.getKeyGeneratorType("nullKeyGenerator"));
  }
}
