/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.TestDomain;
import is.codion.plugin.jackson.json.TestDomain.Department;
import is.codion.plugin.jackson.json.TestDomain.Employee;
import is.codion.plugin.jackson.json.TestDomain.TestEntity;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityObjectMapperTest {

  private final Entities entities = new TestDomain().getEntities();

  @Test
  void entity() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Entity dept = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .with(Department.NAME, "Name")
            .with(Department.LOCATION, "Location")
            .build();
    dept.put(Department.LOCATION, "New Location");
    byte[] logoBytes = new byte[20];
    new Random().nextBytes(logoBytes);
    dept.put(Department.LOGO, logoBytes);

    String jsonString = mapper.writeValueAsString(dept);
    Entity readDept = mapper.readValue(jsonString, Entity.class);
    assertTrue(dept.columnValuesEqual(readDept));

    Entity entity = entities.builder(TestEntity.TYPE)
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

    assertTrue(entity.columnValuesEqual(mapper.readValue(jsonString, Entity.class)));

    entity.put(TestEntity.BOOLEAN, false);
    jsonString = mapper.writeValueAsString(entity);
    Entity entityModified = mapper.readValue(jsonString, Entity.class);
    assertTrue(entityModified.isModified());
    assertTrue(entityModified.isModified(TestEntity.BOOLEAN));
  }

  @Test
  void entityForeignKeys() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities).setIncludeForeignKeyValues(true);

    Entity dept = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .with(Department.NAME, "Name")
            .with(Department.LOCATION, "Location")
            .with(Department.LOCATION, "New Location")
            .build();

    Entity emp = entities.builder(Employee.TYPE)
            .with(Employee.EMPNO, 2)
            .with(Employee.NAME, "Emp")
            .with(Employee.COMMISSION, 134.34)
            .with(Employee.DEPARTMENT_FK, dept)
            .with(Employee.HIREDATE, LocalDate.now())
            .build();

    String jsonString = mapper.writeValueAsString(emp);
    Entity readEmp = mapper.readValue(jsonString, Entity.class);
    assertTrue(emp.columnValuesEqual(readEmp));

    Entity readDept = readEmp.getForeignKey(Employee.DEPARTMENT_FK);
    assertTrue(dept.columnValuesEqual(readDept));
  }

  @Test
  void key() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Key deptKey1 = entities.primaryKey(Department.TYPE, 1);
    Key deptKey2 = entities.primaryKey(Department.TYPE, 2);

    String jsonString = mapper.serializeKeys(asList(deptKey1, deptKey2));

    List<Key> keys = mapper.deserializeKeys(jsonString);
    assertEquals(Department.TYPE, keys.get(0).getEntityType());
    assertEquals(Integer.valueOf(1), keys.get(0).get());
    assertEquals(Integer.valueOf(2), keys.get(1).get());

    Key entityKey = entities.keyBuilder(TestEntity.TYPE)
            .with(TestEntity.DECIMAL, BigDecimal.valueOf(1234L))
            .with(TestEntity.DATE_TIME, LocalDateTime.now())
            .build();

    jsonString = mapper.writeValueAsString(entityKey);

    Key readKey = mapper.readValue(jsonString, Key.class);

    assertEquals(entityKey, readKey);
  }

  @Test
  void keyOld() throws Exception {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Key key = entities.primaryKey(Department.TYPE, 42);

    String keyJSON = mapper.writeValueAsString(singletonList(key));
    assertEquals("[{\"entityType\":\"scott.dept\",\"values\":{\"deptno\":42}}]", keyJSON);
    Key keyParsed = mapper.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityType(), keyParsed.getEntityType());
    assertEquals(key.getAttribute(), keyParsed.getAttribute());
    assertEquals((Integer) key.get(), keyParsed.get());
  }

  @Test
  void entityOld() throws Exception {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    Entity dept10 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, -10)
            .with(Department.NAME, "DEPTNAME")
            .with(Department.LOCATION, "LOCATION")
            .build();

    String jsonString = mapper.writeValueAsString(singletonList(dept10));
    assertTrue(dept10.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    Entity dept20 = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, -20)
            .with(Department.NAME, null)
            .with(Department.LOCATION, "ALOC")
            .build();

    jsonString = mapper.writeValueAsString(singletonList(dept20));
    assertTrue(dept20.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    String twoDepts = mapper.serializeEntities(asList(dept10, dept20));
    mapper.deserializeEntities(twoDepts);

    Entity mgr30 = entities.builder(Employee.TYPE)
            .with(Employee.COMMISSION, 500.5)
            .with(Employee.DEPARTMENT_FK, dept20)
            .with(Employee.HIREDATE, hiredate)
            .with(Employee.EMPNO, -30)
            .with(Employee.JOB, "MANAGER")
            .with(Employee.NAME, "MGR NAME")
            .with(Employee.SALARY, BigDecimal.valueOf(2500.5))
            .build();

    Entity mgr50 = entities.builder(Employee.TYPE)
            .with(Employee.COMMISSION, 500.5)
            .with(Employee.DEPARTMENT_FK, dept20)
            .with(Employee.HIREDATE, hiredate)
            .with(Employee.EMPNO, -50)
            .with(Employee.JOB, "MANAGER")
            .with(Employee.NAME, "MGR2 NAME")
            .with(Employee.SALARY, BigDecimal.valueOf(2500.5))
            .build();

    Entity emp1 = entities.builder(Employee.TYPE)
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
    assertTrue(emp1.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    mapper.setIncludeForeignKeyValues(true);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    Entity emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKey(Employee.DEPARTMENT_FK).columnValuesEqual(emp1Deserialized.getForeignKey(Employee.DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKey(Employee.MGR_FK).columnValuesEqual(emp1Deserialized.getForeignKey(Employee.MGR_FK)));

    LocalDate newHiredate = LocalDate.parse("2002-11-21", format);
    emp1.put(Employee.COMMISSION, 550.55);
    emp1.put(Employee.DEPARTMENT_FK, dept20);
    emp1.put(Employee.JOB, "ANALYST");
    emp1.put(Employee.MGR_FK, mgr50);
    emp1.put(Employee.NAME, "ANOTHER NAME");
    emp1.put(Employee.SALARY, BigDecimal.valueOf(3500.5));
    emp1.put(Employee.HIREDATE, newHiredate);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginal(Employee.COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginal(Employee.DEPARTMENT_FK));
    assertEquals("CLERK", emp1Deserialized.getOriginal(Employee.JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginal(Employee.MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginal(Employee.HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginal(Employee.NAME));
    assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.getOriginal(Employee.SALARY));

    assertTrue(emp1Deserialized.getOriginal(Employee.DEPARTMENT_FK).columnValuesEqual(dept10));
    assertTrue(emp1Deserialized.getOriginal(Employee.MGR_FK).columnValuesEqual(mgr30));

    Entity emp2 = entities.builder(Employee.TYPE)
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
      assertTrue(parsed.columnValuesEqual(entity));
    }

    List<Entity> readEntities = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp1)));
    assertEquals(1, readEntities.size());
    Entity parsedEntity = readEntities.iterator().next();
    assertTrue(emp1.columnValuesEqual(parsedEntity));
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(Employee.COMMISSION));
    assertTrue(parsedEntity.isModified(Employee.DEPARTMENT));
    assertTrue(parsedEntity.isModified(Employee.JOB));
    assertTrue(parsedEntity.isModified(Employee.MGR));
    assertTrue(parsedEntity.isModified(Employee.NAME));
    assertTrue(parsedEntity.isModified(Employee.SALARY));
    assertTrue(parsedEntity.isModified(Employee.HIREDATE));

    Entity emp3 = entities.builder(Employee.TYPE)
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

    Entity emp3Parsed = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp3))).get(0);
    assertFalse(emp3Parsed.contains(Employee.HIREDATE));
    assertFalse(emp3Parsed.contains(Employee.SALARY));
  }

  @Test
  void dependencyMap() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Entity dept = entities.builder(Department.TYPE)
            .with(Department.DEPTNO, 1)
            .with(Department.NAME, "Name")
            .with(Department.LOCATION, "Location")
            .with(Department.LOCATION, "New Location")
            .build();

    Map<String, Collection<Entity>> map = new HashMap<>();

    map.put(Department.TYPE.getName(), singletonList(dept));

    String string = mapper.writeValueAsString(map);

    mapper.readValue(string, new TypeReference<Map<String, Collection<Entity>>>() {});
  }

  @Test
  void customSerializer() throws JsonProcessingException {
    EntityObjectMapper mapper = new CustomEntityObjectMapperFactory().createEntityObjectMapper(entities);

    Custom custom = new Custom("a value");
    assertEquals(custom.value, mapper.readValue(mapper.writeValueAsString(custom), Custom.class).value);
  }
}
