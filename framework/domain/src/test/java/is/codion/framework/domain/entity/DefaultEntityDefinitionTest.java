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
    final EntityType entityType = DOMAIN_TYPE.entityType("test");
    final Attribute<Integer> id = entityType.integerAttribute("id");
    final Attribute<String> name = entityType.stringAttribute("name");
    final Function<Entity, String> stringFactory = StringFactory.stringFactory(name).get();
    final Comparator<Entity> comparator = (o1, o2) -> 0;
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, "tableName",
                primaryKeyProperty(id),
                Properties.columnProperty(name))
                .selectQuery(SelectQuery.builder("dual").columns("*").build())
                .orderBy(orderBy().descending(name))
                .readOnly().selectTableName("selectTableName").groupByClause("name")
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
    assertEquals("*", definition.getSelectQuery().getColumns());
    assertEquals("dual", definition.getSelectQuery().getFrom());
    assertFalse(definition.isSmallDataset());
    assertTrue(definition.isReadOnly());
    assertEquals("selectTableName", definition.getSelectTableName());
    assertEquals("name", definition.getGroupByClause());
    assertEquals(stringFactory, definition.getStringFactory());
    assertEquals(comparator, definition.getComparator());
  }

  @Test
  void defaultSelectAttributes() {
    final Domain domain = new TestDomain();

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
    final EntityType entityType = DOMAIN_TYPE.entityType("entityWithDefaultValues");
    final Attribute<Integer> id = entityType.integerAttribute("id");
    final Attribute<String> name = entityType.stringAttribute("name");
    final Attribute<Integer> value = entityType.integerAttribute("value");

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
    final Entity entity = new TestDomain().getEntities().getDefinition(entityType).entityWithDefaultValues();
    assertTrue(entity.isNull(id));
    assertEquals("DefName", entity.get(name));
    assertEquals(42, entity.get(value));
  }

  @Test
  void entityWithoutProperties() {
    final EntityType entityType = DOMAIN_TYPE.entityType("entityWithoutProperties");
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
        final EntityType entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("derivedProperty");
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
                Properties.derivedProperty(derived, linkedValues ->
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
  void testGroupingProperties() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testGroupingProperties");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn());
      }
    }
    final Domain domain = new TestDomain();

    final EntityDefinition definition = domain.getEntities().getDefinition(entityType);
    assertEquals("p1, p2", definition.getGroupByClause());
  }

  @Test
  void testSetGroupByClauseWithGroupingProperties() {
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        final EntityType entityType = DOMAIN_TYPE.entityType("testSetGroupByClauseWithGroupingProperties");
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("p0")).aggregateColumn(),
                Properties.columnProperty(entityType.integerAttribute("p1")).groupingColumn(),
                Properties.columnProperty(entityType.integerAttribute("p2")).groupingColumn()).groupByClause("p1, p2");
      }
    }
    assertThrows(IllegalStateException.class, () -> new TestDomain());
  }

  @Test
  void testSetHavingClause() {
    final String havingClause = "p1 > 1";
    final EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
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
  void testSetHavingClauseAlreadySet() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClauseAlreadySet");
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
  void testForeignKeyNullability() {
    final Domain domain = new TestDomain();
    assertFalse(domain.getEntities().getDefinition(TestDomain.T_COMPOSITE_DETAIL).getForeignKeyProperty(TestDomain.COMPOSITE_DETAIL_MASTER_FK).isNullable());
    assertTrue(domain.getEntities().getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.MASTER_FK).isNullable());
  }

  @Test
  void testForeignPrimaryKey() {
    final EntityType parent = DOMAIN_TYPE.entityType("parent");
    final EntityType entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
    final Attribute<Integer> integerAttribute = entityType.integerAttribute("attribute");
    final ForeignKey foreignKey = entityType.foreignKey("fkAttribute", integerAttribute, parent.integerAttribute("test"));
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
    final EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
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
  void testLinkedProperties() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testLinkedProperties");
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
  void getColor() {
    final String colorBlue = "blue";
    final String colorYellow = "blue";
    final EntityType entityType = DOMAIN_TYPE.entityType("getColor");
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType,
                primaryKeyProperty(entityType.integerAttribute("attribute")))
                .backgroundColorProvider((entity1, attribute) -> colorBlue)
                .foregroundColorProvider((entity1, attribute) -> colorYellow);
      }
    }
    final Entities entities = new TestDomain().getEntities();

    final Entity entity = entities.entity(entityType);
    final EntityDefinition definition = entities.getDefinition(entityType);
    assertEquals(colorBlue, definition.getBackgroundColorProvider().getColor(entity, entity.getPrimaryKey().getAttribute()));
    assertEquals(colorYellow, definition.getForegroundColorProvider().getColor(entity, entity.getPrimaryKey().getAttribute()));
  }

  @Test
  void testDefaultStringProvider() {
    final EntityType entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
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
  void nullStringProvider() {
    final EntityType entityType = DOMAIN_TYPE.entityType("nullStringProvider");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("stringProvider");
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
  void defaultKeyGenerator() {
    final EntityType entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
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
  void nullKeyGenerator() {
    final EntityType entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("keyGenerator");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("keyGeneratorWithoutPrimaryKey");
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
    final EntityType entityType = DOMAIN_TYPE.entityType("i18n", DefaultEntityDefinitionTest.class.getName());
    class TestDomain extends DefaultDomain {
      public TestDomain() {
        super(DOMAIN_TYPE);
        define(entityType, primaryKeyProperty(entityType.integerAttribute("attribute")))
                .captionResourceKey("test");
      }
    }
    final Domain domain = new TestDomain();

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
    final DefaultEntityDefinition definition = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY),
                    columnProperty(Person.ENABLED)
            ));

    final DefaultEntityDefinition missingProperty = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY)
            ));

    final DefaultEntityDefinition missingDerivedProperty = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY),
                    columnProperty(Person.ENABLED)
            ));

    final DefaultEntityDefinition differentOrder = new DefaultEntityDefinition(COMPATIBILITY.getName(), Person.TYPE, Person.TYPE.getName(),
            Arrays.asList(
                    primaryKeyProperty(Person.ID),
                    derivedProperty(Person.SALARY_FORMATTED, sourceValues ->
                            sourceValues.get(Person.SALARY).toString(), Person.SALARY),
                    columnProperty(Person.ENABLED),
                    columnProperty(Person.NAME),
                    columnProperty(Person.SALARY)
            ));

    final DefaultEntityDefinition differentType = new DefaultEntityDefinition(COMPATIBILITY.getName(), PersonDifferentType.TYPE, Person.TYPE.getName(),
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
