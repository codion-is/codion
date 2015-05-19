/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.SimpleCriteria;
import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityCriteriaUtilTest {

  @BeforeClass
  public static void init() {
    EmpDept.init();
  }

  @Test
  public void criteria() {
    final Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    EntityCriteria criteria = EntityCriteriaUtil.criteria(entity.getPrimaryKey());
    assertPrimaryKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.criteria(Collections.singletonList(entity.getPrimaryKey()));
    assertPrimaryKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.criteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertCriteria(criteria);
  }

  @Test
  public void selectCriteria() {
    final Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(entity.getPrimaryKey());
    assertPrimaryKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.selectCriteria(Collections.singletonList(entity.getPrimaryKey()));
    assertPrimaryKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertCriteria(criteria);

    final Criteria<Property.ColumnProperty> critOne = EntityCriteriaUtil.propertyCriteria(EmpDept.T_DEPARTMENT,
            EmpDept.DEPARTMENT_LOCATION, SearchType.LIKE, "New York");

    criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, critOne, EmpDept.DEPARTMENT_NAME);
    assertEquals(-1, criteria.getFetchCount());

    criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, 10);
    assertEquals(10, criteria.getFetchCount());
  }

  @Test
  public void propertyCriteria() {
    final Criteria<Property.ColumnProperty> critOne = EntityCriteriaUtil.propertyCriteria(EmpDept.T_DEPARTMENT,
            EmpDept.DEPARTMENT_LOCATION, SearchType.LIKE, true, "New York");
    assertEquals("loc like ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyCriteriaNull() {
    final Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", criteria.getWhereClause());
  }

  @Test
  public void foreignKeyCriteriaEntity() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, 10);
    Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, department);
    assertEquals("deptno = ?", criteria.getWhereClause());

    final Entity department2 = Entities.entity(EmpDept.T_DEPARTMENT);
    department2.setValue(EmpDept.DEPARTMENT_ID, 11);
    criteria = EntityCriteriaUtil.foreignKeyCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno in (?, ?))", criteria.getWhereClause());

    criteria = EntityCriteriaUtil.foreignKeyCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.NOT_LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno not in (?, ?))", criteria.getWhereClause());
  }

  @Test
  public void foreignKeyCriteriaEntityKey() {
    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, 10);
    final Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(EmpDept.T_EMPLOYEE,
            EmpDept.EMPLOYEE_DEPARTMENT_FK, SearchType.LIKE, department.getPrimaryKey());
    assertEquals("deptno = ?", criteria.getWhereClause());
  }

  @Test
  public void simpleCriteria() {
    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT,
            new SimpleCriteria<Property.ColumnProperty>("department name is not null"), EmpDept.DEPARTMENT_NAME, -1);
    assertEquals(0, criteria.getValues().size());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(criteria.getOrderByClause(), EmpDept.DEPARTMENT_NAME);
  }

  private void assertPrimaryKeyCriteria(final EntityCriteria criteria) {
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals(10, criteria.getValues().get(0));
    assertEquals(EmpDept.DEPARTMENT_ID, criteria.getValueKeys().get(0).getPropertyID());
  }

  private void assertCriteria(final EntityCriteria criteria) {
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname not like ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals("DEPT", criteria.getValues().get(0));
    assertEquals(EmpDept.DEPARTMENT_NAME, criteria.getValueKeys().get(0).getPropertyID());
  }
}
