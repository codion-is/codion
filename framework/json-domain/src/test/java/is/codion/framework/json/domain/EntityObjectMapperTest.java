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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.condition.Condition;
import is.codion.framework.domain.entity.condition.CustomCondition;
import is.codion.framework.json.TestDomain;
import is.codion.framework.json.TestDomain.Department;
import is.codion.framework.json.TestDomain.Employee;
import is.codion.framework.json.TestDomain.TestEntity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityObjectMapperTest {

	private final Entities entities = new TestDomain().entities();
	private final EntityObjectMapper mapper = new EntityObjectMapper(entities);

	@Test
	void entity() throws JsonProcessingException {
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.NAME, "Name")
						.with(Department.LOCATION, "Location")
						.build();
		dept.set(Department.LOCATION, "New Location");
		byte[] logoBytes = new byte[20];
		new Random().nextBytes(logoBytes);
		dept.set(Department.LOGO, logoBytes);
		dept = dept.immutable();

		String jsonString = mapper.writeValueAsString(dept);
		Entity readDept = mapper.readValue(jsonString, Entity.class);
		assertTrue(dept.equalValues(readDept));

		Entity entity = entities.entity(TestEntity.TYPE)
						.with(TestEntity.DECIMAL, BigDecimal.valueOf(1234L))
						.with(TestEntity.DATE_TIME, LocalDateTime.now())
						.with(TestEntity.OFFSET_DATE_TIME, OffsetDateTime.now())
						.with(TestEntity.BLOB, logoBytes)
						.with(TestEntity.READ_ONLY, "readOnly")
						.with(TestEntity.BOOLEAN, true)
						.with(TestEntity.TIME, LocalTime.now())
						.with(TestEntity.ENTITY, dept)
						.build();

		jsonString = mapper.writeValueAsString(entity);

		assertTrue(entity.equalValues(mapper.readValue(jsonString, Entity.class)));

		entity.set(TestEntity.BOOLEAN, false);
		jsonString = mapper.writeValueAsString(entity);
		Entity entityModified = mapper.readValue(jsonString, Entity.class);
		assertTrue(entityModified.modified());
		assertTrue(entityModified.modified(TestEntity.BOOLEAN));
		assertFalse(entityModified.get(TestEntity.ENTITY).mutable());
	}

	@Test
	void entityForeignKeys() throws JsonProcessingException {
		EntityObjectMapper mapper = new EntityObjectMapper(entities).setIncludeForeignKeyValues(true);

		Entity dept = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.NAME, "Name")
						.with(Department.LOCATION, "Location")
						.with(Department.LOCATION, "New Location")
						.build();

		Entity emp = entities.entity(Employee.TYPE)
						.with(Employee.EMPNO, 2)
						.with(Employee.NAME, "Emp")
						.with(Employee.COMMISSION, 134.34)
						.with(Employee.DEPARTMENT_FK, dept)
						.with(Employee.HIREDATE, LocalDate.now())
						.build();

		String jsonString = mapper.writeValueAsString(emp);
		Entity readEmp = mapper.readValue(jsonString, Entity.class);
		assertTrue(emp.equalValues(readEmp));

		Entity readDept = readEmp.entity(Employee.DEPARTMENT_FK);
		assertTrue(dept.equalValues(readDept));
	}

	@Test
	void key() throws JsonProcessingException {
		Entity.Key deptKey1 = entities.primaryKey(Department.TYPE, 1);
		Entity.Key deptKey2 = entities.primaryKey(Department.TYPE, 2);

		String jsonString = mapper.writeValueAsString(asList(deptKey1, deptKey2));

		List<Entity.Key> keys = mapper.deserializeKeys(jsonString);
		assertEquals(Department.TYPE, keys.get(0).type());
		assertEquals(Integer.valueOf(1), keys.get(0).value());
		assertEquals(Integer.valueOf(2), keys.get(1).value());

		Entity.Key entityKey = entities.key(TestEntity.TYPE)
						.with(TestEntity.DECIMAL, BigDecimal.valueOf(1234L))
						.with(TestEntity.DATE_TIME, LocalDateTime.now())
						.build();

		jsonString = mapper.writeValueAsString(entityKey);

		Entity.Key readKey = mapper.readValue(jsonString, Entity.Key.class);

		assertEquals(entityKey, readKey);
	}

	@Test
	void keyOld() throws Exception {
		Entity.Key key = entities.primaryKey(Department.TYPE, 42);

		String keyJSON = mapper.writeValueAsString(singletonList(key));
		assertEquals("[{\"entityType\":\"employees.department\",\"values\":{\"deptno\":42}}]", keyJSON);
		Entity.Key keyParsed = mapper.deserializeKeys(keyJSON).get(0);
		assertEquals(key.type(), keyParsed.type());
		assertEquals(key.column(), keyParsed.column());
		assertEquals((Integer) key.value(), keyParsed.value());
	}

	@Test
	void entityOld() throws Exception {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate hiredate = LocalDate.parse("2001-12-20", format);

		Entity dept10 = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, -10)
						.with(Department.NAME, "DEPTNAME")
						.with(Department.LOCATION, "LOCATION")
						.build();

		String jsonString = mapper.writeValueAsString(singletonList(dept10));
		assertTrue(dept10.equalValues(mapper.deserializeEntities(jsonString).get(0)));

		Entity dept20 = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, -20)
						.with(Department.NAME, null)
						.with(Department.LOCATION, "ALOC")
						.build();

		jsonString = mapper.writeValueAsString(singletonList(dept20));
		assertTrue(dept20.equalValues(mapper.deserializeEntities(jsonString).get(0)));

		String twoDepts = mapper.writeValueAsString(asList(dept10, dept20));
		mapper.deserializeEntities(twoDepts);

		Entity mgr30 = entities.entity(Employee.TYPE)
						.with(Employee.COMMISSION, 500.5)
						.with(Employee.DEPARTMENT_FK, dept20)
						.with(Employee.HIREDATE, hiredate)
						.with(Employee.EMPNO, -30)
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "MGR NAME")
						.with(Employee.SALARY, BigDecimal.valueOf(2500.5))
						.build();

		Entity mgr50 = entities.entity(Employee.TYPE)
						.with(Employee.COMMISSION, 500.5)
						.with(Employee.DEPARTMENT_FK, dept20)
						.with(Employee.HIREDATE, hiredate)
						.with(Employee.EMPNO, -50)
						.with(Employee.JOB, "MANAGER")
						.with(Employee.NAME, "MGR2 NAME")
						.with(Employee.SALARY, BigDecimal.valueOf(2500.5))
						.build();

		Entity emp1 = entities.entity(Employee.TYPE)
						.with(Employee.COMMISSION, 500.5)
						.with(Employee.DEPARTMENT_FK, dept10)
						.with(Employee.HIREDATE, hiredate)
						.with(Employee.EMPNO, -500)
						.with(Employee.JOB, "CLERK")
						.with(Employee.MGR_FK, mgr30)
						.with(Employee.NAME, "A NAME")
						.with(Employee.SALARY, BigDecimal.valueOf(2500.55))
						.build();

		jsonString = mapper.writeValueAsString(singletonList(emp1));
		assertTrue(emp1.equalValues(mapper.deserializeEntities(jsonString).get(0)));

		mapper.setIncludeForeignKeyValues(true);

		jsonString = mapper.writeValueAsString(singletonList(emp1));
		Entity emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
		assertTrue(emp1.equalValues(emp1Deserialized));
		assertTrue(emp1.entity(Employee.DEPARTMENT_FK).equalValues(emp1Deserialized.entity(Employee.DEPARTMENT_FK)));
		assertTrue(emp1.entity(Employee.MGR_FK).equalValues(emp1Deserialized.entity(Employee.MGR_FK)));

		LocalDate newHiredate = LocalDate.parse("2002-11-21", format);
		emp1.set(Employee.COMMISSION, 550.55);
		emp1.set(Employee.DEPARTMENT_FK, dept20);
		emp1.set(Employee.JOB, "ANALYST");
		emp1.set(Employee.MGR_FK, mgr50);
		emp1.set(Employee.NAME, "ANOTHER NAME");
		emp1.set(Employee.SALARY, BigDecimal.valueOf(3500.5));
		emp1.set(Employee.HIREDATE, newHiredate);

		jsonString = mapper.writeValueAsString(singletonList(emp1));
		emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
		assertTrue(emp1.equalValues(emp1Deserialized));

		assertEquals(500.5, emp1Deserialized.original(Employee.COMMISSION));
		assertEquals(dept10, emp1Deserialized.original(Employee.DEPARTMENT_FK));
		assertEquals("CLERK", emp1Deserialized.original(Employee.JOB));
		assertEquals(mgr30, emp1Deserialized.original(Employee.MGR_FK));
		assertEquals(hiredate, emp1Deserialized.original(Employee.HIREDATE));
		assertEquals("A NAME", emp1Deserialized.original(Employee.NAME));
		assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.original(Employee.SALARY));

		assertTrue(emp1Deserialized.original(Employee.DEPARTMENT_FK).equalValues(dept10));
		assertTrue(emp1Deserialized.original(Employee.MGR_FK).equalValues(mgr30));

		Entity emp2 = entities.entity(Employee.TYPE)
						.with(Employee.COMMISSION, 300.5)
						.with(Employee.DEPARTMENT_FK, dept10)
						.with(Employee.HIREDATE, hiredate)
						.with(Employee.EMPNO, -200)
						.with(Employee.JOB, "CLERK")
						.with(Employee.MGR_FK, mgr50)
						.with(Employee.NAME, "NAME")
						.with(Employee.SALARY, BigDecimal.valueOf(3500.5))
						.build();

		mapper.setIncludeForeignKeyValues(true);

		List<Entity> entityList = asList(emp1, emp2);
		jsonString = mapper.writeValueAsString(entityList);
		List<Entity> parsedEntities = mapper.deserializeEntities(jsonString);
		for (Entity entity : entityList) {
			Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
			assertTrue(parsed.equalValues(entity));
		}

		List<Entity> readEntities = mapper.deserializeEntities(mapper.writeValueAsString(singletonList(emp1)));
		assertEquals(1, readEntities.size());
		Entity parsedEntity = readEntities.iterator().next();
		assertTrue(emp1.equalValues(parsedEntity));
		assertTrue(parsedEntity.modified());
		assertTrue(parsedEntity.modified(Employee.COMMISSION));
		assertTrue(parsedEntity.modified(Employee.DEPARTMENT));
		assertTrue(parsedEntity.modified(Employee.JOB));
		assertTrue(parsedEntity.modified(Employee.MGR));
		assertTrue(parsedEntity.modified(Employee.NAME));
		assertTrue(parsedEntity.modified(Employee.SALARY));
		assertTrue(parsedEntity.modified(Employee.HIREDATE));

		Entity emp3 = entities.entity(Employee.TYPE)
						.with(Employee.COMMISSION, 300.5)
						.with(Employee.DEPARTMENT_FK, dept10)
						.with(Employee.HIREDATE, null)
						.with(Employee.EMPNO, -200)
						.with(Employee.JOB, "CLERK")
						.with(Employee.MGR_FK, mgr50)
						.with(Employee.NAME, "NAME")
						.with(Employee.SALARY, null)
						.build();

		mapper.setIncludeForeignKeyValues(false);
		mapper.setIncludeNullValues(false);

		Entity emp3Parsed = mapper.deserializeEntities(mapper.writeValueAsString(singletonList(emp3))).get(0);
		assertFalse(emp3Parsed.contains(Employee.HIREDATE));
		assertFalse(emp3Parsed.contains(Employee.SALARY));
	}

	@Test
	void dependencyMap() throws JsonProcessingException {
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.with(Department.NAME, "Name")
						.with(Department.LOCATION, "Location")
						.with(Department.LOCATION, "New Location")
						.build();

		Map<String, Collection<Entity>> map = new HashMap<>();

		map.put(Department.TYPE.name(), singletonList(dept));

		String string = mapper.writeValueAsString(map);

		mapper.readValue(string, new TypeReference<Map<String, Collection<Entity>>>() {});
	}

	@Test
	void customSerializer() throws JsonProcessingException {
		EntityObjectMapper mapper = new CustomEntityObjectMapperFactory().entityObjectMapper(entities);

		Custom custom = new Custom("a value");
		assertEquals(custom.value, mapper.readValue(mapper.writeValueAsString(custom), Custom.class).value);
	}

	@Test
	void condition() throws JsonProcessingException {
		Entity dept1 = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 1)
						.build();
		Entity dept2 = entities.entity(Department.TYPE)
						.with(Department.DEPTNO, 2)
						.build();

		Condition condition = and(
						Employee.DEPARTMENT_FK.notIn(dept1, dept2),
						Employee.NAME.equalToIgnoreCase("Loc"),
						Employee.EMPNO.between(10, 40),
						Employee.COMMISSION.isNotNull());

		String jsonString = mapper.writeValueAsString(condition);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);

		assertEquals(condition, readCondition);
		assertEquals("(deptno NOT IN (?, ?) AND UPPER(ename) = UPPER(?) AND (empno >= ? AND empno <= ?) AND comm IS NOT NULL)",
						condition.toString(entities.definition(Employee.TYPE)));
	}

	@Test
	void nullCondition() throws JsonProcessingException {
		Condition condition = Employee.COMMISSION.isNotNull();

		String jsonString = mapper.writeValueAsString(condition);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);

		assertEquals(condition.entityType(), readCondition.entityType());
		assertEquals(condition.columns(), readCondition.columns());
		assertEquals(condition.values(), readCondition.values());
	}

	@Test
	void custom() throws JsonProcessingException {
		CustomCondition customedCondition = TestEntity.CONDITION_TYPE.get(
						asList(TestEntity.DECIMAL, TestEntity.DATE_TIME),
						asList(BigDecimal.valueOf(123.4), LocalDateTime.now()));

		String jsonString = mapper.writeValueAsString(customedCondition);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);
		CustomCondition readCustom = (CustomCondition) readCondition;

		assertEquals(customedCondition.conditionType(), readCustom.conditionType());
		assertEquals(customedCondition.columns(), readCustom.columns());
		assertEquals(customedCondition.values(), readCustom.values());
	}

	@Test
	void allCondition() throws JsonProcessingException {
		Condition condition = Condition.all(Department.TYPE);

		String jsonString = mapper.writeValueAsString(condition);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);

		assertEquals(condition, readCondition);
	}

	@Test
	void combinationOfCombinations() throws JsonProcessingException {
		Condition select = and(
						Employee.COMMISSION.equalTo(100d),
						or(Employee.JOB.notEqualTo("test"),
										Employee.JOB.isNotNull()));

		String jsonString = mapper.writeValueAsString(select);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);

		assertEquals(select, readCondition);
	}

	@Test
	void inCondition() throws JsonProcessingException {
		Condition select = and(
						Employee.COMMISSION.in(100d, 200d),
						Employee.JOB.notInIgnoreCase("test"));

		String jsonString = mapper.writeValueAsString(select);
		Condition readCondition = mapper.readValue(jsonString, Condition.class);

		assertEquals(select, readCondition);
	}
}
