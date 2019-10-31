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
    final Entity.Definition definition = domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR))
            .setSelectQuery("select * from dual", false)
            .setOrderBy(Domain.orderBy().descending("name"))
            .setReadOnly(true).setSelectTableName("selectTableName").setGroupByClause("name")
            .setStringProvider(stringProvider).setComparator(comparator);
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
    final Entity.Definition definition = domain.define("entityId", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR));
    assertThrows(IllegalArgumentException.class, () -> definition.setSearchPropertyIds("id"));
  }

  @Test
  public void derivedProperty() {
    final Entity.Definition definition = domain.define("entityId",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("info", Types.VARCHAR),
            Properties.derivedProperty("derived", Types.VARCHAR, null, linkedValues ->
                    ((String) linkedValues.get("name")) + linkedValues.get("info"), "name", "info"));
    Collection<Property.DerivedProperty> linked = definition.getDerivedProperties("name");
    assertTrue(linked.contains(definition.getPropertyMap().get("derived")));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties("info");
    assertTrue(linked.contains(definition.getPropertyMap().get("derived")));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final Entity.Definition definition = domain.define("entityId",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    final Entity.Definition definition = domain.define("entityId",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    assertThrows(IllegalStateException.class, () -> definition.setGroupByClause("p1, p2"));
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final Entity.Definition definition = domain.define("entityId",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    final Entity.Definition definition = domain.define("entityId",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertThrows(IllegalStateException.class, () -> definition.setHavingClause(havingClause));
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
    final Entity.Definition def = domain.define("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("1"),
            Properties.columnProperty("2"),
            Properties.derivedProperty("der", Types.INTEGER, "cap", linkedValues -> null, "1", "2"));
    assertTrue(def.hasDerivedProperties("1"));
    assertTrue(def.hasDerivedProperties("2"));
  }

  @Test
  public void getBackgroundColor() {
    final Entity.Definition def = domain.define("entity",
            Properties.primaryKeyProperty("propertyId"));
    final Entity entity = domain.entity("entity");
    assertNull(def.getBackgroundColor(entity, entity.getKey().getFirstProperty()));
    final String colorBlue = "blue";
    def.setBackgroundColorProvider((entity1, property) -> colorBlue);
    assertEquals(colorBlue, def.getBackgroundColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  public void setToStringProvider() {
    final Entity.Definition definition = domain.define("entityToString",
            Properties.primaryKeyProperty("propertyId"));
    final Entity entity = domain.entity("entityToString");
    entity.put("propertyId", 1);
    assertEquals("entityToString: propertyId:1", entity.toString());
    definition.setStringProvider(valueMap -> "test");
    //the toString value is cached, so we need to clear it by setting a value
    entity.put("propertyId", 2);
    assertEquals("test", entity.toString());
    assertThrows(NullPointerException.class, () -> definition.setStringProvider(null));
  }

  @Test
  public void keyGenerator() {
    final Entity.Definition definition = domain.define("nullKeyGenerator",
            Properties.primaryKeyProperty("propertyId"));
    assertEquals(Entity.KeyGenerator.Type.NONE, definition.getKeyGeneratorType());
    definition.setKeyGenerator(domain.automaticKeyGenerator("table"));
    assertEquals(Entity.KeyGenerator.Type.AUTOMATIC, definition.getKeyGeneratorType());
    assertThrows(NullPointerException.class, () -> definition.setKeyGenerator(null));
  }
}
