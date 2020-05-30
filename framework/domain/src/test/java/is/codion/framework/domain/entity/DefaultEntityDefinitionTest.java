/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.EntityAttribute;
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
    final Attribute<Integer> id = integerAttribute("id");
    final Attribute<String> name = stringAttribute("name");
    final StringProvider stringProvider = new StringProvider(name);
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty(id),
                Properties.columnProperty(name))
                .selectQuery("select * from dual", false)
                .orderBy(orderBy().descending(name))
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
        final Attribute<Integer> first = integerAttribute("first");
        final Attribute<Integer> second = integerAttribute("second");
        define("test.composite_key_master",
                Properties.columnProperty(first).primaryKeyIndex(0),
                Properties.columnProperty(second).primaryKeyIndex(1));
        final EntityAttribute reference_fk = entityAttribute("reference_fk");
        final Attribute<?> reference = attribute("reference", Integer.class);
        define("test.composite_reference",
                Properties.foreignKeyProperty(reference_fk, null, "test.composite_key_master",
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
        define("entityId", "tableName",
                Properties.primaryKeyProperty(integerAttribute("id")),
                Properties.columnProperty(stringAttribute("name")),
                Properties.columnProperty(integerAttribute("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void duplicateForeignKeyAttributes() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId", "tableName",
                Properties.primaryKeyProperty(integerAttribute("id")),
                Properties.columnProperty(stringAttribute("name")),
                Properties.foreignKeyProperty(entityAttribute("fkProperty"), null, "entityId",
                        Properties.columnProperty(integerAttribute("id"))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    final Attribute<Integer> name = attribute("name", Integer.class);
    final Attribute<String> info = attribute("info", String.class);
    final Attribute<String> derived = attribute("derived", String.class);
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty(attribute("id", Integer.class)),
                Properties.columnProperty(name),
                Properties.columnProperty(info),
                Properties.derivedProperty(derived, null, linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    Collection<DerivedProperty<?>> linked = definition.getDerivedProperties(name);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
    linked = definition.getDerivedProperties(info);
    assertTrue(linked.contains(definition.getProperty(derived)));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty(attribute("p0", Integer.class)).aggregateColumn(true),
                Properties.columnProperty(attribute("p1", Integer.class)).groupingColumn(true),
                Properties.columnProperty(attribute("p2", Integer.class)).groupingColumn(true));
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
                Properties.primaryKeyProperty(attribute("p0", Integer.class)).aggregateColumn(true),
                Properties.columnProperty(attribute("p1", Integer.class)).groupingColumn(true),
                Properties.columnProperty(attribute("p2", Integer.class)).groupingColumn(true)).groupByClause("p1, p2");
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
                Properties.primaryKeyProperty(attribute("p0", Integer.class))).havingClause(havingClause);
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
                Properties.primaryKeyProperty(attribute("p0", Integer.class))).havingClause(havingClause)
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
                Properties.columnProperty(attribute("attribute", Integer.class)));
        define(entityId2,
                Properties.foreignKeyProperty(entityAttribute("fk"), null, entityId1,
                        Properties.columnProperty(attribute("fk_col", Integer.class))));
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
                Properties.foreignKeyProperty(entityAttribute("fkAttribute"), "caption", "parent",
                        Properties.primaryKeyProperty(attribute("attribute", Integer.class))));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testAttributeConflict() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty(attribute("pk", Integer.class)),
                Properties.columnProperty(attribute("col", Integer.class)),
                Properties.columnProperty(attribute("col", Integer.class)));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testAttributeConflictInForeignKey() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty(attribute("pk", Integer.class)),
                Properties.columnProperty(attribute("col", Integer.class)),
                Properties.foreignKeyProperty(entityAttribute("fk"), "cap", "par",
                        Properties.columnProperty(attribute("col", Integer.class))));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final Attribute<Integer> attribute1 = attribute("1", Integer.class);
    final Attribute<Integer> attribute2 = attribute("2", Integer.class);
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityId",
                Properties.primaryKeyProperty(attribute("pk", Integer.class)),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(attribute("der", Integer.class), "cap", linkedValues -> null, attribute1, attribute2));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getDefinition("entityId");
    assertTrue(definition.hasDerivedProperties(attribute1));
    assertTrue(definition.hasDerivedProperties(attribute2));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    class TestDomain extends Domain {
      public TestDomain() {
        define("entity",
                Properties.primaryKeyProperty(attribute("attribute", Integer.class)))
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
    final Attribute<Integer> attribute = attribute("attribute", Integer.class);
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty(attribute));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity("entityToString");
    entity.put(attribute, 1);
    assertEquals("entityToString: attribute:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty(attribute("attribute", Integer.class))).stringProvider(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void setToStringProvider() {
    class TestDomain extends Domain {
      public TestDomain() {
        define("entityToString",
                Properties.primaryKeyProperty(attribute("attribute", Integer.class))).stringProvider(entity -> "test");

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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class)));
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class))).keyGenerator(null);
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
                Properties.primaryKeyProperty(attribute("attribute", Integer.class)))
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
