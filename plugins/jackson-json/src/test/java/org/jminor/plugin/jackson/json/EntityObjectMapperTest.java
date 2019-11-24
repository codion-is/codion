/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json;

import org.jminor.framework.domain.Entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityObjectMapperTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void entity() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(domain);

    final Entity dept = domain.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");
    final byte[] logoBytes = new byte[20];
    new Random().nextBytes(logoBytes);
    dept.put(TestDomain.DEPARTMENT_LOGO, logoBytes);

    final String jsonString = mapper.writeValueAsString(dept);
    System.out.println(jsonString);

    final Entity readDept = mapper.readValue(jsonString, Entity.class);
    assertTrue(dept.valuesEqual(readDept));
  }

  @Test
  public void entityForeignKeys() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(domain).setIncludeForeignKeyValues(true);

    final Entity dept = domain.entity(TestDomain.T_DEPARTMENT);
    dept.put(TestDomain.DEPARTMENT_ID, 1);
    dept.put(TestDomain.DEPARTMENT_NAME, "Name");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "Location");
    dept.put(TestDomain.DEPARTMENT_LOCATION, "New Location");

    final Entity emp = domain.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, 2);
    emp.put(TestDomain.EMP_NAME, "Emp");
    emp.put(TestDomain.EMP_COMMISSION, 134.34);
    emp.put(TestDomain.EMP_DEPARTMENT_FK, dept);
    emp.put(TestDomain.EMP_HIREDATE, LocalDate.now());

    final String jsonString = mapper.writeValueAsString(emp);
    System.out.println(jsonString);

    final Entity readEmp = mapper.readValue(jsonString, Entity.class);
    assertTrue(emp.valuesEqual(readEmp));

    final Entity readDept = readEmp.getForeignKey(TestDomain.EMP_DEPARTMENT_FK);
    assertTrue(dept.valuesEqual(readDept));
  }

  @Test
  public void key() throws JsonProcessingException {
    final EntityObjectMapper mapper = new EntityObjectMapper(domain);

    final Entity.Key deptKey = domain.key(TestDomain.T_DEPARTMENT);
    deptKey.put(TestDomain.DEPARTMENT_ID, 1);

    final String jsonString = mapper.writeValueAsString(deptKey);

    final Entity.Key key = mapper.readValue(jsonString, Entity.Key.class);
    assertEquals(TestDomain.T_DEPARTMENT, key.getEntityId());
    assertEquals(1, key.getFirstValue());
  }

  @Test
  public void keyOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(domain);

    final Entity.Key key = domain.key(TestDomain.T_DEPARTMENT, 42);

    final String keyJSON = mapper.writeValueAsString(singletonList(key));
    assertEquals("[{\"entityId\":\"scott.dept\",\"values\":{\"deptno\":42}}]", keyJSON);
    final Entity.Key keyParsed = mapper.readValue(keyJSON,  new TypeReference<List<Entity.Key>>(){}).get(0);
    assertEquals(key.getEntityId(), keyParsed.getEntityId());
    assertEquals(key.getFirstProperty(), keyParsed.getFirstProperty());
    assertEquals(key.getFirstValue(), keyParsed.getFirstValue());
  }

  @Test
  public void entityOld() throws Exception {
    final EntityObjectMapper mapper = new EntityObjectMapper(domain);

    final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    final Entity dept10 = domain.entity(TestDomain.T_DEPARTMENT);
    dept10.put(TestDomain.DEPARTMENT_ID, -10);
    dept10.put(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.put(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = mapper.writeValueAsString(singletonList(dept10));
    assertTrue(dept10.valuesEqual(mapper.readValue(jsonString, new TypeReference<List<Entity>>(){}).get(0)));

    final Entity dept20 = domain.entity(TestDomain.T_DEPARTMENT);
    dept20.put(TestDomain.DEPARTMENT_ID, -20);
    dept20.put(TestDomain.DEPARTMENT_NAME, null);
    dept20.put(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = mapper.writeValueAsString(singletonList(dept20));
    assertTrue(dept20.valuesEqual(mapper.readValue(jsonString, new TypeReference<List<Entity>>(){}).get(0)));

    final String twoDepts = mapper.writeValueAsString(asList(dept10, dept20));
    mapper.readValue(twoDepts, new TypeReference<List<Entity>>(){});

    final Entity mgr30 = domain.entity(TestDomain.T_EMP);
    mgr30.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.put(TestDomain.EMP_ID, -30);
    mgr30.put(TestDomain.EMP_JOB, "MANAGER");
    mgr30.put(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity mgr50 = domain.entity(TestDomain.T_EMP);
    mgr50.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.put(TestDomain.EMP_ID, -50);
    mgr50.put(TestDomain.EMP_JOB, "MANAGER");
    mgr50.put(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity emp1 = domain.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_COMMISSION, 500.5);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.put(TestDomain.EMP_HIREDATE, hiredate);
    emp1.put(TestDomain.EMP_ID, -500);
    emp1.put(TestDomain.EMP_JOB, "CLERK");
    emp1.put(TestDomain.EMP_MGR_FK, mgr30);
    emp1.put(TestDomain.EMP_NAME, "A NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55));

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    assertTrue(emp1.valuesEqual(mapper.readValue(jsonString, new TypeReference<List<Entity>>(){}).get(0)));

    mapper.setIncludeForeignKeyValues(true);

    jsonString = mapper.writeValueAsString(singletonList(emp1));
    Entity emp1Deserialized = mapper.readValue(jsonString, new TypeReference<List<Entity>>(){}).get(0);
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
    emp1Deserialized = mapper.readValue(jsonString, new TypeReference<List<Entity>>(){}).get(0);
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

    final Entity emp2 = domain.entity(TestDomain.T_EMP);
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
    final List<Entity> parsedEntities = mapper.readValue(jsonString, new TypeReference<List<Entity>>(){});
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.valuesEqual(entity));
    }

    final List<Entity> entities = mapper.readValue(mapper.writeValueAsString(singletonList(emp1)), new TypeReference<List<Entity>>(){});
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(emp1.valuesEqual(parsedEntity));
    assertTrue(parsedEntity.getModifiedObserver().get());
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));

    final Entity emp3 = domain.entity(TestDomain.T_EMP);
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

    final Entity emp3Parsed = mapper.readValue(mapper.writeValueAsString(singletonList(emp3)), new TypeReference<List<Entity>>(){}).get(0);
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_SALARY));
  }
}
