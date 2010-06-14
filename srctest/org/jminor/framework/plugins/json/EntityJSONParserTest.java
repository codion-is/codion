package org.jminor.framework.plugins.json;

import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class EntityJSONParserTest {

  @Test
  public void test() throws Exception {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date hiredate = format.parse("2001-12-20");
    new EmpDept();

    final Entity dept10 = new Entity(EmpDept.T_DEPARTMENT);
    dept10.setValue(EmpDept.DEPARTMENT_ID, -10);
    dept10.setValue(EmpDept.DEPARTMENT_NAME, "DEPTNAME");
    dept10.setValue(EmpDept.DEPARTMENT_LOCATION, "LOCATION");

    String dept10JSON = "{\"department" + EmpDept.version + " PK[deptno:-10]\":{\"propertyValues\":{\"dname\":\"DEPTNAME\",\"loc\":\"LOCATION\",\"deptno\":-10},\"entityID\":\"department" + EmpDept.version + "\"}}";
    String jsonString = EntityJSONParser.getJSONString(Arrays.asList(dept10));
    assertEquals(dept10JSON, jsonString);

    final Entity dept20 = new Entity(EmpDept.T_DEPARTMENT);
    dept20.setValue(EmpDept.DEPARTMENT_ID, -20);
    dept20.setValue(EmpDept.DEPARTMENT_NAME, "ADEPT");
    dept20.setValue(EmpDept.DEPARTMENT_LOCATION, "ALOC");

    String dept20JSON = "{\"department" + EmpDept.version + " PK[deptno:-20]\":{\"propertyValues\":{\"dname\":\"ADEPT\",\"loc\":\"ALOC\",\"deptno\":-20},\"entityID\":\"department" + EmpDept.version + "\"}}";
    jsonString = EntityJSONParser.getJSONString(Arrays.asList(dept20));
    assertEquals(dept20JSON, jsonString);

    final Entity mgr30 = new Entity(EmpDept.T_EMPLOYEE);
    mgr30.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    mgr30.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    mgr30.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    mgr30.setValue(EmpDept.EMPLOYEE_ID, -30);
    mgr30.setValue(EmpDept.EMPLOYEE_JOB, "MGR");
    mgr30.setValue(EmpDept.EMPLOYEE_NAME, "MGR NAME");
    mgr30.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);

    final Entity mgr50 = new Entity(EmpDept.T_EMPLOYEE);
    mgr50.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    mgr50.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    mgr50.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    mgr50.setValue(EmpDept.EMPLOYEE_ID, -50);
    mgr50.setValue(EmpDept.EMPLOYEE_JOB, "MGR2");
    mgr50.setValue(EmpDept.EMPLOYEE_NAME, "MGR2 NAME");
    mgr50.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);

    final Entity emp1 = new Entity(EmpDept.T_EMPLOYEE);
    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept10);
    emp1.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp1.setValue(EmpDept.EMPLOYEE_ID, -500);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "A JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr30);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "A NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);
    String emp1JSON = "{\"employee" + EmpDept.version + " PK[empno:-500]\":{\"propertyValues\":{\"hiredate\":\"2001-12-20\",\"comm\":500.5,\"empno\":-500,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-10,\"mgr\":-30,\"sal\":2500.5},\"entityID\":\"employee" + EmpDept.version + "\"}}";
    jsonString = EntityJSONParser.getJSONString(Arrays.asList(emp1));
    assertEquals(emp1JSON, jsonString);

    final Date newHiredate = format.parse("2002-11-21");
    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 550.55);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept20);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "ANOTHER JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr50);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "ANOTHER NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);
    emp1.setValue(EmpDept.EMPLOYEE_HIREDATE, newHiredate);

    emp1JSON = "{\"employee@EmpDept-v1.0 PK[empno:-500]\":{\"originalValues\":{\"hiredate\":\"2001-12-20\",\"comm\":500.5,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-10,\"mgr\":-30,\"sal\":2500.5},\"propertyValues\":{\"hiredate\":\"2002-11-21\",\"comm\":550.55,\"empno\":-500,\"ename\":\"ANOTHER NAME\",\"job\":\"ANOTHER JOB\",\"deptno\":-20,\"mgr\":-50,\"sal\":3500.5},\"entityID\":\"employee@EmpDept-v1.0\"}}";
    jsonString = EntityJSONParser.getJSONString(Arrays.asList(emp1));
    assertEquals(emp1JSON, jsonString);

    final Entity emp2 = new Entity(EmpDept.T_EMPLOYEE);
    emp2.setValue(EmpDept.EMPLOYEE_COMMISSION, 300.5);
    emp2.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, dept10);
    emp2.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp2.setValue(EmpDept.EMPLOYEE_ID, -200);
    emp2.setValue(EmpDept.EMPLOYEE_JOB, "JOB");
    emp2.setValue(EmpDept.EMPLOYEE_MGR_FK, mgr50);
    emp2.setValue(EmpDept.EMPLOYEE_NAME, "NAME");
    emp2.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);

    final List<Entity> entityList = Arrays.asList(emp1, emp2);
    jsonString = EntityJSONParser.getJSONString(entityList);
    final List<Entity> parsedEntities = EntityJSONParser.parseJSONString(jsonString);
    for (final Entity entity : entityList) {
      final Entity parsed = parsedEntities.get(parsedEntities.indexOf(entity));
      assertTrue(parsed.propertyValuesEqual(entity));
    }

    final List<Entity> entities = EntityJSONParser.parseJSONString(emp1JSON);
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(emp1.propertyValuesEqual(parsedEntity));
    assertTrue(parsedEntity.stateModified().isActive());
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_COMMISSION));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_DEPARTMENT));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_JOB));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_MGR));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_NAME));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_SALARY));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_HIREDATE));
  }
}
