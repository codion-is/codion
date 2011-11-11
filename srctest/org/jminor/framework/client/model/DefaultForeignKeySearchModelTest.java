package org.jminor.framework.client.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DefaultForeignKeySearchModelTest {

  @Test
  public void getSearchEntities() throws DatabaseException {
    EmpDept.init();
    final EntityLookupModel lookupModel = new DefaultEntityLookupModel(EmpDept.T_DEPARTMENT, EntityConnectionImplTest.DB_PROVIDER,
            Arrays.asList(Entities.getColumnProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME)));
    final ForeignKeySearchModel searchModel = new DefaultForeignKeySearchModel(
            Entities.getForeignKeyProperty(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT_FK), lookupModel);
    final Entity sales = EntityConnectionImplTest.DB_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "SALES");
    lookupModel.setSelectedEntity(sales);
    Collection<Entity> searchEntities = searchModel.getSearchEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    final Entity accounting = EntityConnectionImplTest.DB_PROVIDER.getConnection().selectSingle(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
    lookupModel.setSelectedEntities(Arrays.asList(sales, accounting));
    searchEntities = searchModel.getSearchEntities();
    assertEquals(2, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    assertTrue(searchEntities.contains(accounting));

    searchModel.setUpperBound((Object) null);
    assertTrue(lookupModel.getSelectedEntities().isEmpty());
    searchModel.setUpperBound(sales);
    assertEquals(lookupModel.getSelectedEntities().get(0), sales);

    lookupModel.setSelectedEntity(null);

    searchEntities = searchModel.getSearchEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
