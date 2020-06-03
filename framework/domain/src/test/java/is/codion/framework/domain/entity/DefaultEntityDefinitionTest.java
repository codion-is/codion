/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;

import static is.codion.framework.domain.entity.Entities.type;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final EntityType entityType = type("entityType");
    final Attribute<Integer> id = entityType.integerAttribute("id");
    final Attribute<String> name = entityType.stringAttribute("name");
    final StringProvider stringProvider = new StringProvider(name);
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType, "tableName",
                Properties.primaryKeyProperty(id),
                Properties.columnProperty(name))
                .selectQuery("select * from dual", false)
                .orderBy(orderBy().descending(name))
                .readOnly(true).selectTableName("selectTableName").groupByClause("name")
                .stringProvider(stringProvider).comparator(comparator);
      }
    }
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(entityType);
    assertEquals(entityType.getName(), definition.toString());
    assertEquals(entityType, definition.getEntityType());
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
        final EntityType entityType1 = type("test.composite_key_master");
        final Attribute<Integer> first = entityType1.integerAttribute("first");
        final Attribute<Integer> second = entityType1.integerAttribute("second");
        define(entityType1,
                Properties.columnProperty(first).primaryKeyIndex(0),
                Properties.columnProperty(second).primaryKeyIndex(1));
        final EntityType entityType2 = type("test.composite_reference");
        final Attribute<Entity> reference_fk = entityType2.entityAttribute("reference_fk");
        final Attribute<?> reference = entityType2.integerAttribute("reference");
        define(entityType2,
                Properties.foreignKeyProperty(reference_fk, null, entityType1,
                        Properties.columnProperty(reference)
                                .primaryKeyIndex(0)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicateAttributes() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType, "tableName",
                Properties.primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(entityType.stringAttribute("name")),
                Properties.columnProperty(entityType.integerAttribute("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicateForeignKeyAttributes() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType, "tableName",
                Properties.primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(entityType.stringAttribute("name")),
                Properties.foreignKeyProperty(entityType.entityAttribute("fkProperty"), null, entityType,
                        Properties.columnProperty(entityType.integerAttribute("id"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    final EntityType entityType = type("entityType");
    final Attribute<Integer> name = entityType.integerAttribute("name");
    final Attribute<String> info = entityType.stringAttribute("info");
    final Attribute<String> derived = entityType.stringAttribute("derived");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(name),
                Properties.columnProperty(info),
                Properties.derivedProperty(derived, null, linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    Collection<DerivedProperty<?>> linked = definition.getDerivedProperties(name);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties(info);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final EntityType entityType = type("entityType");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn(true));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn(true)).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final EntityType entityType = type("entityType");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("p0"))).havingClause(havingClause);
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("p0"))).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignKeyWithNoPrimaryKey() {
    final EntityType entityType1 = type("testForeignKeyWithNoPrimaryKey");
    final EntityType entityType2 = type("testForeignKeyWithNoPrimaryKey2");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType1,
                Properties.columnProperty(entityType1.integerAttribute("attribute")));
        define(entityType2,
                Properties.foreignKeyProperty(entityType2.entityAttribute("fk"), null, entityType1,
                        Properties.columnProperty(entityType2.integerAttribute("fk_col"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignPrimaryKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        setStrictForeignKeys(false);
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.foreignKeyProperty(entityType.entityAttribute("fkAttribute"), "caption", type("parent"),
                        Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testAttributeConflict() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(entityType.integerAttribute("col")),
                Properties.columnProperty(entityType.integerAttribute("col")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testAttributeConflictInForeignKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(entityType.integerAttribute("col")),
                Properties.foreignKeyProperty(entityType.entityAttribute("fk"), "cap", type("parent"),
                        Properties.columnProperty(entityType.integerAttribute("col"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final EntityType entityType = type("entityType");
    final Attribute<Integer> attribute1 = entityType.integerAttribute("1");
    final Attribute<Integer> attribute2 = entityType.integerAttribute("2");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(entityType.integerAttribute("der"), "cap", linkedValues -> null, attribute1, attribute2));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertTrue(definition.hasDerivedProperties(attribute1));
    assertTrue(definition.hasDerivedProperties(attribute2));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    final EntityType entityType = type("entityType");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute")))
                .colorProvider((entity1, property) -> colorBlue);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    final EntityDefinition definition = entities.getDefinition(entityType);
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  void testDefaultStringProvider() {
    final EntityType entityType = type("entityType");
    final Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(attribute));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    entity.put(attribute, 1);
    assertEquals("entityType: attribute:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void setToStringProvider() {
    final EntityType entityType = type("entityType");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))).stringProvider(entity -> "test");
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    final EntityType entityType = type("defaultKeyGenerator");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute")));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertTrue(definition.getKeyGenerator().isInserted());
  }

  @Test
  public void nullKeyGenerator() {
    class TestDomain extends Domain {
      public TestDomain() {
        final EntityType entityType = type("entityType");
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void keyGenerator() {
    final EntityType entityType = type("entityType");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute")))
                .keyGenerator(automatic("table"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertTrue(definition.isKeyGenerated());
    assertFalse(definition.getKeyGenerator().isInserted());
  }
}
