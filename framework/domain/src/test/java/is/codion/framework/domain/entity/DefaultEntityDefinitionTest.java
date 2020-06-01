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
import static is.codion.framework.domain.property.Attributes.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  @Test
  public void test() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> id = integerAttribute("id", entityId);
    final Attribute<String> name = stringAttribute("name", entityId);
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
        final Attribute<Integer> first = integerAttribute("first", entityId1);
        final Attribute<Integer> second = integerAttribute("second", entityId1);
        define(entityId1,
                Properties.columnProperty(first).primaryKeyIndex(0),
                Properties.columnProperty(second).primaryKeyIndex(1));
        final Identity entityId2 = Identity.identity("test.composite_reference");
        final EntityAttribute reference_fk = entityAttribute("reference_fk", entityId2);
        final Attribute<?> reference = integerAttribute("reference", entityId2);
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
                Properties.primaryKeyProperty(integerAttribute("id", entityId)),
                Properties.columnProperty(stringAttribute("name", entityId)),
                Properties.columnProperty(integerAttribute("id", entityId)));
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
                Properties.primaryKeyProperty(integerAttribute("id", entityId)),
                Properties.columnProperty(stringAttribute("name", entityId)),
                Properties.foreignKeyProperty(entityAttribute("fkProperty", entityId), null, entityId,
                        Properties.columnProperty(integerAttribute("id", entityId))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> name = integerAttribute("name", entityId);
    final Attribute<String> info = stringAttribute("info", entityId);
    final Attribute<String> derived = stringAttribute("derived", entityId);
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(integerAttribute("id", entityId)),
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
                Properties.primaryKeyProperty(integerAttribute("p0", entityId)).aggregateColumn(true),
                Properties.columnProperty(integerAttribute("p1", entityId)).groupingColumn(true),
                Properties.columnProperty(integerAttribute("p2", entityId)).groupingColumn(true));
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
                Properties.primaryKeyProperty(integerAttribute("p0", entityId)).aggregateColumn(true),
                Properties.columnProperty(integerAttribute("p1", entityId)).groupingColumn(true),
                Properties.columnProperty(integerAttribute("p2", entityId)).groupingColumn(true)).groupByClause("p1, p2");
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
                Properties.primaryKeyProperty(integerAttribute("p0", entityId))).havingClause(havingClause);
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
                Properties.primaryKeyProperty(attribute("p0", Integer.class, entityId))).havingClause(havingClause)
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
                Properties.columnProperty(attribute("attribute", Integer.class, entityId1)));
        define(entityId2,
                Properties.foreignKeyProperty(entityAttribute("fk", entityId2), null, entityId1,
                        Properties.columnProperty(attribute("fk_col", Integer.class, entityId2))));
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
                Properties.foreignKeyProperty(entityAttribute("fkAttribute", entityId), "caption", Identity.identity("parent"),
                        Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId))));
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
                Properties.primaryKeyProperty(attribute("pk", Integer.class, entityId)),
                Properties.columnProperty(attribute("col", Integer.class, entityId)),
                Properties.columnProperty(attribute("col", Integer.class, entityId)));
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
                Properties.primaryKeyProperty(attribute("pk", Integer.class, entityId)),
                Properties.columnProperty(attribute("col", Integer.class, entityId)),
                Properties.foreignKeyProperty(entityAttribute("fk", entityId), "cap", Identity.identity("par"),
                        Properties.columnProperty(attribute("col", Integer.class, entityId))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final Identity entityId = Identity.identity("entityId");
    final Attribute<Integer> attribute1 = attribute("1", Integer.class, entityId);
    final Attribute<Integer> attribute2 = attribute("2", Integer.class, entityId);
    class TestDomain extends Domain {
      public TestDomain() {
        define(entityId,
                Properties.primaryKeyProperty(attribute("pk", Integer.class, entityId)),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(attribute("der", Integer.class, entityId), "cap", linkedValues -> null, attribute1, attribute2));
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId)))
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
    final Attribute<Integer> attribute = attribute("attribute", Integer.class, entityId);
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId))).stringProvider(null);
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId))).stringProvider(entity -> "test");
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId)));
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId))).keyGenerator(null);
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class, entityId)))
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
