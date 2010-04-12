package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaValueProvider;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;

import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;

public class CriteriaUtilTest {

  private static final CriteriaValueProvider valueProvider = CriteriaUtil.getCriteriaValueProvider();

  @BeforeClass
  public static void init() {
    new EmpDept();
  }

  @Test
  public void criteria() {
    final Database database = DatabaseProvider.createInstance();
    final Entity entity = new Entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    EntityCriteria criteria = CriteriaUtil.criteria(entity.getPrimaryKey());
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where (deptno = 10)", criteria.getWhereClause(database, valueProvider));

    criteria = CriteriaUtil.criteria(Arrays.asList(entity.getPrimaryKey()));
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where (deptno = 10)", criteria.getWhereClause(database, valueProvider));

    criteria = CriteriaUtil.criteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname <> 'DEPT'", criteria.getWhereClause(database, valueProvider));
  }

  @Test
  public void selectCriteria() {
    final Database database = DatabaseProvider.createInstance();
    final Entity entity = new Entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 10);

    SelectCriteria criteria = CriteriaUtil.selectCriteria(entity.getPrimaryKey());
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where (deptno = 10)", criteria.getWhereClause(database, valueProvider));

    criteria = CriteriaUtil.selectCriteria(Arrays.asList(entity.getPrimaryKey()));
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where (deptno = 10)", criteria.getWhereClause(database, valueProvider));

    criteria = CriteriaUtil.selectCriteria(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, SearchType.NOT_LIKE, "DEPT");
    assertEquals(EmpDept.T_DEPARTMENT, criteria.getEntityID());
    assertEquals("where dname <> 'DEPT'", criteria.getWhereClause(database, valueProvider));
  }
}
