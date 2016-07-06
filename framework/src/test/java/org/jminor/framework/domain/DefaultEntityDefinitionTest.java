/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;

import org.junit.Test;

import java.awt.Color;
import java.sql.Types;
import java.util.Collection;
import java.util.Comparator;

import static org.junit.Assert.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final Entities.StringProvider stringProvider = new Entities.StringProvider("name");
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    final Entity.Definition definition = new DefaultEntityDefinition("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR))
            .setSelectQuery("select * from dual", false).setOrderByClause("order by name")
            .setReadOnly(true).setSelectTableName("selectTableName").setGroupByClause("name")
            .setStringProvider(stringProvider).setComparator(comparator);
    assertEquals("entityID", definition.toString());
    assertEquals("entityID", definition.getEntityID());
    assertEquals("tableName", definition.getTableName());
    assertNotNull(definition.getKeyGenerator());
    assertEquals("select * from dual", definition.getSelectQuery());
    assertEquals(false, definition.isSmallDataset());
    assertEquals("order by name", definition.getOrderByClause());
    assertEquals(true, definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("id, name", definition.getSelectColumnsString());
    assertEquals("name", definition.getGroupByClause());
    assertEquals(stringProvider, definition.getStringProvider());
    assertEquals(comparator, definition.getComparator());
  }

  @Test(expected = IllegalArgumentException.class)
  public void foreignKeyPropertyCountMismatch() {
    Entities.define("test.composite_key_master",
            Properties.columnProperty("first").setPrimaryKeyIndex(0),
            Properties.columnProperty("second").setPrimaryKeyIndex(1));
    new DefaultEntityDefinition("test.composite_reference",
            Properties.foreignKeyProperty("reference_fk", null, "test.composite_key_master",
                    Properties.columnProperty("reference")
                            .setPrimaryKeyIndex(0)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicatePropertyIDs() {
    new DefaultEntityDefinition("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("id"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateForeignKeyPropertyIDs() {
    new DefaultEntityDefinition("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.foreignKeyProperty("fkProperty", null, "entityID",
                    Properties.columnProperty("id")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSearchPropertyIDs() {
    final Entity.Definition definition = new DefaultEntityDefinition("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR));
    definition.setSearchPropertyIDs("id");
  }

  @Test
  public void derivedProperty() {
    final Entity.Definition definition = new DefaultEntityDefinition("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("info", Types.VARCHAR),
            Properties.derivedProperty("derived", Types.VARCHAR, null, linkedValues ->
                    ((String) linkedValues.get("name")) + linkedValues.get("info"), "name", "info"));
    Collection<Property.DerivedProperty> linked = definition.getDerivedProperties("name");
    assertTrue(linked.contains(definition.getProperties().get("derived")));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties("info");
    assertTrue(linked.contains(definition.getProperties().get("derived")));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final Entity.Definition definition = new DefaultEntityDefinition("entityID",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetGroupByClauseWithGroupingProperties() {
    final Entity.Definition definition = new DefaultEntityDefinition("entityID",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    definition.setGroupByClause("p1, p2");
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final Entity.Definition definition = new DefaultEntityDefinition("entityID",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    final Entity.Definition definition = new DefaultEntityDefinition("entityID",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    definition.setHavingClause(havingClause);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoPrimaryKey() {
    new DefaultEntityDefinition("entityID", "tableName",
            Properties.columnProperty("propertyID", Types.INTEGER));
  }

  @Test
  public void testForeignPrimaryKey() {
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, false);
    new DefaultEntityDefinition("entityID", "tableName",
            Properties.foreignKeyProperty("fkPropertyID", "caption", "parent",
                    Properties.primaryKeyProperty("propertyID")));
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIDConflict() {
    new DefaultEntityDefinition("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.columnProperty("col"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIDConflictInForeignKey() {
    new DefaultEntityDefinition("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.foreignKeyProperty("fk", "cap", "par",
                    Properties.columnProperty("col")));
  }

  @Test
  public void testLinkedProperties() {
    final DefaultEntityDefinition def = new DefaultEntityDefinition("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("1"),
            Properties.columnProperty("2"),
            Properties.derivedProperty("der", Types.INTEGER, "cap", linkedValues -> null, "1", "2"));
    assertTrue(def.hasDerivedProperties("1"));
    assertTrue(def.hasDerivedProperties("2"));
  }

  @Test
  public void getBackgroundColor() {
    final Entity.Definition def = Entities.define("entity", "tableName",
            Properties.primaryKeyProperty("propertyID"));
    final Entity entity = Entities.entity("entity");
    assertNull(def.getBackgroundColor(entity, entity.getKey().getFirstProperty()));
    def.setBackgroundColorProvider((entity1, property) -> Color.BLUE);
    assertEquals(Color.BLUE, def.getBackgroundColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  public void setToStringProvider() {
    final Entity.Definition def = Entities.define("entityToString", "tableName",
            Properties.primaryKeyProperty("propertyID"));
    final Entity entity = Entities.entity("entityToString");
    entity.put("propertyID", 1);
    assertEquals("entityToString: propertyID:1", entity.toString());
    def.setStringProvider(valueMap -> "test");
    //the toString value is cached, so we need to clear it by setting a value
    entity.put("propertyID", 2);
    assertEquals("test", entity.toString());
  }
}
