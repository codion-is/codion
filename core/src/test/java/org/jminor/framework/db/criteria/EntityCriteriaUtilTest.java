/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityCriteriaUtilTest {

  @BeforeClass
  public static void init() {
    TestDomain.init();
  }

  @Test
  public void criteria() {
    final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntityCriteria criteria = EntityCriteriaUtil.criteria(entity.getKey());
    assertKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.criteria(Collections.singletonList(entity.getKey()));
    assertKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.criteria(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertCriteria(criteria);
  }

  @Test
  public void selectCriteria() {
    final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 10);

    EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(entity.getKey());
    assertKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.selectCriteria(Collections.singletonList(entity.getKey()));
    assertKeyCriteria(criteria);

    criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertCriteria(criteria);

    final Criteria<Property.ColumnProperty> critOne = EntityCriteriaUtil.propertyCriteria(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, SearchType.LIKE, "New York");

    criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, critOne, TestDomain.DEPARTMENT_NAME);
    assertEquals(-1, criteria.getFetchCount());

    criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT, 10);
    assertEquals(10, criteria.getFetchCount());
  }

  @Test
  public void propertyCriteria() {
    final Criteria<Property.ColumnProperty> critOne = EntityCriteriaUtil.propertyCriteria(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_LOCATION, SearchType.LIKE, true, "New York");
    assertEquals("loc like ?", critOne.getWhereClause());
    assertNotNull(critOne);
  }

  @Test
  public void foreignKeyCriteriaNull() {
    final Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.LIKE, (Entity.Key) null);
    assertEquals("deptno is null", criteria.getWhereClause());
  }

  @Test
  public void foreignKeyCriteriaEntity() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.LIKE, department);
    assertEquals("deptno = ?", criteria.getWhereClause());

    final Entity department2 = Entities.entity(TestDomain.T_DEPARTMENT);
    department2.put(TestDomain.DEPARTMENT_ID, 11);
    criteria = EntityCriteriaUtil.foreignKeyCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno in (?, ?))", criteria.getWhereClause());

    criteria = EntityCriteriaUtil.foreignKeyCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.NOT_LIKE, Arrays.asList(department, department2));
    assertEquals("(deptno not in (?, ?))", criteria.getWhereClause());
  }

  @Test
  public void foreignKeyCriteriaEntityKey() {
    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, 10);
    final Criteria<Property.ColumnProperty> criteria = EntityCriteriaUtil.foreignKeyCriteria(TestDomain.T_EMP,
            TestDomain.EMP_DEPARTMENT_FK, SearchType.LIKE, department.getKey());
    assertEquals("deptno = ?", criteria.getWhereClause());
  }

  @Test
  public void simpleCriteria() {
    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(TestDomain.T_DEPARTMENT,
            CriteriaUtil.<Property.ColumnProperty>stringCriteria("department name is not null"), TestDomain.DEPARTMENT_NAME, -1);
    assertEquals(0, criteria.getValues().size());
    assertEquals(0, criteria.getValueKeys().size());
    assertEquals(criteria.getOrderByClause(), TestDomain.DEPARTMENT_NAME);
  }

  private void assertKeyCriteria(final EntityCriteria criteria) {
    assertEquals(TestDomain.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("deptno = ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals(10, criteria.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_ID, criteria.getValueKeys().get(0).getPropertyID());
  }

  private void assertCriteria(final EntityCriteria criteria) {
    assertEquals(TestDomain.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("dname not like ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueKeys().size());
    assertEquals("DEPT", criteria.getValues().get(0));
    assertEquals(TestDomain.DEPARTMENT_NAME, criteria.getValueKeys().get(0).getPropertyID());
  }
}
