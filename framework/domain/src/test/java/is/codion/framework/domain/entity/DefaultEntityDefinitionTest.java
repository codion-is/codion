/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.entity.query.SelectQuery;
import is.codion.framework.domain.property.Properties;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.automatic;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityDefinitionTest {

  static final DomainType DOMAIN_TYPE = domainType("domainType");

  @Test
  void test() {
    EntityType entityType = DOMAIN_TYPE.entityType("test");
    Attribute<Integer> id = entityType.integerAttribute("id");
    Attribute<String> name = entityType.stringAttribute("name");
    Function<Entity, String> stringFactory = StringFactory.stringFactory(name);
    Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName",
                primaryKeyProperty(id),
                Properties.columnProperty(name)
                        .groupingColumn(true))
                .selectQuery(SelectQuery.builder()
                        .columns("*")
                        .from("dual")
                        .groupBy("name")
                        .build())
                .orderBy(orderBy().descending(name))
                .readOnly(true)
                .selectTableName("selectTableName")
                .stringFactory(stringFactory)
                .comparator(comparator);
      }
    }
    Domain domain = new TestDomain();
    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals(entityType.getName(), definition.toString());
    assertEquals(entityType, definition.getEntityType());
    assertEquals("tableName", definition.getTableName());
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertEquals("*", definition.getSelectQuery().getColumns());
    assertEquals("dual", definition.getSelectQuery().getFrom());
    assertEquals("name", definition.getSelectQuery().getGroupBy());
    assertFalse(definition.isSmallDataset());
    assertTrue(definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("name", definition.getGroupByClause());
    assertEquals(stringFactory, definition.getStringFactory());
    assertEquals(comparator, definition.getComparator());
  }

  @Test
  void defaultSelectAttributes() {
    Domain domain = new TestDomain();

    Collection<Attribute<?>> defaultSelectAttributes = domain.getEntities()
            .getDefinition(Employee.TYPE).getDefaultSelectAttributes();
    assertTrue(defaultSelectAttributes.contains(Employee.ID));
    assertTrue(defaultSelectAttributes.contains(Employee.NAME));
    assertTrue(defaultSelectAttributes.contains(Employee.JOB));
    assertTrue(defaultSelectAttributes.contains(Employee.MGR));
    assertTrue(defaultSelectAttributes.contains(Employee.HIREDATE));
    assertTrue(defaultSelectAttributes.contains(Employee.SALARY));
    assertTrue(defaultSelectAttributes.contains(Employee.COMMISSION));
    assertTrue(defaultSelectAttributes.contains(Employee.DEPARTMENT));
    assertTrue(defaultSelectAttributes.contains(Employee.DEPARTMENT_FK));
    assertTrue(defaultSelectAttributes.contains(Employee.MANAGER_FK));
    assertTrue(defaultSelectAttributes.contains(Employee.DATA));
    assertFalse(defaultSelectAttributes.contains(Employee.DEPARTMENT_LOCATION));
    assertFalse(defaultSelectAttributes.contains(Employee.DEPARTMENT_NAME));

    defaultSelectAttributes = domain.getEntities()
            .getDefinition(Department.TYPE).getDefaultSelectAttributes();
    assertTrue(defaultSelectAttributes.contains(Department.NO));
    assertTrue(defaultSelectAttributes.contains(Department.NAME));
    assertTrue(defaultSelectAttributes.contains(Department.LOCATION));
    assertTrue(defaultSelectAttributes.contains(Department.ACTIVE));
    assertFalse(defaultSelectAttributes.contains(Department.DATA));

    defaultSelectAttributes = domain.getEntities()
            .getDefinition(Detail.TYPE).getDefaultSelectAttributes();
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
    assertTrue(defaultSelectAttributes.contains(Detail.MASTER_CODE_DENORM));
    assertFalse(defaultSelectAttributes.contains(Detail.BYTES));
  }

  @Test
  void entityWithDefaultValues() {
    EntityType entityType = DOMAIN_TYPE.entityType("entityWithDefaultValues");
    Attribute<Integer> id = entityType.integerAttribute("id");
    Attribute<String> name = entityType.stringAttribute("name");
    Attribute<Integer> value = entityType.integerAttribute("value");

    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName",
                primaryKeyProperty(id),
                Properties.columnProperty(name)
                        .defaultValue("DefName"),
                Properties.columnProperty(value)
                        .defaultValue(42));
      }
    }
    Entity entity = new TestDomain().getEntities().getDefinition(entityType).entityWithDefaultValues();
    assertTrue(entity.isNull(id));
    assertEquals("DefName", entity.get(name));
    assertEquals(42, entity.get(value));
  }

  @Test
  void entityWithoutProperties() {
    EntityType entityType = DOMAIN_TYPE.entityType("entityWithoutProperties");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName");
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
        define(entityType, "tableName",
                primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(entityType.stringAttribute("name")),
                Properties.columnProperty(entityType.integerAttribute("id")));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  void testDerivedProperty() {
    EntityType entityType = DOMAIN_TYPE.entityType("derivedProperty");
    Attribute<Integer> name = entityType.integerAttribute("name");
    Attribute<String> info = entityType.stringAttribute("info");
    Attribute<String> derived = entityType.stringAttribute("derived");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("id")),
                Properties.columnProperty(name),
                Properties.columnProperty(info),
                Properties.derivedProperty(derived, linkedValues ->
                        linkedValues.get(name).toString() + linkedValues.get(info), name, info));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    Collection<Attribute<?>> linked = definition.getDerivedAttributes(name);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
    linked = definition.getDerivedAttributes(info);
    assertTrue(linked.contains(derived));
    assertEquals(1, linked.size());
  }

  @Test
  void testGroupingProperties() {
    EntityType entityType = DOMAIN_TYPE.entityType("testGroupingProperties");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(true),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn(true));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")))
                .selectQuery(SelectQuery.builder()
                        .having(havingClause)
                        .build());
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals(havingClause, definition.getSelectQuery().getHaving());
  }

  @Test
  void testForeignKeyNullability() {
    Domain domain = new TestDomain();
    assertFalse(domain.getEntities().getDefinition(TestDomain.T_COMPOSITE_DETAIL).getForeignKeyProperty(TestDomain.COMPOSITE_DETAIL_MASTER_FK).isNullable());
    assertTrue(domain.getEntities().getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.MASTER_FK).isNullable());
  }

  @Test
  void testForeignPrimaryKey() {
    EntityType parent = DOMAIN_TYPE.entityType("parent");
    EntityType entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
    Attribute<Integer> integerAttribute = entityType.integerAttribute("attribute");
    ForeignKey foreignKey = entityType.foreignKey("fkAttribute", integerAttribute, parent.integerAttribute("test"));
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
  void testAttributeConflict() {
    EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        Attribute<Integer> integerAttribute = entityType.integerAttribute("col");
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("pk")),
                Properties.columnProperty(integerAttribute),
                Properties.columnProperty(integerAttribute));
      }
    }
    assertThrows(IllegalArgumentException.class, () -> new TestDomain());
  }

  @Test
  void testLinkedProperties() {
    EntityType entityType = DOMAIN_TYPE.entityType("testLinkedProperties");
    Attribute<Integer> pk = entityType.integerAttribute("pk");
    Attribute<Integer> attribute1 = entityType.integerAttribute("1");
    Attribute<Integer> attribute2 = entityType.integerAttribute("2");
    Attribute<Integer> der = entityType.integerAttribute("der");
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
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertTrue(definition.hasDerivedAttributes(attribute1));
    assertTrue(definition.hasDerivedAttributes(attribute2));
  }

  @Test
  void getColor() {
    final String colorBlue = "blue";
    final String colorYellow = "blue";
    EntityType entityType = DOMAIN_TYPE.entityType("getColor");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")))
                .backgroundColorProvider((entity1, attribute) -> colorBlue)
                .foregroundColorProvider((entity1, attribute) -> colorYellow);
      }
    }
    Entities entities = new TestDomain().getEntities();

    Entity entity = entities.entity(entityType);
    EntityDefinition definition = entities.getDefinition(entityType);
    assertEquals(colorBlue, definition.getBackgroundColorProvider().getColor(entity, entity.getPrimaryKey().getAttribute()));
    assertEquals(colorYellow, definition.getForegroundColorProvider().getColor(entity, entity.getPrimaryKey().getAttribute()));
  }

  @Test
  void testDefaultStringProvider() {
    EntityType entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
    Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(attribute));
      }
    }
    Entities entities = new TestDomain().getEntities();

    Entity entity = entities.entity(entityType);
    entity.put(attribute, 1);
    assertEquals("testDefaultStringProvider: attribute:1", entity.toString());
  }

  @Test
  void nullStringProvider() {
    EntityType entityType = DOMAIN_TYPE.entityType("nullStringProvider");
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
  void stringProvider() {
    EntityType entityType = DOMAIN_TYPE.entityType("stringProvider");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute"))).stringFactory(entity -> "test");
      }
    }
    Entities entities = new TestDomain().getEntities();

    Entity entity = entities.entity(entityType);
    assertEquals("test", entity.toString());
  }

  @Test
  void defaultKeyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertFalse(definition.isKeyGenerated());
    assertTrue(definition.getKeyGenerator().isInserted());
  }

  @Test
  void nullKeyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
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
  void keyGenerator() {
    EntityType entityType = DOMAIN_TYPE.entityType("keyGenerator");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")))
                .keyGenerator(automatic("table"));
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertNotNull(definition.getKeyGenerator());
    assertTrue(definition.isKeyGenerated());
    assertFalse(definition.getKeyGenerator().isInserted());
  }

  @Test
  void compositeKeySingleValueConstructor() {
    assertThrows(IllegalStateException.class, () -> new TestDomain().getEntities()
            .getDefinition(TestDomain.T_COMPOSITE_MASTER).primaryKey(1L));
  }

  @Test
  void singleValueConstructorWrongType() {
    assertThrows(IllegalArgumentException.class, () -> new TestDomain().getEntities()
            .getDefinition(Department.TYPE).primaryKey(1L));
  }

  @Test
  void keyGeneratorWithoutPrimaryKey() {
    EntityType entityType = DOMAIN_TYPE.entityType("keyGeneratorWithoutPrimaryKey");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                columnProperty(entityType.integerAttribute("attribute")))
                .keyGenerator(KeyGenerator.queried("select 1"));
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
        define(entityType, primaryKeyProperty(entityType.integerAttribute("attribute")))
                .captionResourceKey("test");
      }
    }
    Domain domain = new TestDomain();

    EntityDefinition definition = domain.getEntities().getDefinition(entityType);

    Locale.setDefault(new Locale("en", "EN"));
    assertEquals("Test", definition.getCaption());

    definition = Serializer.deserialize(Serializer.serialize(definition));

    Locale.setDefault(new Locale("is", "IS"));
    assertEquals("Prufa", definition.getCaption());
  }

  private static final DomainType COMPATIBILITY = domainType("compatibility");

  interface Person {
    EntityType TYPE = COMPATIBILITY.entityType("test");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<Double> SALARY = TYPE.doubleAttribute("salary");
    Attribute<String> SALARY_FORMATTED = TYPE.stringAttribute("salary_formatted");
    Attribute<Boolean> ENABLED = TYPE.booleanAttribute("enabled");
  }

  interface PersonDifferentType {
    EntityType TYPE = COMPATIBILITY.entityType("test2");
    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<String> NAME = TYPE.stringAttribute("name");
    Attribute<BigDecimal> SALARY = TYPE.bigDecimalAttribute("salary");
    Attribute<String> SALARY_FORMATTED = TYPE.stringAttribute("salary_formatted");
    Attribute<Boolean> ENABLED = TYPE.booleanAttribute("enabled");
  }

  @Test
  void serializationCompatibility() {
    DefaultEntityDefinition definition = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY),
                    columnProperty(Person.ENABLED)
            ));

    DefaultEntityDefinition missingProperty = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY)
            ));

    DefaultEntityDefinition missingDerivedProperty = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    columnProperty(Person.ENABLED)
            ));

    DefaultEntityDefinition differentOrder = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY),
                    columnProperty(Person.ENABLED),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY)
            ));

    DefaultEntityDefinition differentType = new DefaultEntityDefinition(COMPATIBILITY.getName(), PersonDifferentType.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(PersonDifferentType.ID),
                    columnProperty(PersonDifferentType.NAME),
                    columnProperty(PersonDifferentType.SALARY),
                    derivedProperty(PersonDifferentType.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(PersonDifferentType.SALARY).toString(), PersonDifferentType.SALARY),
                    columnProperty(PersonDifferentType.ENABLED)
            ));

    assertNotEquals(definition.getSerializationVersion(), missingProperty.getSerializationVersion());
    assertEquals(definition.getSerializationVersion(), missingDerivedProperty.getSerializationVersion());
    assertNotEquals(definition.getSerializationVersion(), differentOrder.getSerializationVersion());
    assertNotEquals(missingProperty.getSerializationVersion(), differentOrder.getSerializationVersion());
    assertNotEquals(definition.getSerializationVersion(), differentType.getSerializationVersion());
  }
}
