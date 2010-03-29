/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.demos.empdept.domain.EmpDept;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class EntityUtilTest {

  @Test
  public void testJSON() throws Exception {
    final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    final Date hiredate = format.parse("2001-12-20");
    new EmpDept();
    final Entity emp1 = new Entity(EmpDept.T_EMPLOYEE);
    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 500.5);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT, -20);
    emp1.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp1.setValue(EmpDept.EMPLOYEE_ID, -500);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "A JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR, -30);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "A NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 2500.5);
    String emp1JSON = "{\"employee PK[empno:-500]\":{\"propertyValues\":{\"hiredate\":\"2001-12-20\",\"comm\":500.5,\"empno\":-500,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-20,\"mgr\":-30,\"sal\":2500.5},\"entityID\":\"employee\"}}";
    String json = EntityUtil.getJSONString(Arrays.asList(emp1));
    assertEquals(emp1JSON, json);

    emp1.setValue(EmpDept.EMPLOYEE_COMMISSION, 550.55);
    emp1.setValue(EmpDept.EMPLOYEE_DEPARTMENT, -40);
    emp1.setValue(EmpDept.EMPLOYEE_JOB, "ANOTHER JOB");
    emp1.setValue(EmpDept.EMPLOYEE_MGR, -50);
    emp1.setValue(EmpDept.EMPLOYEE_NAME, "ANOTHER NAME");
    emp1.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);

    emp1JSON = "{\"employee PK[empno:-500]\":{\"originalValues\":{\"comm\":500.5,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-20,\"mgr\":-30,\"sal\":2500.5},\"propertyValues\":{\"hiredate\":\"2001-12-20\",\"comm\":550.55,\"empno\":-500,\"ename\":\"ANOTHER NAME\",\"job\":\"ANOTHER JOB\",\"deptno\":-40,\"mgr\":-50,\"sal\":3500.5},\"entityID\":\"employee\"}}";
    json = EntityUtil.getJSONString(Arrays.asList(emp1));
    assertEquals(emp1JSON, json);

    final Entity emp2 = new Entity(EmpDept.T_EMPLOYEE);
    emp2.setValue(EmpDept.EMPLOYEE_COMMISSION, 300.5);
    emp2.setValue(EmpDept.EMPLOYEE_DEPARTMENT, -10);
    emp2.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    emp2.setValue(EmpDept.EMPLOYEE_ID, -200);
    emp2.setValue(EmpDept.EMPLOYEE_JOB, "JOB");
    emp2.setValue(EmpDept.EMPLOYEE_MGR, -20);
    emp2.setValue(EmpDept.EMPLOYEE_NAME, "NAME");
    emp2.setValue(EmpDept.EMPLOYEE_SALARY, 3500.5);

    final String emp12JSON = "{\"employee PK[empno:-200]\":{\"propertyValues\":{\"hiredate\":\"2001-12-20\",\"comm\":300.5,\"empno\":-200,\"ename\":\"NAME\",\"job\":\"JOB\",\"deptno\":-10,\"mgr\":-20,\"sal\":3500.5},\"entityID\":\"employee\"},\"employee PK[empno:-500]\":{\"originalValues\":{\"comm\":500.5,\"ename\":\"A NAME\",\"job\":\"A JOB\",\"deptno\":-20,\"mgr\":-30,\"sal\":2500.5},\"propertyValues\":{\"hiredate\":\"2001-12-20\",\"comm\":550.55,\"empno\":-500,\"ename\":\"ANOTHER NAME\",\"job\":\"ANOTHER JOB\",\"deptno\":-40,\"mgr\":-50,\"sal\":3500.5},\"entityID\":\"employee\"}}";
    json = EntityUtil.getJSONString(Arrays.asList(emp1, emp2));
    assertEquals(emp12JSON, json);

    final Entity jsonEntity = new Entity(EmpDept.T_EMPLOYEE);
    jsonEntity.setValue(EmpDept.EMPLOYEE_COMMISSION, 1200d);
    jsonEntity.setValue(EmpDept.EMPLOYEE_DEPARTMENT, 10);
    jsonEntity.setValue(EmpDept.EMPLOYEE_HIREDATE, format.parse("1983-01-23"));
    jsonEntity.setValue(EmpDept.EMPLOYEE_ID, 7934);
    jsonEntity.setValue(EmpDept.EMPLOYEE_JOB, "CLERK");
    jsonEntity.setValue(EmpDept.EMPLOYEE_MGR, 7782);
    jsonEntity.setValue(EmpDept.EMPLOYEE_NAME, "MILLER");
    jsonEntity.setValue(EmpDept.EMPLOYEE_SALARY, 1300d);

    final String jsonString = "{\"employee PK[empno:7934]\":{\"originalValues\":{\"comm\":500.5,\"sal\":1990},\"propertyValues\":{\"hiredate\":\"1983-01-23\",\"comm\":1200,\"empno\":7934,\"ename\":\"MILLER\",\"job\":\"CLERK\",\"deptno\":10,\"mgr\":7782,\"sal\":1300},\"entityID\":\"employee\"}}";
    final Collection<Entity> entities = EntityUtil.parseJSONString(jsonString);
    assertEquals(1, entities.size());
    final Entity parsedEntity = entities.iterator().next();
    assertTrue(jsonEntity.propertyValuesEqual(parsedEntity));
    assertTrue(parsedEntity.getModifiedState().isActive());
    assertTrue(parsedEntity.isModified());
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_COMMISSION));
    assertTrue(parsedEntity.isModified(EmpDept.EMPLOYEE_SALARY));
  }
}
