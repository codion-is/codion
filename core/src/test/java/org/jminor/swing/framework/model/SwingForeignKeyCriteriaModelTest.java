package org.jminor.swing.framework.model;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.EntityComboBoxModel;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class SwingForeignKeyCriteriaModelTest {

  @Test
  public void getSearchEntitiesComboBoxModel() throws DatabaseException {
    TestDomain.init();
    final EntityComboBoxModel comboBoxModel = new DefaultEntityComboBoxModel(TestDomain.T_DEPARTMENT, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    final SwingForeignKeyCriteriaModel criteriaModel = new SwingForeignKeyCriteriaModel(
            Entities.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK), comboBoxModel);
    final Entity sales = EntityConnectionProvidersTest.CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    comboBoxModel.setSelectedItem(sales);
    Collection<Entity> searchEntities = criteriaModel.getCriteriaEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));
    comboBoxModel.refresh();
    assertEquals(sales, comboBoxModel.getSelectedValue());
    searchEntities = criteriaModel.getCriteriaEntities();
    assertEquals(1, searchEntities.size());
    assertTrue(searchEntities.contains(sales));

    criteriaModel.setUpperBound((Object) null);
    assertNull(comboBoxModel.getSelectedItem());
    criteriaModel.setUpperBound(sales);
    assertEquals(comboBoxModel.getSelectedItem(), sales);

    comboBoxModel.setSelectedItem(null);

    searchEntities = criteriaModel.getCriteriaEntities();
    assertTrue(searchEntities.isEmpty());
  }
}
