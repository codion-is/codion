/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.TestDomain;

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

    Entity dept = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 1)
            .with(TestDomain.DEPARTMENT_NAME, "Name")
            .with(TestDomain.DEPARTMENT_LOCATION, "Location")
            .build();
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");
    byte[] logoBytes = new byte[20];
    new Random().nextBytes(logoBytes);
    dept.put(TestDomain.DEPARTMENT_LOGO, logoBytes);

    String jsonString = mapper.writeValueAsString(dept);
    Entity readDept = mapper.readValue(jsonString, Entity.class);
    assertTrue(dept.columnValuesEqual(readDept));

    Entity entity = entities.builder(TestDomain.T_ENTITY)
            .with(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L))
            .with(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now())
            .with(TestDomain.ENTITY_OFFSET_DATE_TIME, OffsetDateTime.now())
            .with(TestDomain.ENTITY_BLOB, logoBytes)
            .with(TestDomain.ENTITY_READ_ONLY, "readOnly")
            .with(TestDomain.ENTITY_BOOLEAN, true)
            .with(TestDomain.ENTITY_TIME, LocalTime.now())
            .build();

    jsonString = mapper.writeValueAsString(entity);

    assertTrue(entity.columnValuesEqual(mapper.readValue(jsonString, Entity.class)));

    entity.put(TestDomain.ENTITY_BOOLEAN, false);
    jsonString = mapper.writeValueAsString(entity);
    Entity entityModified = mapper.readValue(jsonString, Entity.class);
    assertTrue(entityModified.isModified());
    assertTrue(entityModified.isModified(TestDomain.ENTITY_BOOLEAN));
  }

  @Test
  void entityForeignKeys() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities).setIncludeForeignKeyValues(true);

    Entity dept = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 1)
            .with(TestDomain.DEPARTMENT_NAME, "Name")
            .with(TestDomain.DEPARTMENT_LOCATION, "Location")
            .with(TestDomain.DEPARTMENT_LOCATION, "New Location")
            .build();

    Entity emp = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_ID, 2)
            .with(TestDomain.EMP_NAME, "Emp")
            .with(TestDomain.EMP_COMMISSION, 134.34)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept)
            .with(TestDomain.EMP_HIREDATE, LocalDate.now())
            .build();

    String jsonString = mapper.writeValueAsString(emp);
    Entity readEmp = mapper.readValue(jsonString, Entity.class);
    assertTrue(emp.columnValuesEqual(readEmp));

    Entity readDept = readEmp.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(dept.columnValuesEqual(readDept));
  }

  @Test
  void key() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Key deptKey1 = entities.primaryKey(TestDomain.T_DEPARTMENT, 1);
    Key deptKey2 = entities.primaryKey(TestDomain.T_DEPARTMENT, 2);

    String jsonString = mapper.serializeKeys(asList(deptKey1, deptKey2));

    List<Key> keys = mapper.deserializeKeys(jsonString);
    assertEquals(TestDomain.T_DEPARTMENT, keys.get(0).getEntityType());
    assertEquals(Integer.valueOf(1), keys.get(0).get());
    assertEquals(Integer.valueOf(2), keys.get(1).get());

    Key entityKey = entities.keyBuilder(TestDomain.T_ENTITY)
            .with(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L))
            .with(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now())
            .build();

    jsonString = mapper.writeValueAsString(entityKey);

    Key readKey = mapper.readValue(jsonString, Key.class);

    assertEquals(entityKey, readKey);
  }

  @Test
  void keyOld() throws Exception {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Key key = entities.primaryKey(TestDomain.T_DEPARTMENT, 42);

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

    Entity dept10 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -10)
            .with(TestDomain.DEPARTMENT_NAME, "DEPTNAME")
            .with(TestDomain.DEPARTMENT_LOCATION, "LOCATION")
            .build();

    String jsonString = mapper.writeValueAsString(singletonList(dept10));
    assertTrue(dept10.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    Entity dept20 = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -20)
            .with(TestDomain.DEPARTMENT_NAME, null)
            .with(TestDomain.DEPARTMENT_LOCATION, "ALOC")
            .build();

    jsonString = mapper.writeValueAsString(singletonList(dept20));
    assertTrue(dept20.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    String twoDepts = mapper.serializeEntities(asList(dept10, dept20));
    mapper.deserializeEntities(twoDepts);

    Entity mgr30 = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_COMMISSION, 500.5)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept20)
            .with(TestDomain.EMP_HIREDATE, hiredate)
            .with(TestDomain.EMP_ID, -30)
            .with(TestDomain.EMP_JOB, "MANAGER")
            .with(TestDomain.EMP_NAME, "MGR NAME")
            .with(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5))
            .build();

    Entity mgr50 = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_COMMISSION, 500.5)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept20)
            .with(TestDomain.EMP_HIREDATE, hiredate)
            .with(TestDomain.EMP_ID, -50)
            .with(TestDomain.EMP_JOB, "MANAGER")
            .with(TestDomain.EMP_NAME, "MGR2 NAME")
            .with(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5))
            .build();

    Entity emp1 = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_COMMISSION, 500.5)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept10)
            .with(TestDomain.EMP_HIREDATE, hiredate)
            .with(TestDomain.EMP_ID, -500)
            .with(TestDomain.EMP_JOB, "CLERK")
            .with(TestDomain.EMP_MGR_FK, mgr30)
            .with(TestDomain.EMP_NAME, "A NAME")
            .with(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55))
            .build();

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    assertTrue(emp1.columnValuesEqual(mapper.deserializeEntities(jsonString).get(0)));

    mapper.setIncludeForeignKeyValues(true);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    Entity emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_DEPARTMENT_FK).columnValuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_MGR_FK).columnValuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_MGR_FK)));

    LocalDate newHiredate = LocalDate.parse("2002-11-21", format);
    emp1.put(TestDomain.EMP_COMMISSION, 550.55);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    emp1.put(TestDomain.EMP_JOB, "ANALYST");
    emp1.put(TestDomain.EMP_MGR_FK, mgr50);
    emp1.put(TestDomain.EMP_NAME, "ANOTHER NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));
    emp1.put(TestDomain.EMP_HIREDATE, newHiredate);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    emp1Deserialized = mapper.deserializeEntities(jsonString).get(0);
    assertTrue(emp1.columnValuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginal(TestDomain.EMP_COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals("CLERK", emp1Deserialized.getOriginal(TestDomain.EMP_JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginal(TestDomain.EMP_HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginal(TestDomain.EMP_NAME));
    assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.getOriginal(TestDomain.EMP_SALARY));

    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK).columnValuesEqual(dept10));
    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK).columnValuesEqual(mgr30));

    Entity emp2 = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_COMMISSION, 300.5)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept10)
            .with(TestDomain.EMP_HIREDATE, hiredate)
            .with(TestDomain.EMP_ID, -200)
            .with(TestDomain.EMP_JOB, "CLERK")
            .with(TestDomain.EMP_MGR_FK, mgr50)
            .with(TestDomain.EMP_NAME, "NAME")
            .with(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5))
            .build();

    mapper.setIncludeForeignKeyValues(true);

    List<Entity> entityList = asList(emp1, emp2);
    jsonString = mapper.writeValueAsString(entityList);
    List<Entity> parsedEntities = mapper.deserializeEntities(jsonString);
    for (final Entity entity : entityList) {
      Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.columnValuesEqual(entity));
    }

    List<Entity> readEntities = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp1)));
    assertEquals(1, readEntities.size());
    Entity parsedEntity = readEntities.iterator().next();
    assertTrue(emp1.columnValuesEqual(parsedEntity));
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));

    Entity emp3 = entities.builder(TestDomain.T_EMP)
            .with(TestDomain.EMP_COMMISSION, 300.5)
            .with(TestDomain.EMP_DEPARTMENT_FK, dept10)
            .with(TestDomain.EMP_HIREDATE, null)
            .with(TestDomain.EMP_ID, -200)
            .with(TestDomain.EMP_JOB, "CLERK")
            .with(TestDomain.EMP_MGR_FK, mgr50)
            .with(TestDomain.EMP_NAME, "NAME")
            .with(TestDomain.EMP_SALARY, null)
            .build();

    mapper.setIncludeForeignKeyValues(false);
    mapper.setIncludeNullValues(false);

    Entity emp3Parsed = mapper.deserializeEntities(mapper.serializeEntities(singletonList(emp3))).get(0);
    assertFalse(emp3Parsed.contains(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.contains(TestDomain.EMP_SALARY));
  }

  @Test
  void dependencyMap() throws JsonProcessingException {
    EntityObjectMapper mapper = new EntityObjectMapper(entities);

    Entity dept = entities.builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, 1)
            .with(TestDomain.DEPARTMENT_NAME, "Name")
            .with(TestDomain.DEPARTMENT_LOCATION, "Location")
            .with(TestDomain.DEPARTMENT_LOCATION, "New Location")
            .build();

    Map<String, Collection<Entity>> map = new HashMap<>();

    map.put(TestDomain.T_DEPARTMENT.getName(), singletonList(dept));

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
