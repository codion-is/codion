/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  static final DomainType DOMAIN_TYPE = domainType("domainType");

  @Test
  public void test() {
    final EntityType entityType = DOMAIN_TYPE.entityType("test");
    final Attribute<Integer> id = entityType.integerAttribute("id");
    final Attribute<String> name = entityType.stringAttribute("name");
    final StringProvider stringProvider = new StringProvider(name);
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
        super(DOMAIN_TYPE);
        final EntityType entityType1 = DOMAIN_TYPE.entityType("test.composite_key_master");
        final Attribute<Integer> first = entityType1.integerAttribute("first");
        final Attribute<Integer> second = entityType1.integerAttribute("second");
        define(entityType1,
                Properties.columnProperty(first).primaryKeyIndex(0),
                Properties.columnProperty(second).primaryKeyIndex(1));
        final EntityType entityType2 = DOMAIN_TYPE.entityType("test.composite_reference");
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
        super(DOMAIN_TYPE);
        final EntityType entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
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
        super(DOMAIN_TYPE);
        final EntityType entityType = DOMAIN_TYPE.entityType("duplicateForeignKeyAttributes");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("derivedProperty");
    final Attribute<Integer> name = entityType.integerAttribute("name");
    final Attribute<String> info = entityType.stringAttribute("info");
    final Attribute<String> derived = entityType.stringAttribute("derived");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
    Collection<Attribute<?>> linked = definition.getDerivedAttributes(name);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
    linked = definition.getDerivedAttributes(info);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testGroupingProperties");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
        super(DOMAIN_TYPE);
        final EntityType entityType = DOMAIN_TYPE.entityType("testSetGroupByClauseWithGroupingProperties");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
   final EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClauseAlreadySet");
    final String havingClause = "p1 > 1";
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("p0"))).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignKeyWithNoPrimaryKey() {
    final EntityType entityType1 = DOMAIN_TYPE.entityType("testForeignKeyWithNoPrimaryKey");
    final EntityType entityType2 = DOMAIN_TYPE.entityType("testForeignKeyWithNoPrimaryKey2");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
    final EntityType entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        setStrictForeignKeys(false);
        define(entityType,
                Properties.foreignKeyProperty(entityType.entityAttribute("fkAttribute"), "caption", DOMAIN_TYPE.entityType("parent"),
                        Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testAttributeConflict() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
    final EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflictInForeignKey");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(entityType.integerAttribute("col")),
                Properties.foreignKeyProperty(entityType.entityAttribute("fk"), "cap", DOMAIN_TYPE.entityType("parent"),
                        Properties.columnProperty(entityType.integerAttribute("col"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testLinkedProperties");
    final Attribute<Integer> attribute1 = entityType.integerAttribute("1");
    final Attribute<Integer> attribute2 = entityType.integerAttribute("2");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(entityType.integerAttribute("der"), "cap", linkedValues -> null, attribute1, attribute2));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition(entityType);
    assertTrue(definition.hasDerivedAttributes(attribute1));
    assertTrue(definition.hasDerivedAttributes(attribute2));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    final EntityType entityType = DOMAIN_TYPE.entityType("getColor");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute")))
                .colorProvider((entity1, attribute) -> colorBlue);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    final EntityDefinition definition = entities.getDefinition(entityType);
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getKey().getAttribute()));
  }

  @Test
  void testDefaultStringProvider() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
    final Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(attribute));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    entity.put(attribute, 1);
    assertEquals("testDefaultStringProvider: attribute:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    final EntityType entityType = DOMAIN_TYPE.entityType("nullStringProvider");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void stringProvider() {
    final EntityType entityType = DOMAIN_TYPE.entityType("stringProvider");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
    final EntityType entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
    final EntityType entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                Properties.primaryKeyProperty(entityType.integerAttribute("attribute"))).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void keyGenerator() {
    final EntityType entityType = DOMAIN_TYPE.entityType("keyGenerator");
    class TestDomain extends Domain {
      public TestDomain() {
        super(DOMAIN_TYPE);
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
