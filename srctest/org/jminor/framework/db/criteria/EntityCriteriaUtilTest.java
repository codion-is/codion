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

    criteria = EntityCriteriaUtil.criteria(Arrays.asList(entity.getPrimaryKey()));
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

    criteria = EntityCriteriaUtil.selectCriteria(Arrays.asList(entity.getPrimaryKey()));
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
    assertEquals("loc like ?", critOne.asString());
    assertNotNull(critOne);
  }

  @Test
  public void simpleCriteria() {
    final EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT,
            new SimpleCriteria<Property.ColumnProperty>("department name is not null"), EmpDept.DEPARTMENT_NAME,
            EmpDept.DEPARTMENT_NAME, -1);
    assertEquals(0, criteria.getValues().size());
    assertEquals(0, criteria.getValueProperties().size());
    assertEquals(criteria.getOrderByClause(), EmpDept.DEPARTMENT_NAME);
    assertEquals(criteria.getGroupByClause(), EmpDept.DEPARTMENT_NAME);
  }

  private void assertPrimaryKeyCriteria(final EntityCriteria criteria) {
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueProperties().size());
    assertEquals(10, criteria.getValues().get(0));
    assertEquals(EmpDept.DEPARTMENT_ID, criteria.getValueProperties().get(0).getPropertyID());
  }

  private void assertCriteria(final EntityCriteria criteria) {
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname not like ?", criteria.getWhereClause());
    assertEquals(1, criteria.getValues().size());
    assertEquals(1, criteria.getValueProperties().size());
    assertEquals("DEPT", criteria.getValues().get(0));
    assertEquals(EmpDept.DEPARTMENT_NAME, criteria.getValueProperties().get(0).getPropertyID());
  }
}
