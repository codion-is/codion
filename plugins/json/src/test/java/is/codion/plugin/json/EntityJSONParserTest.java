/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.json;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityJSONParserTest {

  private static final Entities ENTITIES = new TestDomain().getEntities();

  @Test
  public void key() throws Exception {
    final Key key = ENTITIES.key(TestDomain.T_DEPARTMENT, 42);

    final EntityJSONParser parser = new EntityJSONParser(ENTITIES);

    final String keyJSON = parser.serializeKeys(singletonList(key));
    final Key keyParsed = parser.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityType(), keyParsed.getEntityType());
    assertEquals(key.getFirstAttribute(), keyParsed.getFirstAttribute());
    assertEquals((Integer) key.getFirstValue(), keyParsed.getFirstValue());
  }

  @Test
  public void entity() throws Exception {
    final DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    final LocalDate hiredate = LocalDate.parse("2001-12-20", format);

    EntityJSONParser parser = new EntityJSONParser(ENTITIES);

    final Entity dept10 = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept10.put(TestDomain.DEPARTMENT_ID, -10);
    dept10.put(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.put(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = parser.serialize(singletonList(dept10));
    assertTrue(dept10.valuesEqual(parser.deserialize(jsonString).get(0)));

    final Entity dept20 = ENTITIES.entity(TestDomain.T_DEPARTMENT);
    dept20.put(TestDomain.DEPARTMENT_ID, -20);
    dept20.put(TestDomain.DEPARTMENT_NAME, null);
    dept20.put(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = parser.serialize(singletonList(dept20));
    assertTrue(dept20.valuesEqual(parser.deserialize(jsonString).get(0)));

    final String twoDepts = parser.serialize(asList(dept10, dept20));
    parser.deserialize(twoDepts);

    final Entity mgr30 = ENTITIES.entity(TestDomain.T_EMP);
    mgr30.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.put(TestDomain.EMP_ID, -30);
    mgr30.put(TestDomain.EMP_JOB, "MANAGER");
    mgr30.put(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity mgr50 = ENTITIES.entity(TestDomain.T_EMP);
    mgr50.put(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.put(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.put(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.put(TestDomain.EMP_ID, -50);
    mgr50.put(TestDomain.EMP_JOB, "MANAGER");
    mgr50.put(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.5));

    final Entity emp1 = ENTITIES.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_COMMISSION, 500.5);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.put(TestDomain.EMP_HIREDATE, hiredate);
    emp1.put(TestDomain.EMP_ID, -500);
    emp1.put(TestDomain.EMP_JOB, "CLERK");
    emp1.put(TestDomain.EMP_MGR_FK, mgr30);
    emp1.put(TestDomain.EMP_NAME, "A NAME");
    emp1.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(2500.55));

    jsonString = parser.serialize(singletonList(emp1));
    assertTrue(emp1.valuesEqual(parser.deserialize(jsonString).get(0)));

    parser = new EntityJSONParser(ENTITIES);
    parser.setIncludeForeignKeyValues(true);

    jsonString = parser.serialize(singletonList(emp1));
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

    jsonString = parser.serialize(singletonList(emp1));
    emp1Deserialized = parser.deserialize(jsonString).get(0);
    assertTrue(emp1.valuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginal(TestDomain.EMP_COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals("CLERK", emp1Deserialized.getOriginal(TestDomain.EMP_JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginal(TestDomain.EMP_HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginal(TestDomain.EMP_NAME));
    assertEquals(BigDecimal.valueOf(2500.55), emp1Deserialized.getOriginal(TestDomain.EMP_SALARY));

    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_DEPARTMENT_FK).valuesEqual(dept10));
    assertTrue(emp1Deserialized.getOriginal(TestDomain.EMP_MGR_FK).valuesEqual(mgr30));

    final Entity emp2 = ENTITIES.entity(TestDomain.T_EMP);
    emp2.put(TestDomain.EMP_COMMISSION, 300.5);
    emp2.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp2.put(TestDomain.EMP_HIREDATE, hiredate);
    emp2.put(TestDomain.EMP_ID, -200);
    emp2.put(TestDomain.EMP_JOB, "CLERK");
    emp2.put(TestDomain.EMP_MGR_FK, mgr50);
    emp2.put(TestDomain.EMP_NAME, "NAME");
    emp2.put(TestDomain.EMP_SALARY, BigDecimal.valueOf(3500.5));

    parser = new EntityJSONParser(ENTITIES);

    final List<Entity> entityList = asList(emp1, emp2);
    jsonString = parser.serialize(entityList);
    final List<Entity> parsedEntities = parser.deserialize(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.valuesEqual(entity));
    }

    final List<Entity> entities = parser.deserialize(parser.serialize(singletonList(emp1)));
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(emp1.valuesEqual(parsedEntity));
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));

    final Entity emp3 = ENTITIES.entity(TestDomain.T_EMP);
    emp3.put(TestDomain.EMP_COMMISSION, 300.5);
    emp3.put(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp3.put(TestDomain.EMP_HIREDATE, null);
    emp3.put(TestDomain.EMP_ID, -200);
    emp3.put(TestDomain.EMP_JOB, "CLERK");
    emp3.put(TestDomain.EMP_MGR_FK, mgr50);
    emp3.put(TestDomain.EMP_NAME, "NAME");
    emp3.put(TestDomain.EMP_SALARY, null);

    parser = new EntityJSONParser(ENTITIES);
    parser.setIncludeForeignKeyValues(false);
    parser.setIncludeNullValues(false);

    final Entity emp3Parsed = parser.deserialize(parser.serialize(singletonList(emp3))).get(0);
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_HIREDATE));
    assertFalse(emp3Parsed.containsKey(TestDomain.EMP_SALARY));
  }

  @Test
  public void emptyStringAndNull() throws JSONException {
    final EntityJSONParser parser = new EntityJSONParser(ENTITIES);
    assertEquals(0, parser.deserialize("").size());
    assertEquals(0, parser.deserialize(null).size());
    assertEquals(0, parser.deserializeEntities("").size());
    assertEquals(0, parser.deserializeKeys(null).size());

    final List<Entity> entities = emptyList();
    assertEquals("", parser.serialize(entities));
    assertEquals("", parser.serialize(null));
    final List<Key> keys = emptyList();
    assertEquals("", parser.serializeKeys(keys));
    assertEquals("", parser.serializeKeys(null));
  }
}
