/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.model;

import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.sql.Types;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultEntityTableCriteriaModelTest {

  private final EntityTableModel tableModel = new DefaultEntityTableModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  private final EntityTableCriteriaModel criteriaModel = tableModel.getCriteriaModel();

  static {
    TestDomain.init();
  }

  @Test
  public void test() {
    assertEquals(TestDomain.T_EMP, criteriaModel.getEntityID());
    criteriaModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, criteriaModel.getConjunction());
    assertEquals(9, criteriaModel.getPropertyFilterModels().size());
    assertEquals(8, criteriaModel.getPropertyCriteriaModels().size());

    criteriaModel.refresh();
    assertTrue(((ForeignKeyCriteriaModel) criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() > 1);
    criteriaModel.clear();
    assertTrue(((ForeignKeyCriteriaModel) criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() == 0);

    assertFalse(criteriaModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(criteriaModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(criteriaModel.isEnabled());
    criteriaModel.setEnabled(TestDomain.EMP_DEPARTMENT_FK, true);
    assertTrue(criteriaModel.isEnabled());
  }

  @Test
  public void getPropertyFilterModel() {
    assertNotNull(criteriaModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION));
    assertNull(criteriaModel.getPropertyFilterModel("bla bla"));
  }

  @Test
  public void getPropertyCriteriaModel() {
    assertNotNull(criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_COMMISSION));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPropertyCriteriaModelNonExisting() {
    criteriaModel.getPropertyCriteriaModel("bla bla");
  }

  @Test
  public void setFilterValue() {
    criteriaModel.setFilterValue(TestDomain.EMP_COMMISSION, 1400);
    criteriaModel.setFilterValue("bla bla", "bla");
    final ColumnCriteriaModel<Property> propertyCriteriaModel = criteriaModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyCriteriaModel.isEnabled());
    assertEquals(SearchType.LIKE, propertyCriteriaModel.getSearchType());
    assertEquals(1400, propertyCriteriaModel.getUpperBound());
  }

  @Test
  public void searchStateListener() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {
        counter.incrementAndGet();
      }
    };
    criteriaModel.addCriteriaStateListener(listener);
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_COMMISSION).setEnabled(true);
    assertEquals(1, counter.get());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_COMMISSION).setEnabled(false);
    assertEquals(2, counter.get());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_COMMISSION).setUpperBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(4, counter.get());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_COMMISSION).setSearchType(SearchType.GREATER_THAN);
    assertEquals(5, counter.get());
    criteriaModel.removeCriteriaStateListener(listener);
  }

  @Test
  public void testSearchState() {
    assertFalse(criteriaModel.hasCriteriaStateChanged());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_JOB).setLikeValue("job");
    assertTrue(criteriaModel.hasCriteriaStateChanged());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_JOB).setEnabled(false);
    assertFalse(criteriaModel.hasCriteriaStateChanged());
    criteriaModel.getPropertyCriteriaModel(TestDomain.EMP_JOB).setEnabled(true);
    assertTrue(criteriaModel.hasCriteriaStateChanged());
    criteriaModel.rememberCurrentCriteriaState();
    assertFalse(criteriaModel.hasCriteriaStateChanged());
  }

  @Test
  public void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    final String wildcardValue = wildcard + "test" + wildcard;
    criteriaModel.setSimpleCriteriaString(value);
    for (final PropertyCriteriaModel model : criteriaModel.getPropertyCriteriaModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertEquals(wildcardValue, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    criteriaModel.setSimpleCriteriaString(null);
    for (final PropertyCriteriaModel model : criteriaModel.getPropertyCriteriaModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
