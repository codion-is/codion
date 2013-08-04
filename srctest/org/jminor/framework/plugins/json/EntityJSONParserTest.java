/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.json;

import org.jminor.common.model.SerializeException;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

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
    EmpDept.init();
    final Entity.Key key = Entities.key(EmpDept.T_DEPARTMENT);
    key.setValue(EmpDept.DEPARTMENT_ID, 42);

    final String keyJSON = EntityJSONParser.serializeKeys(Arrays.asList(key));
    assertEquals("[{\"values\":{\"deptno\":42},\"entityID\":\"scott.dept\"}]", keyJSON);
    final Entity.Key keyParsed = EntityJSONParser.deserializeKeys(keyJSON).get(0);
    assertEquals(key.getEntityID(), keyParsed.getEntityID());
    assertEquals(key.getFirstKeyProperty(), keyParsed.getFirstKeyProperty());
    assertEquals(key.getFirstKeyValue(), keyParsed.getFirstKeyValue());
  }

  @Test
  public void entity() throws Exception {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date hiredate = format.parse("2001-12-20");
    EmpDept.init();

    EntityJSONParser parser = new EntityJSONParser();

    final Entity dept10 = Entities.entity(EmpDept.T_DEPARTMENT);
    dept10.setValue(EmpDept.DEPARTMENT_ID, -10);
    dept10.setValue(EmpDept.DEPARTMENT_NAME, "DEPTNAME");
    dept10.setValue(EmpDept.DEPARTMENT_LOCATION, "LOCATION");

    final String dept10JSON = "[{\"values\":{\"dname\":\"DEPTNAME\",\"loc\":\"LOCATION\",\"deptno\":-10},\"entityID\":\"scott.dept\"}]";
    String jsonString = parser.serialize(Arrays.asList(dept10));
    assertEquals(dept10JSON, jsonString);

    final Entity dept20 = Entities.entity(EmpDept.T_DEPARTMENT);
    dept20.setValue(EmpDept.DEPARTMENT_ID, -20);
    dept20.setValue(EmpDept.DEPARTMENT_NAME, null);
    dept20.setValue(EmpDept.DEPARTMENT_LOCATION, "ALOC");

    final String dept20JSON = "[{\"values\":{\"dname\":null,\"loc\":\"ALOC\",\"deptno\":-20},\"entityID\":\"scott.dept\"}]";
    jsonString = parser.serialize(Arrays.asList(dept20));
    assertEquals(dept20JSON, jsonString);

    final Entity dept20Des = parser.deserialize(dept20JSON).get(0);
    assertEquals(EmpDept.T_DEPARTMENT, dept20Des.getEntityID());
    assertEquals(-20, dept20Des.getValue(EmpDept.DEPARTMENT_ID));
    assertTrue(dept20Des.isValueNull(EmpDept.DEPARTMENT_NAME));
    assertEquals("ALOC", dept20Des.getValue(EmpDept.DEPARTMENT_LOCATION));

    final String twoDepts = parser.serialize(Arrays.asList(dept10, dept20));
    parser.deserialize(twoDepts);

    final Entity mgr30 = Entities.entity(EmpDept.T_EMPLOYEE);
    mgr30.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    mgr30.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    mgr30.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    mgr30.setValue(EmpDept.EMPLOYEE_ID, -30);
    mgr30.setValue(EmpDept.EMPLOYEE_JOB, "MGR");
    mgr30.setValue(EmpDept.EMPLOYEE_NAME, "MGR NAME");
    mgr30.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);

    final Entity mgr50 = Entities.entity(EmpDept.T_EMPLOYEE);
    mgr50.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    mgr50.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    mgr50.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    mgr50.setValue(EmpDept.EMPLOYEE_ID, -50);
    mgr50.setValue(EmpDept.EMPLOYEE_JOB, "MGR2");
    mgr50.setValue(EmpDept.EMPLOYEE_NAME, "MGR2 NAME");
    mgr50.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);

    final Entity emp1 = Entities.entity(EmpDept.T_EMPLOYEE);
    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept10);
    emp1.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp1.setValue(EmpDept.EMPLOYEE_ID, -500);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "A JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr30);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "A NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);
    String emp1JSON = "[{\"values\":{\"comm\":500.5,\"hiredate\":\"2001-12-20\",\"empno\":-500,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-10,\"mgr\":-30,\"sal\":2500.5},\"entityID\":\"scott.emp\"}]";
    jsonString = EntityJSONParser.serializeEntities(Arrays.asList(emp1), false);
    assertEquals(emp1JSON, jsonString);
    emp1JSON = "[{\"values\":{\"comm\":500.5,\"dept_fk\":{\"values\":{\"dname\":\"DEPTNAME\",\"loc\":\"LOCATION\",\"deptno\":-10},\"entityID\":\"scott.dept\"},\"hiredate\":\"2001-12-20\",\"empno\":-500,\"mgr_fk\":{\"values\":{\"comm\":500.5,\"dept_fk\":{\"values\":{\"dname\":null,\"loc\":\"ALOC\",\"deptno\":-20},\"entityID\":\"scott.dept\"},\"hiredate\":\"2001-12-20\",\"empno\":-30,\"ename\":\"MGR NAME\",\"job\":\"MGR\",\"deptno\":-20,\"sal\":2500.5},\"entityID\":\"scott.emp\"},\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-10,\"mgr\":-30,\"sal\":2500.5},\"entityID\":\"scott.emp\"}]";
    jsonString = EntityJSONParser.serializeEntities(Arrays.asList(emp1), true);
    assertEquals(emp1JSON, jsonString);

    final Entity emp1Deserialized = parser.deserialize(emp1JSON).get(0);
    assertTrue(emp1.propertyValuesEqual(emp1Deserialized));
    assertTrue(emp1.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK).propertyValuesEqual(emp1Deserialized.getForeignKeyValue(EmpDept.EMPLOYEE_DEPARTMENT_FK)));
    assertTrue(emp1.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK).propertyValuesEqual(emp1Deserialized.getForeignKeyValue(EmpDept.EMPLOYEE_MGR_FK)));

    final Date newHiredate = format.parse("2002-11-21");
    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 550.55);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "ANOTHER JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr50);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "ANOTHER NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);
    emp1.setValue(EmpDept.EMPLOYEE_HIREDATE, newHiredate);

    emp1JSON = "[{\"originalValues\":{\"hiredate\":\"2001-12-20\",\"comm\":500.5,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-10,\"mgr\":-30,\"sal\":2500.5},\"values\":{\"comm\":550.55,\"hiredate\":\"2002-11-21\",\"empno\":-500,\"ename\":\"ANOTHER NAME\",\"job\":\"ANOTHER JOB\",\"deptno\":-20,\"mgr\":-50,\"sal\":3500.5},\"entityID\":\"scott.emp\"}]";
    jsonString = parser.serialize(Arrays.asList(emp1));
    assertEquals(emp1JSON, jsonString);

    final Entity emp2 = Entities.entity(EmpDept.T_EMPLOYEE);
    emp2.setValue(EmpDept.EMPLOYEE_COMMISSION, 300.5);
    emp2.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept10);
    emp2.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp2.setValue(EmpDept.EMPLOYEE_ID, -200);
    emp2.setValue(EmpDept.EMPLOYEE_JOB, "JOB");
    emp2.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr50);
    emp2.setValue(EmpDept.EMPLOYEE_NAME, "NAME");
    emp2.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);

    parser = new EntityJSONParser();

    final List<Entity> entityList = Arrays.asList(emp1, emp2);
    jsonString = parser.serialize(entityList);
    final List<Entity> parsedEntities = parser.deserialize(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.propertyValuesEqual(entity));
    }

    emp1JSON = parser.serialize(Arrays.asList(emp1));
    final List<Entity> entities = parser.deserialize(emp1JSON);
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(emp1.propertyValuesEqual(parsedEntity));
    assertTrue(parsedEntity.getModifiedState().isActive());
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_COMMISSION));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_DEPARTMENT));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_JOB));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_MGR));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_NAME));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_SALARY));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_HIREDATE));
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
