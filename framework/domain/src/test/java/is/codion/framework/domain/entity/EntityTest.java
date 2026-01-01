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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.entity.Entity.Key;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityTest {

	private final Entities entities = new TestDomain().entities();

	@Test
	void equal() {
		Entity department1 = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.with(Department.NAME, "name")
						.with(Department.LOCATION, "loc")
						.build();

		Entity department2 = entities.entity(Department.TYPE)
						.with(Department.ID, 2)
						.with(Department.NAME, "name")
						.with(Department.LOCATION, "loc")
						.build();

		assertFalse(department1.equalValues(department2, asList(Department.ID, Department.NAME, Department.LOCATION)));
		assertTrue(department1.equalValues(department2, asList(Department.NAME, Department.LOCATION)));
		department2.remove(Department.LOCATION);
		assertFalse(department1.equalValues(department2, asList(Department.NAME, Department.LOCATION)));
		department1.remove(Department.LOCATION);
		assertTrue(department1.equalValues(department2, asList(Department.NAME, Department.LOCATION)));

		Entity employee = entities.entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "name")
						.build();

		assertThrows(IllegalArgumentException.class, () -> department1.equalValues(employee));
	}

	@Test
	void values() {
		List<Entity> entityList = new ArrayList<>();
		List<Object> values = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			entityList.add(entities.entity(Department.TYPE)
							.with(Department.ID, i == 5 ? null : i)
							.build());
			if (i != 5) {
				values.add(i);
			}
		}
		Collection<Integer> attributeValues = Entity.values(Department.ID, entityList);
		assertTrue(attributeValues.containsAll(values));
		assertTrue(Entity.values(Department.ID, emptyList()).isEmpty());
	}

	@Test
	void distinct() {
		List<Entity> entityList = new ArrayList<>();
		List<Object> values = new ArrayList<>();

		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, null)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 2)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 3)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 3)
						.build());
		entityList.add(entities.entity(Department.TYPE)
						.with(Department.ID, 4)
						.build());

		values.add(1);
		values.add(2);
		values.add(3);
		values.add(4);

		Collection<Integer> attributeValues = Entity.distinct(Department.ID, entityList);
		assertEquals(4, attributeValues.size());
		assertTrue(attributeValues.containsAll(values));
	}

	@Test
	void originalPrimaryKeys() {
		Entity dept1 = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		Entity dept2 = entities.entity(Department.TYPE)
						.with(Department.ID, 2)
						.build();
		dept1.set(Department.ID, 3);
		dept2.set(Department.ID, 4);

		Collection<Key> originalPrimaryKeys = Entity.originalPrimaryKeys(asList(dept1, dept2));
		assertTrue(originalPrimaryKeys.contains(entities.primaryKey(Department.TYPE, 1)));
		assertTrue(originalPrimaryKeys.contains(entities.primaryKey(Department.TYPE, 2)));
	}

	@Test
	void primaryKeyMap() {
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		Entity emp = entities.entity(Employee.TYPE)
						.with(Employee.ID, 3)
						.build();
		Entity emp2 = entities.entity(Employee.TYPE)
						.with(Employee.ID, null)
						.build();
		Map<Key, Entity> entityMap = Entity.primaryKeyMap(asList(dept, emp, emp2));
		assertSame(dept, entityMap.get(dept.primaryKey()));
		assertSame(emp, entityMap.get(emp.primaryKey()));
		assertSame(emp2, entityMap.get(emp2.primaryKey()));

		Entity dept2 = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		assertThrows(IllegalArgumentException.class, () -> Entity.primaryKeyMap(asList(dept, dept2, emp)));
		assertThrows(IllegalArgumentException.class, () -> Entity.primaryKeyMap(asList(emp2, emp2)));
	}

	@Test
	void groupKeysByType() {
		Key dept = entities.primaryKey(Department.TYPE, 1);
		Key emp = entities.primaryKey(Employee.TYPE, 3);

		LinkedHashMap<EntityType, List<Key>> mapped = Key.groupByType(asList(dept, emp));
		assertEquals(dept, mapped.get(Department.TYPE).get(0));
		assertEquals(emp, mapped.get(Employee.TYPE).get(0));
	}

	@Test
	void groupByValue() {
		List<Entity> entityList = new ArrayList<>();

		Entity entityOne = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		entityList.add(entityOne);

		Entity entityTwo = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		entityList.add(entityTwo);

		Entity entityThree = entities.entity(Department.TYPE)
						.with(Department.ID, 2)
						.build();
		entityList.add(entityThree);

		Entity entityFour = entities.entity(Department.TYPE)
						.with(Department.ID, 3)
						.build();
		entityList.add(entityFour);

		Entity entityFive = entities.entity(Department.TYPE)
						.with(Department.ID, 3)
						.build();
		entityList.add(entityFive);

		Entity entitySix = entities.entity(Department.TYPE)
						.with(Department.ID, null)
						.build();
		entityList.add(entitySix);

		Map<Integer, List<Entity>> map = Entity.groupByValue(Department.ID, entityList);
		Collection<Entity> ones = map.get(1);
		assertTrue(ones.contains(entityOne));
		assertTrue(ones.contains(entityTwo));

		Collection<Entity> twos = map.get(2);
		assertTrue(twos.contains(entityThree));

		Collection<Entity> threes = map.get(3);
		assertTrue(threes.contains(entityFour));
		assertTrue(threes.contains(entityFive));

		Collection<Entity> nulls = map.get(null);
		assertTrue(nulls.contains(entitySix));
	}

	@Test
	void groupByType() {
		Entity one = entities.entity(Employee.TYPE).build();
		Entity two = entities.entity(Department.TYPE).build();
		Entity three = entities.entity(Detail.TYPE).build();
		Entity four = entities.entity(Employee.TYPE).build();

		Collection<Entity> entities = asList(one, two, three, four);
		Map<EntityType, List<Entity>> map = Entity.groupByType(entities);

		Collection<Entity> mapped = map.get(Employee.TYPE);
		assertTrue(mapped.contains(one));
		assertTrue(mapped.contains(four));

		mapped = map.get(Department.TYPE);
		assertTrue(mapped.contains(two));

		mapped = map.get(Detail.TYPE);
		assertTrue(mapped.contains(three));
	}

	@Test
	void setNull() {
		Entity dept = entities.entity(Department.TYPE).build();
		for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributes().definitions()) {
			if (!definition.attribute().equals(Department.ID)) {
				assertFalse(dept.contains(definition.attribute()));
			}
			assertTrue(dept.isNull(definition.attribute()));
		}
		for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributes().definitions()) {
			dept.set(definition.attribute(), null);
		}
		//putting nulls should not have an effect
		assertFalse(dept.modified());
		for (AttributeDefinition<?> definition : entities.definition(Department.TYPE).attributes().definitions()) {
			assertTrue(dept.contains(definition.attribute()));
			assertTrue(dept.isNull(definition.attribute()));
		}
	}

	@Test
	void referencedKeys() {
		Entity dept1 = entities.entity(Department.TYPE)
						.with(Department.ID, 1)
						.build();
		Entity dept2 = entities.entity(Department.TYPE)
						.with(Department.ID, 2)
						.build();

		Entity emp1 = entities.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept1)
						.build();
		Entity emp2 = entities.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept1)
						.build();
		Entity emp3 = entities.entity(Employee.TYPE)
						.with(Employee.DEPARTMENT_FK, dept2)
						.build();
		Entity emp4 = entities.entity(Employee.TYPE)
						.build();

		Collection<Key> referencedKeys = Entity.keys(Employee.DEPARTMENT_FK, asList(emp1, emp2, emp3, emp4));
		assertEquals(2, referencedKeys.size());
		referencedKeys.forEach(key -> assertEquals(Department.TYPE, key.type()));
		Collection<Integer> values = Entity.values(new ArrayList<>(referencedKeys));
		assertTrue(values.contains(1));
		assertTrue(values.contains(2));
		assertFalse(values.contains(3));
	}

	@Test
	void noPkEntity() {
		Entity noPk = entities.entity(NoPk.TYPE)
						.with(NoPk.COL1, 1)
						.with(NoPk.COL2, 2)
						.with(NoPk.COL3, 3)
						.build();
		Collection<Key> keys = Entity.primaryKeys(singletonList(noPk));
		assertFalse(keys.iterator().next().isNull());
		assertFalse(keys.iterator().next().primary());
	}
}
