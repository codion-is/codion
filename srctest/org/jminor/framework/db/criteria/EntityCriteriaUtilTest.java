package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

public class EntityCriteriaUtilTest {

  private static final Database DATABASE = DatabaseProvider.createInstance();
  private static final Criteria.ValueProvider VALUE_PROVIDER = EntityCriteriaUtil.getCriteriaValueProvider();

  @BeforeClass
  public static void init() {
    new EmpDept();
  }

  @Test
  public void criteria() {
    final Entity entity = new Entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    EntityCriteria criteria = EntityCriteriaUtil.criteria(entity.getPrimaryKey());
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = 10", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));

    criteria = EntityCriteriaUtil.criteria(Arrays.asList(entity.getPrimaryKey()));
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = 10", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));

    criteria = EntityCriteriaUtil.criteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname not like 'DEPT'", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));
  }

  @Test
  public void selectCriteria() {
    final Entity entity = new Entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    EntitySelectCriteria criteria = EntityCriteriaUtil.selectCriteria(entity.getPrimaryKey());
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = 10", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));

    criteria = EntityCriteriaUtil.selectCriteria(Arrays.asList(entity.getPrimaryKey()));
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where deptno = 10", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));

    criteria = EntityCriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname not like 'DEPT'", criteria.getWhereClause(DATABASE, VALUE_PROVIDER));
  }
}
