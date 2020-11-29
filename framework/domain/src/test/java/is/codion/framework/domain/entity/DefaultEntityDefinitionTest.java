/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerators.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  static final DomainType DOMAIN_TYPE = domainType("domainType");

  @Test
  public void test() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("test");
    final Attribute<Integer> id = entityType.integerAttribute("id");
    final Attribute<String> name = entityType.stringAttribute("name");
    final StringFactory stringFactory = new StringFactory(name);
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName",
                primaryKeyProperty(id),
                Properties.columnProperty(name))
                .selectQuery("select * from dual", false)
                .orderBy(orderBy().descending(name))
                .readOnly(true).selectTableName("selectTableName").groupByClause("name")
                .stringFactory(stringFactory).comparator(comparator);
      }
    }
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
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
    assertEquals(stringFactory, definition.getStringProvider());
    assertEquals(comparator, definition.getComparator());
  }

  @Test
  public void entityWithoutProperties() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("entityWithoutProperties");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName");
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void entityWithValueProvider() {
    final Domain domain = new TestDomain();
    final EntityDefinition definition = domain.getEntities().getDefinition(Detail.TYPE);
    final Entity detail = definition.entity(property -> null);
    assertFalse(detail.containsValue(Detail.DOUBLE));//columnHasDefaultValue
    assertFalse(detail.containsValue(Detail.DATE));//columnHasDefaultValue
    assertTrue(detail.containsValue(Detail.BOOLEAN_NULLABLE));//columnHasDefaultValue && property.hasDefaultValue
  }

  @Test
  public void duplicateAttributes() {
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
        define(entityType, "tableName",
                primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(entityType.stringAttribute("name")),
                Properties.columnProperty(entityType.integerAttribute("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void derivedProperty() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("derivedProperty");
    final Attribute<Integer> name = entityType.integerAttribute("name");
    final Attribute<String> info = entityType.stringAttribute("info");
    final Attribute<String> derived = entityType.stringAttribute("derived");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(name),
                Properties.columnProperty(info),
                Properties.derivedProperty(derived, null, linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    Collection<Attribute<?>> linked = definition.getDerivedAttributes(name);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
    linked = definition.getDerivedAttributes(info);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
  }

  @Test
  public void testGroupingProperties() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testGroupingProperties");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn(true));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  public void testSetGroupByClauseWithGroupingProperties() {
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testSetGroupByClauseWithGroupingProperties");
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn(true)).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0"))).havingClause(havingClause);
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals(havingClause, definition.getHavingClause());
  }

  @Test
  public void testSetHavingClauseAlreadySet() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testSetHavingClauseAlreadySet");
    final String havingClause = "p1 > 1";
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0"))).havingClause(havingClause)
                .havingClause(havingClause);
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  public void testForeignKeyNullability() {
    final Domain domain = new TestDomain();
    assertFalse(domain.getEntities().getDefinition(TestDomain.T_COMPOSITE_DETAIL).getForeignKeyProperty(TestDomain.COMPOSITE_DETAIL_MASTER_FK).isNullable());
    assertTrue(domain.getEntities().getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.MASTER_FK).isNullable());
  }

  @Test
  public void testForeignPrimaryKey() {
    final EntityType<Entity> parent = DOMAIN_TYPE.entityType("parent");
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
    final Attribute<Integer> integerAttribute = entityType.integerAttribute("attribute");
    final ForeignKeyAttribute foreignKey = entityType.foreignKey("fkAttribute", integerAttribute, parent.integerAttribute("test"));
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        setStrictForeignKeys(false);
        define(entityType,
                primaryKeyProperty(integerAttribute),
                foreignKeyProperty(foreignKey, "caption"));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  public void testAttributeConflict() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        final Attribute<Integer> integerAttribute = entityType.integerAttribute("col");
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(integerAttribute),
                Properties.columnProperty(integerAttribute));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  public void testLinkedProperties() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testLinkedProperties");
    final Attribute<Integer> pk = entityType.integerAttribute("pk");
    final Attribute<Integer> attribute1 = entityType.integerAttribute("1");
    final Attribute<Integer> attribute2 = entityType.integerAttribute("2");
    final Attribute<Integer> der = entityType.integerAttribute("der");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(pk),
                Properties.columnProperty(attribute1),
                Properties.columnProperty(attribute2),
                Properties.derivedProperty(der, "cap", linkedValues -> null, attribute1, attribute2));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertTrue(definition.hasDerivedAttributes(attribute1));
    assertTrue(definition.hasDerivedAttributes(attribute2));
  }

  @Test
  public void getColor() {
    final String colorBlue = "blue";
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("getColor");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")))
                .colorProvider((entity1, attribute) -> colorBlue);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    final EntityDefinition definition = entities.getDefinition(entityType);
    assertEquals(colorBlue, definition.getColorProvider().getColor(entity, entity.getPrimaryKey().getAttribute()));
  }

  @Test
  void testDefaultStringProvider() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
    final Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(attribute));
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    entity.put(attribute, 1);
    assertEquals("testDefaultStringProvider: attribute:1", entity.toString());
  }

  @Test
  public void nullStringProvider() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("nullStringProvider");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute"))).stringFactory((Function<Entity, String>) null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void stringProvider() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("stringProvider");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute"))).stringFactory(entity -> "test");
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    assertEquals("test", entity.toString());
  }

  @Test
  public void defaultKeyGenerator() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertTrue(definition.getKeyGenerator().isInserted());
  }

  @Test
  public void nullKeyGenerator() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute"))).keyGenerator(null);
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  public void keyGenerator() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("keyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")))
                .keyGenerator(automatic("table"));
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertTrue(definition.isKeyGenerated());
    assertFalse(definition.getKeyGenerator().isInserted());
  }

  @Test
  public void compositeKeySingleValueConstructor() {
    assertThrows(IllegalStateException.class, () -> new TestDomain().getEntities()
            .getDefinition(TestDomain.T_COMPOSITE_MASTER).primaryKey(1L));
  }

  @Test
  public void singleValueConstructorWrongType() {
    assertThrows(IllegalArgumentException.class, () -> new TestDomain().getEntities()
            .getDefinition(TestDomain.Department.TYPE).primaryKey(1L));
  }

  @Test
  public void keyGeneratorWithoutPrimaryKey() {
    final EntityType<Entity> entityType = DOMAIN_TYPE.entityType("keyGeneratorWithoutPrimaryKey");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                columnProperty(entityType.integerAttribute("attribute")))
                .keyGenerator(KeyGenerators.queried("select 1"));
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }
}
