/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;

import org.junit.Test;

import java.awt.Color;
import java.sql.Types;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityDefinitionImplTest {

  @Test
  public void test() {
    final Entities.StringProvider stringProvider = new Entities.StringProvider("name");
    final Comparator<Entity> comparator = new Comparator<Entity>() {
      @Override
      public int compare(final Entity o1, final Entity o2) {
        return 0;
      }
    };
    final Entity.Definition definition = new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR))
            .setSelectQuery("select * from dual").setOrderByClause("order by name")
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
  public void duplicatePropertyIDs() {
    new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("id"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void duplicateForeignKeyPropertyIDs() {
    new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.foreignKeyProperty("fkProperty", null, "entityID",
                    Properties.columnProperty("id")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void setSearchPropertyIDs() {
    final Entity.Definition definition = new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR));
    definition.setSearchPropertyIDs("id");
  }

  @Test
  public void derivedProperty() {
    final Entity.Definition definition = new EntityDefinitionImpl("entityID", "tableName",
            Properties.primaryKeyProperty("id"),
            Properties.columnProperty("name", Types.VARCHAR),
            Properties.columnProperty("info", Types.VARCHAR),
            Properties.derivedProperty("derived", Types.VARCHAR, null, new Property.DerivedProperty.Provider() {
              @Override
              public Object getValue(final Map<String, Object> linkedValues) {
                return ((String) linkedValues.get("name")) + linkedValues.get("info");
              }
            }, "name", "info"));
    Collection<String> linked = definition.getLinkedPropertyIDs("name");
    assertTrue(linked.contains("derived"));
    assertEquals(1, linked.size());
    linked = definition.getLinkedPropertyIDs("info");
    assertTrue(linked.contains("derived"));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final Entity.Definition definition = new EntityDefinitionImpl("entityID",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetGroupByClauseWithGroupingProperties() {
    final Entity.Definition definition = new EntityDefinitionImpl("entityID",
            Properties.primaryKeyProperty("p0").setAggregateColumn(true),
            Properties.columnProperty("p1").setGroupingColumn(true),
            Properties.columnProperty("p2").setGroupingColumn(true));
    definition.setGroupByClause("p1, p2");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoPrimaryKey() {
    new EntityDefinitionImpl("entityID", "tableName",
            Properties.columnProperty("propertyID", Types.INTEGER));
  }

  @Test
  public void testForeignPrimaryKey() {
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, false);
    new EntityDefinitionImpl("entityID", "tableName",
            Properties.foreignKeyProperty("fkPropertyID", "caption", "parent",
                    Properties.primaryKeyProperty("propertyID")));
    Configuration.setValue(Configuration.STRICT_FOREIGN_KEYS, true);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIDConflict() {
    new EntityDefinitionImpl("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.columnProperty("col"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPropertyIDConflictInForeignKey() {
    new EntityDefinitionImpl("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("col"),
            Properties.foreignKeyProperty("fk", "cap", "par",
                    Properties.columnProperty("col")));
  }

  @Test
  public void testLinkedProperties() {
    final EntityDefinitionImpl def = new EntityDefinitionImpl("entityId",
            Properties.primaryKeyProperty("pk"),
            Properties.columnProperty("1"),
            Properties.columnProperty("2"),
            Properties.derivedProperty("der", Types.INTEGER, "cap", new Property.DerivedProperty.Provider() {
              @Override
              public Object getValue(final Map<String, Object> linkedValues) {
                return null;
              }
            }, "1", "2"));
    assertTrue(def.hasLinkedProperties("1"));
    assertTrue(def.hasLinkedProperties("2"));
  }

  @Test
  public void getBackgroundColor() {
    final Entity.Definition def = Entities.define("entity", "tableName",
            Properties.primaryKeyProperty("propertyID"));
    final Entity entity = Entities.entity("entity");
    assertNull(def.getBackgroundColor(entity, entity.getPrimaryKey().getFirstKeyProperty()));
    def.setBackgroundColorProvider(new Entity.BackgroundColorProvider() {
      @Override
      public Color getBackgroundColor(final Entity entity, final Property property) {
        return Color.BLUE;
      }
    });
    assertEquals(Color.BLUE, def.getBackgroundColor(entity, entity.getPrimaryKey().getFirstKeyProperty()));
  }

  @Test
  public void setToStringProvider() {
    final Entity.Definition def = Entities.define("entityToString", "tableName",
            Properties.primaryKeyProperty("propertyID"));
    final Entity entity = Entities.entity("entityToString");
    entity.setValue("propertyID", 1);
    assertEquals("entityToString: propertyID:1", entity.toString());
    def.setStringProvider(new Entity.ToString() {
      @Override
      public String toString(final Entity valueMap) {
        return "test";
      }
    });
    //the toString value is cached, so we need to clear it by setting a value
    entity.setValue("propertyID", 2);
    assertEquals("test", entity.toString());
  }
}
