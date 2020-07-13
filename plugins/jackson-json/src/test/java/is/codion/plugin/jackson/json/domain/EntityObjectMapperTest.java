/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.plugin.jackson.json.TestDomain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

  private static final TypeReference<List<Entity>> ENTITY_LIST_TYPE_REF = new TypeReference<List<Entity>>() {};

  private final Entities entities = new TestDomain().getEntities();

  @Test
  public void entity() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");
    final byte[] logoBytes = new byte[20];
    new Random().nextBytes(logoBytes);
    dept.put(TestDomain.DEPARTMENT_LOGO, logoBytes);

    String jsonString = mapper.writeValueAsString(dept);
    final Entity readDept = mapper.readValue(jsonString, Entity.class);
    assertTrue(dept.valuesEqual(readDept));

    final Entity entity = entities.entity(TestDomain.T_ENTITY);
    entity.put(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L));
    entity.put(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now());
    entity.put(TestDomain.ENTITY_BLOB, logoBytes);
    entity.put(TestDomain.ENTITY_READ_ONLY, "readOnly");
    entity.put(TestDomain.ENTITY_BOOLEAN, true);
    entity.put(TestDomain.ENTITY_TIME, LocalTime.now());

    jsonString = mapper.writeValueAsString(entity);

    assertTrue(entity.valuesEqual(mapper.readValue(jsonString, Entity.class)));

    entity.put(TestDomain.ENTITY_BOOLEAN, false);
    jsonString = mapper.writeValueAsString(entity);
    final Entity entityModified = mapper.readValue(jsonString, Entity.class);
    assertTrue(entityModified.isModified());
    assertTrue(entityModified.isModified(TestDomain.ENTITY_BOOLEAN));
  }

  @Test
  public void entityForeignKeys() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities).setIncludeForeignKeyValues(true);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");

    final Entity emp = entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, 2);
    emp.put(TestDomain.EMP_NAME, "Emp");
    emp.put(TestDomain.EMP_COMMISSION, 134.34);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_HIREDATE, LocalDate.now());

    final String jsonString = mapper.writeValueAsString(emp);
    final Entity readEmp = mapper.readValue(jsonString, Entity.class);
    assertTrue(emp.valuesEqual(readEmp));

    final Entity readDept = readEmp.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(dept.valuesEqual(readDept));
  }

  @Test
  public void key() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Key deptKey = entities.key(TestDomain.T_DEPARTMENT);
    deptKey.put(1);

    String jsonString = mapper.writeValueAsString(deptKey);

    final Key key = mapper.readValue(jsonString, Key.class);
    assertEquals(TestDomain.T_DEPARTMENT, key.getEntityType());
    assertEquals(Integer.valueOf(1), key.get());

    final Key entityKey = entities.key(TestDomain.T_ENTITY);
    entityKey.put(TestDomain.ENTITY_DECIMAL, BigDecimal.valueOf(1234L));
    entityKey.put(TestDomain.ENTITY_DATE_TIME, LocalDateTime.now());

    jsonString = mapper.writeValueAsString(entityKey);

    final Key readKey = mapper.readValue(jsonString, Key.class);

    assertEquals(entityKey, readKey);
  }

  @Test
  public void keyOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Key key = entities.key(TestDomain.T_DEPARTMENT, 42);

    final String keyJSON = mapper.writeValueAsString(singletonList(key));
    assertEquals("[{\"entityType\":\"scott.dept\",\"values\":{\"deptno\":42}}]", keyJSON);
    final Key keyParsed = mapper.readValue(keyJSON,  new TypeReference<List<Key>>(){}).get(0);
    assertEquals(key.getEntityType(), keyParsed.getEntityType());
    assertEquals(key.getAttribute(), keyParsed.getAttribute());
    assertEquals((Integer) key.get(), keyParsed.get());
  }

  @Test
  public void entityOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    final Entity dept10 = entities.entity(TestDomain.T_DEPARTMENT);
    dept10.put(TestDomain.DEPARTMENT_ID, -10);
    dept10.put(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.put(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = mapper.writeValueAsString(singletonList(dept10));
    assertTrue(dept10.valuesEqual(mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF).get(0)));

    final Entity dept20 = entities.entity(TestDomain.T_DEPARTMENT);
    dept20.put(TestDomain.DEPARTMENT_ID, -20);
    dept20.put(TestDomain.DEPARTMENT_NAME, null);
    dept20.put(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = mapper.writeValueAsString(singletonList(dept20));
    assertTrue(dept20.valuesEqual(mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF).get(0)));

    final String twoDepts = mapper.writeValueAsString(asList(dept10, dept20));
    mapper.readValue(twoDepts, ENTITY_LIST_TYPE_REF);

    final Entity mgr30 = entities.entity(TestDomain.T_EMP);
    mgr30.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.put(TestDomain.EMP_ID, -30);
    mgr30.put(TestDomain.EMP_JOB, "MANAGER");
    mgr30.put(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity mgr50 = entities.entity(TestDomain.T_EMP);
    mgr50.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.put(TestDomain.EMP_ID, -50);
    mgr50.put(TestDomain.EMP_JOB, "MANAGER");
    mgr50.put(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity emp1 = entities.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_COMMISSION, 500.5);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.put(TestDomain.EMP_HIREDATE, hiredate);
    emp1.put(TestDomain.EMP_ID, -500);
    emp1.put(TestDomain.EMP_JOB, "CLERK");
    emp1.put(TestDomain.EMP_MGR_FK, mgr30);
    emp1.put(TestDomain.EMP_NAME, "A NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55));

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    assertTrue(emp1.valuesEqual(mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF).get(0)));

    mapper.setIncludeForeignKeyValues(true);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    Entity emp1Deserialized = mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF).get(0);
    assertTrue(emp1.valuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_DEPARTMENT_FK).valuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKey(TestDomain.EMP_MGR_FK).valuesEqual(emp1Deserialized.getForeignKey(TestDomain.EMP_MGR_FK)));

    final LocalDate newHiredate = LocalDate.parse("2002-11-21", format);
    emp1.put(TestDomain.EMP_COMMISSION, 550.55);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    emp1.put(TestDomain.EMP_JOB, "ANALYST");
    emp1.put(TestDomain.EMP_MGR_FK, mgr50);
    emp1.put(TestDomain.EMP_NAME, "ANOTHER NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));
    emp1.put(TestDomain.EMP_HIREDATE, newHiredate);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    emp1Deserialized = mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF).get(0);
    assertTrue(emp1.valuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginal(TestDomain.EMP_COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals("CLERK", emp1Deserialized.getOriginal(TestDomain.EMP_JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginal(TestDomain.EMP_HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginal(TestDomain.EMP_NAME));
    assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.getOriginal(TestDomain.EMP_SALARY));

    assertTrue(((Entity) emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK)).valuesEqual(dept10));
    assertTrue(((Entity) emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK)).valuesEqual(mgr30));

    final Entity emp2 = entities.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_COMMISSION, 300.5);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp2.put(TestDomain.EMP_HIREDATE, hiredate);
    emp2.put(TestDomain.EMP_ID, -200);
    emp2.put(TestDomain.EMP_JOB, "CLERK");
    emp2.put(TestDomain.EMP_MGR_FK, mgr50);
    emp2.put(TestDomain.EMP_NAME, "NAME");
    emp2.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));

    mapper.setIncludeForeignKeyValues(true);

    final List<Entity> entityList = asList(emp1, emp2);
    jsonString = mapper.writeValueAsString(entityList);
    final List<Entity> parsedEntities = mapper.readValue(jsonString, ENTITY_LIST_TYPE_REF);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.valuesEqual(entity));
    }

    final List<Entity> readEntities = mapper.readValue(mapper.writeValueAsString(singletonList(emp1)), ENTITY_LIST_TYPE_REF);
    assertEquals(1, readEntities.size());
    final Entity parsedEntity = readEntities.iterator().next();
    assertTrue(emp1.valuesEqual(parsedEntity));
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));

    final Entity emp3 = entities.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_COMMISSION, 300.5);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp3.put(TestDomain.EMP_HIREDATE, null);
    emp3.put(TestDomain.EMP_ID, -200);
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_MGR_FK, mgr50);
    emp3.put(TestDomain.EMP_NAME, "NAME");
    emp3.put(TestDomain.EMP_SALARY, null);

    mapper.setIncludeForeignKeyValues(false);
    mapper.setIncludeNullValues(false);

    final Entity emp3Parsed = mapper.readValue(mapper.writeValueAsString(singletonList(emp3)), ENTITY_LIST_TYPE_REF).get(0);
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_SALARY));
  }

  @Test
  public void dependencyMap() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(entities);

    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");

    final Map<EntityType<?>, List<Entity>> map = new HashMap<>();

    map.put(TestDomain.T_DEPARTMENT, singletonList(dept));

    final String string = mapper.writeValueAsString(map);

    final Map<EntityType<?>, Collection<Entity>> readMap = mapper.readValue(string, new TypeReference<Map<EntityType<?>, Collection<Entity>>>() {});

    System.out.println(readMap);
  }
}
