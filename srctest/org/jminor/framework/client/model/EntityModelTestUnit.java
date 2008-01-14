/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.UserException;
import org.jminor.framework.model.AbstractEntityTestUnit;
import org.jminor.framework.model.Entity;

import java.util.List;

public abstract class EntityModelTestUnit extends AbstractEntityTestUnit {

  private final EntityModel testModel;

  public EntityModelTestUnit(final String name, final AbstractEntityModelTestFixture fixture) throws UserException {
    super(name, fixture);
    this.testModel = fixture.initializeEntityModel();
    setEntityID(testModel.getEntityID());
  }

  /** {@inheritDoc} */
  public String getEntityID() {
    return testModel.getEntityID();
  }

  public void testModel() throws Exception {
    final EntityTableModel tableModel = testModel.getTableModel();
    tableModel.setQueryRangeEnabled(false);
    tableModel.refresh();
    final int rowCount = tableModel.getRowCount();
    final List<Entity> entities = createTestEntities();
    tableModel.forceRefresh();
    int newRowCount = tableModel.getRowCount();
    assertEquals(rowCount + entities.size(), newRowCount);

    tableModel.setSelectedEntities(entities);
    testModel.delete();
    testModel.getTableModel().refresh();
    newRowCount = tableModel.getRowCount();
    assertEquals(rowCount, newRowCount);
  }
}
