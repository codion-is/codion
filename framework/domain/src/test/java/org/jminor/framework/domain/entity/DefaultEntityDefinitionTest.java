/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.entity;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Collection;
import java.util.Comparator;

import static org.jminor.framework.domain.entity.KeyGenerators.automatic;
import static org.jminor.framework.domain.entity.OrderBy.orderBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final StringProvider stringProvider = new StringProvider("name");
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id"),
                Properties.columnProperty("name", Types.VARCHAR))
                .selectQuery("select * from dual", false)
                .orderBy(orderBy().descending("name"))
                .readOnly(true).selectTableName("selectTableName").groupByClause("name")
                .stringProvider(stringProvider).comparator(comparator);
      }
    }
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition("entityId");
    assertEquals("entityId", definition.toString());
    assertEquals("entityId", definition.getEntityId());
    assertEquals("tableName", definition.getTableName());
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertEquals("select * from dual", definition.getSelectQuery());
    assertFalse(definition.isSmallDataset());
    assertTrue(definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("name", definition.getGroupByClause());
    assertEquals(stringProvider, definition.getStringProvider());
    assertEquals(comparator, definition.getComparator());
  }

  @Test
  public void foreignKeyPropertyCountMismatch() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("test.composite_key_master",
                Properties.columnProperty("first").primaryKeyIndex(0),
                Properties.columnProperty("second").primaryKeyIndex(1));
        define("test.composite_reference",
                Properties.foreignKeyProperty("reference_fk", null, "test.composite_key_master",
                        Properties.columnProperty("reference")
                                .primaryKeyIndex(0)));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void duplicatePropertyIds() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id"),
                Properties.columnProperty("name", Types.VARCHAR),
                Properties.columnProperty("id"));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void duplicateForeignKeyPropertyIds() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id"),
                Properties.columnProperty("name", Types.VARCHAR),
                Properties.foreignKeyProperty("fkProperty", null, "entityId",
                        Properties.columnProperty("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void setSearchPropertyIds() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id"),
                Properties.columnProperty("name", Types.VARCHAR)).searchPropertyIds("id");
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void derivedProperty() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("id"),
                Properties.columnProperty("name", Types.VARCHAR),
                Properties.columnProperty("info", Types.VARCHAR),
                Properties.derivedProperty("derived", Types.VARCHAR, null, linkedValues ->
                        linkedValues.get("name").toString() + linkedValues.get("info"), "name", "info"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    Collection<DerivedProperty> linked = definition.getDerivedProperties("name");
    assertTrue(linked.contains(definition.getProperty("derived")));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties("info");
    assertTrue(linked.contains(definition.getProperty("derived")));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("p0").aggregateColumn(true),
                Properties.columnProperty("p1").groupingColumn(true),
                Properties.columnProperty("p2").groupingColumn(true));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("p0").aggregateColumn(true),
                Properties.columnProperty("p1").groupingColumn(true),
                Properties.columnProperty("p2").groupingColumn(true)).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, TestDomain::new);
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("p0")).havingClause(havingClause);
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("p0")).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, TestDomain::new);
  }

  @Test
  public void testForeignKeyWithNoPrimaryKey() {
    final String entityId1 = "testForeignKeyWithNoPrimaryKey";
    final String entityId2 = "testForeignKeyWithNoPrimaryKey2";
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId1,
                Properties.columnProperty("propertyId", Types.INTEGER));
        define(entityId2,
                Properties.foreignKeyProperty("fk", null, entityId1,
                        Properties.columnProperty("fk_col")));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void testForeignPrimaryKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        setStrictForeignKeys(false);
        define("entityId",
                Properties.foreignKeyProperty("fkPropertyID", "caption", "parent",
                        Properties.primaryKeyProperty("propertyId")));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testPropertyIDConflict() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("pk"),
                Properties.columnProperty("col"),
                Properties.columnProperty("col"));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void testPropertyIDConflictInForeignKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("pk"),
                Properties.columnProperty("col"),
                Properties.foreignKeyProperty("fk", "cap", "par",
                        Properties.columnProperty("col")));
      }
    }
    assertThrows(IllegalArgumentException.class, TestDomain::new);
  }

  @Test
  public void testLinkedProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("pk"),
                Properties.columnProperty("1"),
                Properties.columnProperty("2"),
                Properties.derivedProperty("der", Types.INTEGER, "cap", linkedValues -> null, "1", "2"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    assertTrue(definition.hasDerivedProperties("1"));
    assertTrue(definition.hasDerivedProperties("2"));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    class TestDomain extends Domain {
      public TestDomain() {
        define("entity",
                Properties.primaryKeyProperty("propertyId"))
                .colorProvider((entity1, property) -> colorBlue);
      }
    }
    final Domain domain = new TestDomain();

    final Entity entity = domain.entity("entity");
    final EntityDefinition definition = domain.getDefinition("entity");
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  void testDefaultStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId"));
      }
    }
    final Domain domain = new TestDomain();

    final Entity entity = domain.entity("entityToString");
    entity.put("propertyId", 1);
    assertEquals("entityToString: propertyId:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId")).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, TestDomain::new);
  }

  @Test
  public void setToStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId")).stringProvider(entity -> "test");

      }
    }
    final Domain domain = new TestDomain();

    final Entity entity = domain.entity("entityToString");
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    final String entityId = "defaultKeyGenerator";
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty("propertyId"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertTrue(definition.getKeyGenerator().isInserted());
  }

  @Test
  public void nullKeyGenerator() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("nullKeyGenerator",
                Properties.primaryKeyProperty("propertyId")).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, TestDomain::new);
  }

  @Test
  public void keyGenerator() {
    final String entityId = "automaticKeyGenerator";
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty("propertyId"))
                .keyGenerator(automatic("table"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    assertNotNull(definition.getKeyGenerator());
    assertTrue(definition.isKeyGenerated());
    assertFalse(definition.getKeyGenerator().isInserted());
  }
}
