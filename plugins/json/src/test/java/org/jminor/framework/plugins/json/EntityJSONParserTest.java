/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.json;

import org.jminor.common.Serializer;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityJSONParserTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void key() throws Exception {
    final Entity.Key key = DOMAIN.key(TestDomain.T_DEPARTMENT);
    key.put(TestDomain.DEPARTMENT_ID, 42);

    final EntityJSONParser parser = new EntityJSONParser(DOMAIN);

    final String keyJSON = parser.serializeKeys(Collections.singletonList(key));
    assertEquals("[{\"values\":{\"deptno\":42},\"entityId\":\"scott.dept\"}]", keyJSON);
    final Entity.Key keyParsed = parser.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityId(), keyParsed.getEntityId());
    assertEquals(key.getFirstProperty(), keyParsed.getFirstProperty());
    assertEquals(key.getFirstValue(), keyParsed.getFirstValue());
  }

  @Test
  public void entity() throws Exception {
    final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    EntityJSONParser parser = new EntityJSONParser(DOMAIN);

    final Entity dept10 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept10.put(TestDomain.DEPARTMENT_ID, -10);
    dept10.put(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.put(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = parser.serialize(Collections.singletonList(dept10));
    assertTrue(dept10.valuesEqual(parser.deserialize(jsonString).get(0)));

    final Entity dept20 = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    dept20.put(TestDomain.DEPARTMENT_ID, -20);
    dept20.put(TestDomain.DEPARTMENT_NAME, null);
    dept20.put(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = parser.serialize(Collections.singletonList(dept20));
    assertTrue(dept20.valuesEqual(parser.deserialize(jsonString).get(0)));

    final String twoDepts = parser.serialize(Arrays.asList(dept10, dept20));
    parser.deserialize(twoDepts);

    final Entity mgr30 = DOMAIN.entity(TestDomain.T_EMP);
    mgr30.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.put(TestDomain.EMP_ID, -30);
    mgr30.put(TestDomain.EMP_JOB, "MANAGER");
    mgr30.put(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity mgr50 = DOMAIN.entity(TestDomain.T_EMP);
    mgr50.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.put(TestDomain.EMP_ID, -50);
    mgr50.put(TestDomain.EMP_JOB, "MANAGER");
    mgr50.put(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity emp1 = DOMAIN.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_COMMISSION, 500.5);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.put(TestDomain.EMP_HIREDATE, hiredate);
    emp1.put(TestDomain.EMP_ID, -500);
    emp1.put(TestDomain.EMP_JOB, "CLERK");
    emp1.put(TestDomain.EMP_MGR_FK, mgr30);
    emp1.put(TestDomain.EMP_NAME, "A NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55));

    jsonString = parser.serialize(Collections.singletonList(emp1));
    assertTrue(emp1.valuesEqual(parser.deserialize(jsonString).get(0)));

    parser = new EntityJSONParser(DOMAIN);
    parser.setIncludeForeignKeyValues(true);

    jsonString = parser.serialize(Collections.singletonList(emp1));
    Entity emp1Deserialized = parser.deserialize(jsonString).get(0);
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

    jsonString = parser.serialize(Collections.singletonList(emp1));
    emp1Deserialized = parser.deserialize(jsonString).get(0);
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

    final Entity emp2 = DOMAIN.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_COMMISSION, 300.5);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp2.put(TestDomain.EMP_HIREDATE, hiredate);
    emp2.put(TestDomain.EMP_ID, -200);
    emp2.put(TestDomain.EMP_JOB, "CLERK");
    emp2.put(TestDomain.EMP_MGR_FK, mgr50);
    emp2.put(TestDomain.EMP_NAME, "NAME");
    emp2.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));

    parser = new EntityJSONParser(DOMAIN);

    final List<Entity> entityList = Arrays.asList(emp1, emp2);
    jsonString = parser.serialize(entityList);
    final List<Entity> parsedEntities = parser.deserialize(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.valuesEqual(entity));
    }

    final List<Entity> entities = parser.deserialize(parser.serialize(Collections.singletonList(emp1)));
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

    final Entity emp3 = DOMAIN.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_COMMISSION, 300.5);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp3.put(TestDomain.EMP_HIREDATE, null);
    emp3.put(TestDomain.EMP_ID, -200);
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_MGR_FK, mgr50);
    emp3.put(TestDomain.EMP_NAME, "NAME");
    emp3.put(TestDomain.EMP_SALARY, null);

    parser = new EntityJSONParser(DOMAIN);
    parser.setIncludeForeignKeyValues(false);
    parser.setIncludeNullValues(false);

    final Entity emp3Parsed = parser.deserialize(parser.serialize(Collections.singletonList(emp3))).get(0);
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_SALARY));
  }

  @Test
  public void emptyStringAndNull() throws Serializer.SerializeException, JSONException {
    final EntityJSONParser parser = new EntityJSONParser(DOMAIN);
    assertEquals(0, parser.deserialize("").size());
    assertEquals(0, parser.deserialize(null).size());
    assertEquals(0, parser.deserializeEntities("").size());
    assertEquals(0, parser.deserializeKeys(null).size());

    final List<Entity> entities = Collections.emptyList();
    assertEquals("", parser.serialize(entities));
    assertEquals("", parser.serialize(null));
    final List<Entity.Key> keys = Collections.emptyList();
    assertEquals("", parser.serializeKeys(keys));
    assertEquals("", parser.serializeKeys(null));
  }
}
