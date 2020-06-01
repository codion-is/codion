/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.framework.domain.property.Identity;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;

import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> id = entityId.integerAttribute("id");
    final Attribute<String> name = entityId.stringAttribute("name");
    final StringProvider stringProvider = new StringProvider(name);
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId, "tableName",
                Properties.primaryKeyProperty(id),
                Properties.columnProperty(name))
                .selectQuery("select * from dual", false)
                .orderBy(orderBy().descending(name))
                .readOnly(true).selectTableName("selectTableName").groupByClause("name")
                .stringProvider(stringProvider).comparator(comparator);
      }
    }
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getDefinition(entityId);
    assertEquals(entityId.getName(), definition.toString());
    assertEquals(entityId, definition.getEntityId());
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
        final Identity entityId1 = Identity.identity("test.composite_key_master");
        final Attribute<Integer> first = entityId1.integerAttribute("first");
        final Attribute<Integer> second = entityId1.integerAttribute("second");
        define(entityId1,
                Properties.columnProperty(first).primaryKeyIndex(0),
                Properties.columnProperty(second).primaryKeyIndex(1));
        final Identity entityId2 = Identity.identity("test.composite_reference");
        final EntityAttribute reference_fk = entityId2.entityAttribute("reference_fk");
        final Attribute<?> reference = entityId2.integerAttribute("reference");
        define(entityId2,
                Properties.foreignKeyProperty(reference_fk, null, entityId1,
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
        final Identity entityId = Identity.identity("entityId");
        define(entityId, "tableName",
                Properties.primaryKeyProperty(entityId.integerAttribute("id")),
                Properties.columnProperty(entityId.stringAttribute("name")),
                Properties.columnProperty(entityId.integerAttribute("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicateForeignKeyAttributes() {
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId, "tableName",
                Properties.primaryKeyProperty(entityId.integerAttribute("id")),
                Properties.columnProperty(entityId.stringAttribute("name")),
                Properties.foreignKeyProperty(entityId.entityAttribute("fkProperty"), null, entityId,
                        Properties.columnProperty(entityId.integerAttribute("id"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> name = entityId.integerAttribute("name");
    final Attribute<String> info = entityId.stringAttribute("info");
    final Attribute<String> derived = entityId.stringAttribute("derived");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("id")),
                Properties.columnProperty(name),
                Properties.columnProperty(info),
                Properties.derivedProperty(derived, null, linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    Collection<DerivedProperty<?>> linked = definition.getDerivedProperties(name);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties(info);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final Identity entityId = Identity.identity("entityId");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityId.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityId.integerAttribute("p2")).groupingColumn(true));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityId.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityId.integerAttribute("p2")).groupingColumn(true)).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final Identity entityId = Identity.identity("entityId");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("p0"))).havingClause(havingClause);
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("p0"))).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignKeyWithNoPrimaryKey() {
    final Identity entityId1 = Identity.identity("testForeignKeyWithNoPrimaryKey");
    final Identity entityId2 = Identity.identity("testForeignKeyWithNoPrimaryKey2");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId1,
                Properties.columnProperty(entityId1.integerAttribute("attribute")));
        define(entityId2,
                Properties.foreignKeyProperty(entityId2.entityAttribute("fk"), null, entityId1,
                        Properties.columnProperty(entityId2.integerAttribute("fk_col"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignPrimaryKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        setStrictForeignKeys(false);
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.foreignKeyProperty(entityId.entityAttribute("fkAttribute"), "caption", Identity.identity("parent"),
                        Properties.primaryKeyProperty(entityId.integerAttribute("attribute"))));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testAttributeConflict() {
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("pk")),
                Properties.columnProperty(entityId.integerAttribute("col")),
                Properties.columnProperty(entityId.integerAttribute("col")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testAttributeConflictInForeignKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("pk")),
                Properties.columnProperty(entityId.integerAttribute("col")),
                Properties.foreignKeyProperty(entityId.entityAttribute("fk"), "cap", Identity.identity("parent"),
                        Properties.columnProperty(entityId.integerAttribute("col"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> attribute1 = entityId.integerAttribute("1");
    final Attribute<Integer> attribute2 = entityId.integerAttribute("2");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("pk")),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(entityId.integerAttribute("der"), "cap", linkedValues -> null, attribute1, attribute2));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityId);
    assertTrue(definition.hasDerivedProperties(attribute1));
    assertTrue(definition.hasDerivedProperties(attribute2));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    final Identity entityId = Identity.identity("entityId");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute")))
                .colorProvider((entity1, property) -> colorBlue);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityId);
    final EntityDefinition definition = entities.getDefinition(entityId);
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getKey().getFirstProperty()));
  }

  @Test
  void testDefaultStringProvider() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> attribute = entityId.integerAttribute("attribute");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(attribute));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityId);
    entity.put(attribute, 1);
    assertEquals("entityId: attribute:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute"))).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void setToStringProvider() {
    final Identity entityId = Identity.identity("entityId");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute"))).stringProvider(entity -> "test");
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityId);
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    final Identity entityId = Identity.identity("defaultKeyGenerator");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute")));
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
        final Identity entityId = Identity.identity("entityId");
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute"))).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void keyGenerator() {
    final Identity entityId = Identity.identity("entityId");
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(entityId.integerAttribute("attribute")))
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
