/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.Collection;
import java.util.Comparator;

import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final StringProvider stringProvider = new StringProvider("name");
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id", Types.INTEGER),
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
  public void entityWithValueProvider() {
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    final Entity detail = definition.entity(property -> null);
    assertFalse(detail.containsKey(TestDomain.DETAIL_DOUBLE));//columnHasDefaultValue
    assertFalse(detail.containsKey(TestDomain.DETAIL_DATE));//columnHasDefaultValue
    assertTrue(detail.containsKey(TestDomain.DETAIL_BOOLEAN_NULLABLE));//columnHasDefaultValue && property.hasDefaultValue
  }

  @Test
  public void foreignKeyPropertyCountMismatch() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("test.composite_key_master",
                Properties.columnProperty("first", Types.INTEGER).primaryKeyIndex(0),
                Properties.columnProperty("second", Types.INTEGER).primaryKeyIndex(1));
        define("test.composite_reference",
                Properties.foreignKeyProperty("reference_fk", null, "test.composite_key_master",
                        Properties.columnProperty("reference", Types.INTEGER)
                                .primaryKeyIndex(0)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicatePropertyIds() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id", Types.INTEGER),
                Properties.columnProperty("name", Types.VARCHAR),
                Properties.columnProperty("id", Types.INTEGER));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicateForeignKeyPropertyIds() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty("id", Types.INTEGER),
                Properties.columnProperty("name", Types.VARCHAR),
                Properties.foreignKeyProperty("fkProperty", null, "entityId",
                        Properties.columnProperty("id", Types.INTEGER)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("id", Types.INTEGER),
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
                Properties.primaryKeyProperty("p0", Types.INTEGER).aggregateColumn(true),
                Properties.columnProperty("p1", Types.INTEGER).groupingColumn(true),
                Properties.columnProperty("p2", Types.INTEGER).groupingColumn(true));
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
                Properties.primaryKeyProperty("p0", Types.INTEGER).aggregateColumn(true),
                Properties.columnProperty("p1", Types.INTEGER).groupingColumn(true),
                Properties.columnProperty("p2", Types.INTEGER).groupingColumn(true)).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("p0", Types.INTEGER)).havingClause(havingClause);
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
                Properties.primaryKeyProperty("p0", Types.INTEGER)).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
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
                        Properties.columnProperty("fk_col", Types.INTEGER)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignPrimaryKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        setStrictForeignKeys(false);
        define("entityId",
                Properties.foreignKeyProperty("fkPropertyID", "caption", "parent",
                        Properties.primaryKeyProperty("propertyId", Types.INTEGER)));
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
                Properties.primaryKeyProperty("pk", Types.INTEGER),
                Properties.columnProperty("col", Types.INTEGER),
                Properties.columnProperty("col", Types.INTEGER));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testPropertyIDConflictInForeignKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("pk", Types.INTEGER),
                Properties.columnProperty("col", Types.INTEGER),
                Properties.foreignKeyProperty("fk", "cap", "par",
                        Properties.columnProperty("col", Types.INTEGER)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty("pk", Types.INTEGER),
                Properties.columnProperty("1", Types.INTEGER),
                Properties.columnProperty("2", Types.INTEGER),
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
                Properties.primaryKeyProperty("propertyId", Types.INTEGER))
                .colorProvider((entity1, property) -> colorBlue);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity("entity");
    final EntityDefinition definition = entities.getDefinition("entity");
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  void testDefaultStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId", Types.INTEGER));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity("entityToString");
    entity.put("propertyId", 1);
    assertEquals("entityToString: propertyId:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId", Types.INTEGER)).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void setToStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty("propertyId", Types.INTEGER)).stringProvider(entity -> "test");

      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity("entityToString");
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    final String entityId = "defaultKeyGenerator";
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty("propertyId", Types.INTEGER));
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
                Properties.primaryKeyProperty("propertyId", Types.INTEGER)).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void keyGenerator() {
    final String entityId = "automaticKeyGenerator";
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty("propertyId", Types.INTEGER))
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
