/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.json;

import org.jminor.common.model.SerializeException;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;

import org.json.JSONException;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityJSONParserTest {

  @Test
  public void key() throws Exception {
    TestDomain.init();
    final Entity.Key key = Entities.key(TestDomain.T_DEPARTMENT);
    key.setValue(TestDomain.DEPARTMENT_ID, 42);

    final String keyJSON = EntityJSONParser.serializeKeys(Collections.singletonList(key));
    assertEquals("[{\"values\":{\"deptno\":42},\"entityID\":\"unittest.scott.dept\"}]", keyJSON);
    final Entity.Key keyParsed = EntityJSONParser.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityID(), keyParsed.getEntityID());
    assertEquals(key.getFirstKeyProperty(), keyParsed.getFirstKeyProperty());
    assertEquals(key.getFirstKeyValue(), keyParsed.getFirstKeyValue());
  }

  @Test
  public void entity() throws Exception {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date hiredate = format.parse("2001-12-20");
    TestDomain.init();

    EntityJSONParser parser = new EntityJSONParser();

    final Entity dept10 = Entities.entity(TestDomain.T_DEPARTMENT);
    dept10.setValue(TestDomain.DEPARTMENT_ID, -10);
    dept10.setValue(TestDomain.DEPARTMENT_NAME, "DEPTNAME");
    dept10.setValue(TestDomain.DEPARTMENT_LOCATION, "LOCATION");

    String jsonString = parser.serialize(Collections.singletonList(dept10));
    assertTrue(dept10.propertyValuesEqual(parser.deserialize(jsonString).get(0)));

    final Entity dept20 = Entities.entity(TestDomain.T_DEPARTMENT);
    dept20.setValue(TestDomain.DEPARTMENT_ID, -20);
    dept20.setValue(TestDomain.DEPARTMENT_NAME, null);
    dept20.setValue(TestDomain.DEPARTMENT_LOCATION, "ALOC");

    jsonString = parser.serialize(Collections.singletonList(dept20));
    assertTrue(dept20.propertyValuesEqual(parser.deserialize(jsonString).get(0)));

    final String twoDepts = parser.serialize(Arrays.asList(dept10, dept20));
    parser.deserialize(twoDepts);

    final Entity mgr30 = Entities.entity(TestDomain.T_EMP);
    mgr30.setValue(TestDomain.EMP_COMMISSION, 500.5);
    mgr30.setValue(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr30.setValue(TestDomain.EMP_HIREDATE, hiredate);
    mgr30.setValue(TestDomain.EMP_ID, -30);
    mgr30.setValue(TestDomain.EMP_JOB, "MGR");
    mgr30.setValue(TestDomain.EMP_NAME, "MGR NAME");
    mgr30.setValue(TestDomain.EMP_SALARY, 2500.5);

    final Entity mgr50 = Entities.entity(TestDomain.T_EMP);
    mgr50.setValue(TestDomain.EMP_COMMISSION, 500.5);
    mgr50.setValue(TestDomain.EMP_DEPARTMENT_FK, dept20);
    mgr50.setValue(TestDomain.EMP_HIREDATE, hiredate);
    mgr50.setValue(TestDomain.EMP_ID, -50);
    mgr50.setValue(TestDomain.EMP_JOB, "MGR2");
    mgr50.setValue(TestDomain.EMP_NAME, "MGR2 NAME");
    mgr50.setValue(TestDomain.EMP_SALARY, 2500.5);

    final Entity emp1 = Entities.entity(TestDomain.T_EMP);
    emp1.setValue(TestDomain.EMP_COMMISSION, 500.5);
    emp1.setValue(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp1.setValue(TestDomain.EMP_HIREDATE, hiredate);
    emp1.setValue(TestDomain.EMP_ID, -500);
    emp1.setValue(TestDomain.EMP_JOB, "A JOB");
    emp1.setValue(TestDomain.EMP_MGR_FK, mgr30);
    emp1.setValue(TestDomain.EMP_NAME, "A NAME");
    emp1.setValue(TestDomain.EMP_SALARY, 2500.5);

    jsonString = EntityJSONParser.serializeEntities(Collections.singletonList(emp1), false);
    assertTrue(emp1.propertyValuesEqual(parser.deserialize(jsonString).get(0)));

    jsonString = EntityJSONParser.serializeEntities(Collections.singletonList(emp1), true);
    Entity emp1Deserialized = parser.deserialize(jsonString).get(0);
    assertTrue(emp1.propertyValuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK).propertyValuesEqual(emp1Deserialized.getForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKeyValue(TestDomain.EMP_MGR_FK).propertyValuesEqual(emp1Deserialized.getForeignKeyValue(TestDomain.EMP_MGR_FK)));

    final Date newHiredate = format.parse("2002-11-21");
    emp1.setValue(TestDomain.EMP_COMMISSION, 550.55);
    emp1.setValue(TestDomain.EMP_DEPARTMENT_FK, dept20);
    emp1.setValue(TestDomain.EMP_JOB, "ANOTHER JOB");
    emp1.setValue(TestDomain.EMP_MGR_FK, mgr50);
    emp1.setValue(TestDomain.EMP_NAME, "ANOTHER NAME");
    emp1.setValue(TestDomain.EMP_SALARY, 3500.5);
    emp1.setValue(TestDomain.EMP_HIREDATE, newHiredate);

    jsonString = EntityJSONParser.serializeEntities(Collections.singletonList(emp1), true);
    emp1Deserialized = parser.deserialize(jsonString).get(0);
    assertTrue(emp1.propertyValuesEqual(emp1Deserialized));

    assertEquals(500.5, emp1Deserialized.getOriginalValue(TestDomain.EMP_COMMISSION));
    assertEquals(dept10, emp1Deserialized.getOriginalValue(TestDomain.EMP_DEPARTMENT_FK));
    assertEquals("A JOB", emp1Deserialized.getOriginalValue(TestDomain.EMP_JOB));
    assertEquals(mgr30, emp1Deserialized.getOriginalValue(TestDomain.EMP_MGR_FK));
    assertEquals(hiredate, emp1Deserialized.getOriginalValue(TestDomain.EMP_HIREDATE));
    assertEquals("A NAME", emp1Deserialized.getOriginalValue(TestDomain.EMP_NAME));
    assertEquals(2500.5, emp1Deserialized.getOriginalValue(TestDomain.EMP_SALARY));

    assertTrue(((Entity) emp1Deserialized.getOriginalValue(TestDomain.EMP_DEPARTMENT_FK)).propertyValuesEqual(dept10));
    assertTrue(((Entity) emp1Deserialized.getOriginalValue(TestDomain.EMP_MGR_FK)).propertyValuesEqual(mgr30));

    final Entity emp2 = Entities.entity(TestDomain.T_EMP);
    emp2.setValue(TestDomain.EMP_COMMISSION, 300.5);
    emp2.setValue(TestDomain.EMP_DEPARTMENT_FK, dept10);
    emp2.setValue(TestDomain.EMP_HIREDATE, hiredate);
    emp2.setValue(TestDomain.EMP_ID, -200);
    emp2.setValue(TestDomain.EMP_JOB, "JOB");
    emp2.setValue(TestDomain.EMP_MGR_FK, mgr50);
    emp2.setValue(TestDomain.EMP_NAME, "NAME");
    emp2.setValue(TestDomain.EMP_SALARY, 3500.5);

    parser = new EntityJSONParser();

    final List<Entity> entityList = Arrays.asList(emp1, emp2);
    jsonString = parser.serialize(entityList);
    final List<Entity> parsedEntities = parser.deserialize(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.propertyValuesEqual(entity));
    }

    final List<Entity> entities = parser.deserialize(parser.serialize(Collections.singletonList(emp1)));
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(emp1.propertyValuesEqual(parsedEntity));
    assertTrue(parsedEntity.getModifiedObserver().isActive());
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(TestDomain.EMP_COMMISSION));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_DEPARTMENT));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_JOB));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_MGR));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_NAME));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_SALARY));
    assertTrue(parsedEntity.isModified(TestDomain.EMP_HIREDATE));
  }

  @Test
  public void emptyStringAndNull() throws SerializeException, ParseException, JSONException {
    final EntityJSONParser parser = new EntityJSONParser();
    assertEquals(0, parser.deserialize("").size());
    assertEquals(0, parser.deserialize(null).size());
    assertEquals(0, EntityJSONParser.deserializeEntities("").size());
    assertEquals(0, EntityJSONParser.deserializeKeys(null).size());

    final List<Entity> entities = Collections.emptyList();
    assertEquals("", parser.serialize(entities));
    assertEquals("", parser.serialize(null));
    final List<Entity.Key> keys = Collections.emptyList();
    assertEquals("", EntityJSONParser.serializeKeys(keys));
    assertEquals("", EntityJSONParser.serializeKeys(null));
  }
}
