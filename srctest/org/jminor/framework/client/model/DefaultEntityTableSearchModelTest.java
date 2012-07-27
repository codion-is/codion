package org.jminor.framework.client.model;

import org.jminor.common.model.Conjunction;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionImplTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.*;

public class DefaultEntityTableSearchModelTest {

  private final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, EntityConnectionImplTest.CONNECTION_PROVIDER);
  private final EntityTableSearchModel searchModel = tableModel.getSearchModel();

  public DefaultEntityTableSearchModelTest() {
    EmpDept.init();
  }

  @Test
  public void test() {
    assertEquals(EmpDept.T_EMPLOYEE, searchModel.getEntityID());
    searchModel.setSearchConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, searchModel.getSearchConjunction());
    assertEquals(9, searchModel.getPropertyFilterModels().size());
    assertEquals(8, searchModel.getPropertySearchModels().size());

    searchModel.refresh();
    assertTrue(((ForeignKeySearchModel) searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() > 1);
    searchModel.clear();
    assertTrue(((ForeignKeySearchModel) searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() == 0);

    assertFalse(searchModel.isFilterEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(searchModel.isSearchEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
  }

  @Test
  public void testSearchState() {
    assertFalse(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setLikeValue("job");
    assertTrue(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setEnabled(false);
    assertFalse(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setEnabled(true);
    assertTrue(searchModel.hasSearchStateChanged());
    searchModel.rememberCurrentSearchState();
    assertFalse(searchModel.hasSearchStateChanged());
  }

  @Test
  public void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    final String wildcardValue = wildcard + "test" + wildcard;
    searchModel.setSimpleSearchString(value);
    for (final PropertySearchModel model : searchModel.getPropertySearchModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertEquals(wildcardValue, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    searchModel.setSimpleSearchString(null);
    for (final PropertySearchModel model : searchModel.getPropertySearchModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
