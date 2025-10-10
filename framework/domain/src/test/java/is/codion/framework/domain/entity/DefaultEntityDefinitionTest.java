/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.DomainModel;
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
import is.codion.framework.domain.entity.query.EntitySelectQuery;

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
		Function<Entity, String> formatter = EntityFormatter.builder().value(name).build();
		Comparator<Entity> comparator = (o1, o2) -> 0;
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(
												id.define()
																.primaryKey(),
												name.define()
																.column()
																.groupBy(true))
								.table("tableName")
								.selectQuery(EntitySelectQuery.builder()
												.columns("*")
												.from("dual")
												.groupBy("name")
												.build())
								.orderBy(OrderBy.descending(name))
								.readOnly(true)
								.selectTable("selectTableName")
								.formatter(formatter)
								.comparator(comparator)
								.build());
			}
		}
		Domain domain = new TestDomain();
		EntityDefinition definition = domain.entities().definition(entityType);
		assertEquals(entityType.name(), definition.toString());
		assertEquals(entityType, definition.type());
		assertEquals("tableName", definition.table());
		assertNotNull(definition.primaryKey().generator());
		assertFalse(definition.primaryKey().generated());
		EntitySelectQuery query = definition.selectQuery().orElseThrow(IllegalStateException::new);
		assertEquals("*", query.columns());
		assertEquals("dual", query.from());
		assertEquals("name", query.groupBy());
		assertFalse(definition.smallDataset());
		assertTrue(definition.readOnly());
		assertEquals("selectTableName", definition.selectTable());
		assertEquals(formatter, definition.formatter());
		assertEquals(comparator, definition.comparator());
	}

	@Test
	void selectAttributes() {
		Domain domain = new TestDomain();

		Collection<Attribute<?>> defaultSelectAttributes = domain.entities()
						.definition(Employee.TYPE).attributes().selected();
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
						.definition(Department.TYPE).attributes().selected();
		assertTrue(defaultSelectAttributes.contains(Department.ID));
		assertTrue(defaultSelectAttributes.contains(Department.NAME));
		assertTrue(defaultSelectAttributes.contains(Department.LOCATION));
		assertTrue(defaultSelectAttributes.contains(Department.ACTIVE));
		assertFalse(defaultSelectAttributes.contains(Department.DATA));

		defaultSelectAttributes = domain.entities()
						.definition(Detail.TYPE).attributes().selected();
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
		assertTrue(defaultSelectAttributes.contains(Detail.INT_ITEMS));
		assertFalse(defaultSelectAttributes.contains(Detail.INT_DERIVED));
		assertFalse(defaultSelectAttributes.contains(Detail.BYTES));
	}

	@Test
	void entityWithoutAttributes() {
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				EntityType entityType = DOMAIN_TYPE.entityType("entityWithoutAttributes");
				add(entityType.define()
								.table("tableName")
								.build());
			}
		}
		assertThrows(IllegalArgumentException.class, () -> new TestDomain());
	}

	@Test
	void duplicateAttributes() {
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				EntityType entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
				add(entityType.define(
												entityType.integerColumn("id").define().primaryKey(),
												entityType.stringColumn("name").define().column(),
												entityType.integerColumn("id").define().column())
								.build());
			}
		}
		assertThrows(IllegalArgumentException.class, () -> new TestDomain());

		class TestDomain2 extends DomainModel {
			public TestDomain2() {
				super(DOMAIN_TYPE);
				EntityType entityType = DOMAIN_TYPE.entityType("duplicateAttributes");
				add(entityType.define(
												entityType.integerColumn("id").define().primaryKey(),
												entityType.stringColumn("name").define().column(),
												entityType.integerAttribute("id").define().attribute())
								.build());
			}
		}
		assertThrows(IllegalArgumentException.class, () -> new TestDomain2());
	}

	@Test
	void testDerivedAttribute() {
		EntityType entityType = DOMAIN_TYPE.entityType("derivedAttribute");
		Column<Integer> name = entityType.integerColumn("name");
		Column<String> info = entityType.stringColumn("info");
		Attribute<String> derived = entityType.stringAttribute("derived");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(
												entityType.integerColumn("id").define().primaryKey(),
												name.define().column(),
												info.define().column(),
												derived.define()
																.derived()
																.from(name, info)
																.value(source ->
																				source.get(name).toString() + source.get(info)))
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);
		Collection<Attribute<?>> linked = definition.attributes().derivedFrom(name);
		assertTrue(linked.contains(derived));
		assertEquals(1, linked.size());
		linked = definition.attributes().derivedFrom(info);
		assertTrue(linked.contains(derived));
		assertEquals(1, linked.size());
	}

	@Test
	void invalidDerivedAttribute() {
		EntityType entityType = DOMAIN_TYPE.entityType("invalidDerivedAttribute");
		Column<Integer> name = entityType.integerColumn("name");
		Column<String> info = entityType.stringColumn("info");
		Attribute<String> derived = entityType.stringAttribute("derived");
		assertThrows(IllegalArgumentException.class, () -> entityType.define(
										entityType.integerColumn("id").define().primaryKey(),
										//name.define().column(), <- the problem
										info.define().column(),
										derived.define()
														.derived()
														.from(name, info)
														.value(source -> null))
						.build());
	}

	@Test
	void invalidForeignKey() {
		assertThrows(IllegalArgumentException.class, () -> Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey(),
//										Employee.DEPARTMENT_NO.define() <- the problem
//														.column(),
										Employee.DEPARTMENT_FK.define()
														.foreignKey())
						.build());
	}

	@Test
	void invalidDenormalizedAttribute() {
		Attribute<String> denormalized = Employee.TYPE.stringAttribute("denormalized");
		assertThrows(IllegalArgumentException.class, () -> Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey(),
										Employee.DEPARTMENT_NO.define()
														.column(),
										Employee.DEPARTMENT_FK.define()
														.foreignKey(),
										denormalized.define()
														.denormalized()
														.from(Employee.DEPARTMENT_FK)
														.attribute(Employee.JOB))// <- the problem
						.build());
	}

	@Test
	void testGroupByColumns() {
		EntityType entityType = DOMAIN_TYPE.entityType("testGroupByColumns");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(
												entityType.integerColumn("p0").define().primaryKey().aggregate(true),
												entityType.integerColumn("p1").define().column().groupBy(true),
												entityType.integerColumn("p2").define().column().groupBy(true))
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);

		assertTrue(definition.columns().definition((Column<?>) definition.attributes().get("p0")).aggregate());
		assertFalse(definition.columns().definition((Column<?>) definition.attributes().get("p0")).groupBy());

		assertFalse(definition.columns().definition((Column<?>) definition.attributes().get("p1")).aggregate());
		assertTrue(definition.columns().definition((Column<?>) definition.attributes().get("p1")).groupBy());

		assertFalse(definition.columns().definition((Column<?>) definition.attributes().get("p2")).aggregate());
		assertTrue(definition.columns().definition((Column<?>) definition.attributes().get("p2")).groupBy());
	}

	@Test
	void testSetHavingClause() {
		final String havingClause = "p1 > 1";
		EntityType entityType = DOMAIN_TYPE.entityType("testSetHavingClause");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("p0").define().primaryKey())
								.selectQuery(EntitySelectQuery.builder()
												.having(havingClause)
												.build())
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);
		assertEquals(havingClause, definition.selectQuery().orElseThrow(IllegalStateException::new).having());
	}

	@Test
	void testForeignKeyNullability() {
		Domain domain = new TestDomain();
		assertFalse(domain.entities().definition(CompositeDetail.TYPE)
						.foreignKeys().definition(CompositeDetail.COMPOSITE_DETAIL_MASTER_FK).nullable());
		assertTrue(domain.entities().definition(Detail.TYPE).foreignKeys().definition(Detail.MASTER_FK).nullable());
	}

	@Test
	void testForeignPrimaryKey() {
		EntityType parent = DOMAIN_TYPE.entityType("parent");
		EntityType entityType = DOMAIN_TYPE.entityType("testForeignPrimaryKey");
		Column<Integer> integerColumn = entityType.integerColumn("column");
		ForeignKey foreignKey = entityType.foreignKey("fkColumn", integerColumn, parent.integerColumn("test"));
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				validateForeignKeys(false);
				add(entityType.define(
												integerColumn.define().primaryKey(),
												foreignKey.define()
																.foreignKey()
																.caption("caption"))
								.build());
				validateForeignKeys(true);
			}
		}
		new TestDomain();
	}

	@Test
	void testAttributeConflict() {
		EntityType entityType = DOMAIN_TYPE.entityType("testAttributeConflict");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				Column<Integer> integerColumn = entityType.integerColumn("col");
				add(entityType.define(
												entityType.integerColumn("pk").define().primaryKey(),
												integerColumn.define().column(),
												integerColumn.define().column())
								.build());
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
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(
												pk.define().primaryKey(),
												column1.define().column(),
												column2.define().column(),
												der.define()
																.derived()
																.from(column1, column2)
																.value(source -> null))
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);
		assertFalse(definition.attributes().derivedFrom(column1).isEmpty());
		assertFalse(definition.attributes().derivedFrom(column2).isEmpty());
	}

	@Test
	void testDefaultStringProvider() {
		EntityType entityType = DOMAIN_TYPE.entityType("testDefaultStringProvider");
		Column<Integer> attribute = entityType.integerColumn("attribute");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(attribute.define().primaryKey()).build());
			}
		}
		Entities entities = new TestDomain().entities();

		Entity entity = entities.entity(entityType).build();
		entity.set(attribute, 1);
		assertEquals("testDefaultStringProvider: attribute: 1", entity.toString());
	}

	@Test
	void nullFormatter() {
		EntityType entityType = DOMAIN_TYPE.entityType("nullFormatter");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey())
								.formatter((Function<Entity, String>) null)
								.build());
			}
		}
		assertThrows(NullPointerException.class, () -> new TestDomain());
	}

	@Test
	void formatter() {
		EntityType entityType = DOMAIN_TYPE.entityType("formatter");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey())
								.formatter(entity -> "test")
								.build());
			}
		}
		Entities entities = new TestDomain().entities();

		Entity entity = entities.entity(entityType).build();
		assertEquals("test", entity.toString());
	}

	@Test
	void defaultKeyGenerator() {
		EntityType entityType = DOMAIN_TYPE.entityType("defaultKeyGenerator");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey()).build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);
		assertNotNull(definition.primaryKey().generator());
		assertFalse(definition.primaryKey().generated());
		assertTrue(definition.primaryKey().generator().inserted());
	}

	@Test
	void nullKeyGenerator() {
		EntityType entityType = DOMAIN_TYPE.entityType("nullKeyGenerator");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey())
								.keyGenerator(null)
								.build());
			}
		}
		assertThrows(NullPointerException.class, () -> new TestDomain());
	}

	@Test
	void keyGenerator() {
		EntityType entityType = DOMAIN_TYPE.entityType("keyGenerator");
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey())
								.keyGenerator(automatic("table"))
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);
		assertNotNull(definition.primaryKey().generator());
		assertTrue(definition.primaryKey().generated());
		assertFalse(definition.primaryKey().generator().inserted());
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
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().column())
								.keyGenerator(KeyGenerator.queried("select 1"))
								.build());
			}
		}
		assertThrows(IllegalStateException.class, () -> new TestDomain());
	}

	@Test
	void i18n() throws IOException, ClassNotFoundException {
		EntityType entityType = DOMAIN_TYPE.entityType("i18n", DefaultEntityDefinitionTest.class.getName());
		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType.define(entityType.integerColumn("attribute").define().primaryKey())
								.captionResourceKey("test")
								.descriptionResourceKey("test.description")
								.build());
			}
		}
		Domain domain = new TestDomain();

		EntityDefinition definition = domain.entities().definition(entityType);

		Locale.setDefault(new Locale("en", "EN"));
		assertEquals("Test", definition.caption());
		assertEquals("Description", definition.description().orElse(null));

		definition = Serializer.deserialize(Serializer.serialize(definition));

		Locale.setDefault(new Locale("is", "IS"));
		assertEquals("Prufa", definition.caption());
		assertEquals("Lysing", definition.description().orElse(null));
	}

	@Test
	void entityTypeMismatch() {
		EntityType entityType1 = DOMAIN_TYPE.entityType("mismatch1");
		EntityType entityType2 = DOMAIN_TYPE.entityType("mismatch2");

		class TestDomain extends DomainModel {
			public TestDomain() {
				super(DOMAIN_TYPE);
				add(entityType1.define(entityType2.integerColumn("attribute").define().primaryKey()).build());
			}
		}
		assertThrows(IllegalArgumentException.class, () -> new TestDomain());

		class TestDomain2 extends DomainModel {
			public TestDomain2() {
				super(DOMAIN_TYPE);
				add(entityType1.define(
												entityType1.integerColumn("attribute").define().primaryKey(),
												entityType2.integerColumn("attribute").define().column())
								.build());
			}
		}

		assertThrows(IllegalArgumentException.class, () -> new TestDomain2());
	}
}
