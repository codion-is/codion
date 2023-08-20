/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.CompositeDetail;
import is.codion.framework.domain.TestDomain.CompositeMaster;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.query.SelectQuery;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  static final DomainType DOMAIN_TYPE = domainType("domainType");

  @Test
  void test() {
    EntityType entityType = DOMAIN_TYPE.entityType("test");
    Column<Integer> id = entityType.integerColumn("id");
    Column<String> name = entityType.stringColumn("name");
    Function<Entity, String> stringFactory = StringFactory.builder().value(name).build();
    Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(
                id.primaryKeyColumn(),
                name.column()
                        .groupBy(true))
                .tableName("tableName")
                .selectQuery(SelectQuery.builder()
                        .columns("*")
                        .from("dual")
                        .groupBy("name")
                        .build())
                .orderBy(OrderBy.descending(name))
                .readOnly(true)
                .selectTableName("selectTableName")
                .stringFactory(stringFactory)
                .comparator(comparator));
      }
    }
    Domain domain = new TestDomain();
    EntityDefinition definition = domain.entities().definition(entityType);
    assertEquals(entityType.name(), definition.toString());
    assertEquals(entityType, definition.type());
    assertEquals("tableName", definition.tableName());
    assertNotNull(definition.keyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertEquals("*", definition.selectQuery().columns());
    assertEquals("dual", definition.selectQuery().from());
    assertEquals("name", definition.selectQuery().groupBy());
    assertFalse(definition.isSmallDataset());
    assertTrue(definition.isReadOnly());
    assertEquals("selectTableName", definition.selectTableName());
    assertEquals(stringFactory, definition.stringFactory());
    assertEquals(comparator, definition.comparator());
  }

  @Test
  void selectAttributes() {
    Domain domain = new TestDomain();

    Collection<Attribute<?>> defaultSelectAttributes = domain.entities()
            .definition(Employee.TYPE).selectAttributes();
    assertTrue(defaultSelectAttributes.contains(Employee.ID));
    assertTrue(defaultSelectAttributes.contains(Employee.NAME));
    assertTrue(defaultSelectAttributes.contains(Employee.JOB));
    assertTrue(defaultSelectAttributes.contains(Employee.MGR));
    assertTrue(defaultSelectAttributes.contains(Employee.HIREDATE));
    assertTrue(defaultSelectAttributes.contains(Employee.SALARY));
    assertTrue(defaultSelectAttributes.contains(Employee.COMMISSION));
    assertTrue(defaultSelectAttributes.contains(Employee.DEPARTMENT_NO));
    assertTrue(defaultSelectAttributes.contains(Employee.DEPARTMENT_FK));
    assertTrue(defaultSelectAttributes.contains(Employee.MANAGER_FK));
    assertTrue(defaultSelectAttributes.contains(Employee.DATA));
    assertFalse(defaultSelectAttributes.contains(Employee.DEPARTMENT_LOCATION));
    assertFalse(defaultSelectAttributes.contains(Employee.DEPARTMENT_NAME));

    defaultSelectAttributes = domain.entities()
            .definition(Department.TYPE).selectAttributes();
    assertTrue(defaultSelectAttributes.contains(Department.NO));
    assertTrue(defaultSelectAttributes.contains(Department.NAME));
    assertTrue(defaultSelectAttributes.contains(Department.LOCATION));
    assertTrue(defaultSelectAttributes.contains(Department.ACTIVE));
    assertFalse(defaultSelectAttributes.contains(Department.DATA));

    defaultSelectAttributes = domain.entities()
            .definition(Detail.TYPE).selectAttributes();
    assertTrue(defaultSelectAttributes.contains(Detail.ID));
    assertTrue(defaultSelectAttributes.contains(Detail.INT));
    assertTrue(defaultSelectAttributes.contains(Detail.DOUBLE));
    assertFalse(defaultSelectAttributes.contains(Detail.STRING));
    assertTrue(defaultSelectAttributes.contains(Detail.TIMESTAMP));
    assertTrue(defaultSelectAttributes.contains(Detail.BOOLEAN));
    assertTrue(defaultSelectAttributes.contains(Detail.BOOLEAN_NULLABLE));
    assertTrue(defaultSelectAttributes.contains(Detail.MASTER_ID));
    assertTrue(defaultSelectAttributes.contains(Detail.MASTER_FK));
    assertFalse(defaultSelectAttributes.contains(Detail.MASTER_NAME));
    assertTrue(defaultSelectAttributes.contains(Detail.MASTER_CODE_NON_DENORM));
    assertFalse(defaultSelectAttributes.contains(Detail.MASTER_CODE));
    assertTrue(defaultSelectAttributes.contains(Detail.MASTER_VIA_CODE_FK));
    assertTrue(defaultSelectAttributes.contains(Detail.INT_VALUE_LIST));
    assertFalse(defaultSelectAttributes.contains(Detail.INT_DERIVED));
    assertFalse(defaultSelectAttributes.contains(Detail.BYTES));
  }

  @Test
  void entityWithoutAttributes() {
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        EntityType entityType = DOMAIN_TYPE.entityType("entityWithoutAttributes");
        add(entityType.define()
                .tableName("tableName"));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  void duplicateAttributes() {
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        EntityType entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
        add(entityType.define(
                entityType.integerColumn("id").primaryKeyColumn(),
                entityType.stringColumn("name").column(),
                entityType.integerColumn("id").column()));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  void testDerivedAttribute() {
    EntityType entityType = DOMAIN_TYPE.entityType("derivedAttribute");
    Column<Integer> name = entityType.integerColumn("name");
    Column<String> info = entityType.stringColumn("info");
    Attribute<String> derived = entityType.stringAttribute("derived");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(
                entityType.integerColumn("id").primaryKeyColumn(),
                name.column(),
                info.column(),
                derived.derivedAttribute(linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info)));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);
    Collection<Attribute<?>> linked = definition.derivedAttributes(name);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
    linked = definition.derivedAttributes(info);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
  }

  @Test
  void testGroupByColumns() {
    EntityType entityType = DOMAIN_TYPE.entityType("testGroupByColumns");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(
                entityType.integerColumn("p0").primaryKeyColumn().aggregate(true),
                entityType.integerColumn("p1").column().groupBy(true),
                entityType.integerColumn("p2").column().groupBy(true)));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);

    assertTrue(definition.columnDefinition((Column<?>) definition.attribute("p0")).isAggregate());
    assertFalse(definition.columnDefinition((Column<?>) definition.attribute("p0")).isGroupBy());

    assertFalse(definition.columnDefinition((Column<?>) definition.attribute("p1")).isAggregate());
    assertTrue(definition.columnDefinition((Column<?>) definition.attribute("p1")).isGroupBy());

    assertFalse(definition.columnDefinition((Column<?>) definition.attribute("p2")).isAggregate());
    assertTrue(definition.columnDefinition((Column<?>) definition.attribute("p2")).isGroupBy());
  }

  @Test
  void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("p0").primaryKeyColumn())
                .selectQuery(SelectQuery.builder()
                        .having(havingClause)
                        .build()));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);
    assertEquals(havingClause, definition.selectQuery().having());
  }

  @Test
  void testForeignKeyNullability() {
    Domain domain = new TestDomain();
    assertFalse(domain.entities().definition(CompositeDetail.TYPE).foreignKeyDefinition(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK).isNullable());
    assertTrue(domain.entities().definition(Detail.TYPE).foreignKeyDefinition(Detail.MASTER_FK).isNullable());
  }

  @Test
  void testForeignPrimaryKey() {
    EntityType parent = DOMAIN_TYPE.entityType("parent");
    EntityType entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
    Column<Integer> integerColumn = entityType.integerColumn("column");
    ForeignKey foreignKey = entityType.foreignKey("fkColumn", integerColumn, parent.integerColumn("test"));
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        setStrictForeignKeys(false);
        add(entityType.define(
                integerColumn.primaryKeyColumn(),
                foreignKey.foreignKey()
                        .caption("caption")));
        setStrictForeignKeys(true);
      }
    }
    new TestDomain();
  }

  @Test
  void testAttributeConflict() {
    EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        Column<Integer> integerColumn = entityType.integerColumn("col");
        add(entityType.define(
                entityType.integerColumn("pk").primaryKeyColumn(),
                integerColumn.column(),
                integerColumn.column()));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  void testLinkedAttributes() {
    EntityType entityType = DOMAIN_TYPE.entityType("testLinkedAttributes");
    Column<Integer> pk = entityType.integerColumn("pk");
    Column<Integer> column1 = entityType.integerColumn("1");
    Column<Integer> column2 = entityType.integerColumn("2");
    Attribute<Integer> der = entityType.integerAttribute("der");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(
                pk.primaryKeyColumn(),
                column1.column(),
                column2.column(),
                der.derivedAttribute(linkedValues -> null, column1, column2)));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);
    assertTrue(definition.hasDerivedAttributes(column1));
    assertTrue(definition.hasDerivedAttributes(column2));
  }

  @Test
  void color() {
    final String colorBlue = "blue";
    final String colorYellow = "blue";
    EntityType entityType = DOMAIN_TYPE.entityType("getColor");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .backgroundColorProvider((entity1, attribute) -> colorBlue)
                .foregroundColorProvider((entity1, attribute) -> colorYellow));
      }
    }
    Entities entities = new TestDomain().entities();

    Entity entity = entities.entity(entityType);
    EntityDefinition definition = entities.definition(entityType);
    assertEquals(colorBlue, definition.backgroundColorProvider().color(entity, entity.primaryKey().column()));
    assertEquals(colorYellow, definition.foregroundColorProvider().color(entity, entity.primaryKey().column()));
  }

  @Test
  void testDefaultStringProvider() {
    EntityType entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
    Column<Integer> attribute = entityType.integerColumn("attribute");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(attribute.primaryKeyColumn()));
      }
    }
    Entities entities = new TestDomain().entities();

    Entity entity = entities.entity(entityType);
    entity.put(attribute, 1);
    assertEquals("testDefaultStringProvider: attribute: 1", entity.toString());
  }

  @Test
  void nullStringFactory() {
    EntityType entityType = DOMAIN_TYPE.entityType("nullStringFactory");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .stringFactory((Function<Entity, String>) null));
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  void stringFactory() {
    EntityType entityType = DOMAIN_TYPE.entityType("stringFactory");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .stringFactory(entity -> "test"));
      }
    }
    Entities entities = new TestDomain().entities();

    Entity entity = entities.entity(entityType);
    assertEquals("test", entity.toString());
  }

  @Test
  void defaultKeyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn()));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);
    assertNotNull(definition.keyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertTrue(definition.keyGenerator().isInserted());
  }

  @Test
  void nullKeyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .keyGenerator(null));
      }
    }
    assertThrows(NullPointerException.class, () -> new TestDomain());
  }

  @Test
  void keyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("keyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .keyGenerator(automatic("table")));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);
    assertNotNull(definition.keyGenerator());
    assertTrue(definition.isKeyGenerated());
    assertFalse(definition.keyGenerator().isInserted());
  }

  @Test
  void compositeKeySingleValueConstructor() {
    assertThrows(IllegalStateException.class, () -> new TestDomain().entities()
            .definition(CompositeMaster.TYPE).primaryKey(1L));
  }

  @Test
  void singleValueConstructorWrongType() {
    assertThrows(IllegalArgumentException.class, () -> new TestDomain().entities()
            .definition(Department.TYPE).primaryKey(1L));
  }

  @Test
  void keyGeneratorWithoutPrimaryKey() {
    EntityType entityType = DOMAIN_TYPE.entityType("keyGeneratorWithoutPrimaryKey");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").column())
                .keyGenerator(KeyGenerator.queried("select 1")));
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  void i18n() throws IOException, ClassNotFoundException {
    EntityType entityType = DOMAIN_TYPE.entityType("i18n", DefaultEntityDefinitionTest.class.getName());
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType.define(entityType.integerColumn("attribute").primaryKeyColumn())
                .captionResourceKey("test"));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.entities().definition(entityType);

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", definition.caption());

    definition = Serializer.deserialize(Serializer.serialize(definition));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", definition.caption());
  }

  @Test
  void entityTypeMismatch() {
    EntityType entityType1 = DOMAIN_TYPE.entityType("mismatch1");
    EntityType entityType2 = DOMAIN_TYPE.entityType("mismatch2");

    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        add(entityType1.define(entityType2.integerColumn("attribute").primaryKeyColumn()));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());

    class TestDomain2 extends DefaultDomain {
      public TestDomain2() {
        super(DOMAIN_TYPE);
        add(entityType1.define(
                entityType1.integerColumn("attribute").primaryKeyColumn(),
                entityType2.integerColumn("attribute").column()));
      }
    }

    assertThrows(IllegalArgumentException.class, () -> new TestDomain2());
  }
}
