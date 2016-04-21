/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.framework.db.EntityConnectionProvidersTest;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.TestDomain;
import org.jminor.framework.model.AbstractEntityTableModelTest;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.embed.swing.JFXPanel;

import java.util.Arrays;
import java.util.List;

public final class FXEntityListModelTest extends AbstractEntityTableModelTest<FXEntityListModel> {

  static {
    new JFXPanel();
  }

  @Override
  protected FXEntityListModel createTestTableModel() {
    return new EntityTableModelTmp();
  }

  @Override
  protected FXEntityListModel createMasterTableModel() {
    return new FXEntityListModel(TestDomain.T_MASTER, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createEmployeeTableModelWithoutEditModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
    new EntityTableView(listModel);

    return listModel;
  }

  @Override
  protected FXEntityListModel createDepartmentTableModel() {
    final FXEntityListModel deptModel = new FXEntityListModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider());
    deptModel.setEditModel(new FXEntityEditModel(TestDomain.T_DEPARTMENT, testModel.getConnectionProvider()));
    new EntityTableView(deptModel);

    return deptModel;
  }

  @Override
  protected FXEntityListModel createEmployeeTableModel() {
    final FXEntityListModel empModel = new FXEntityListModel(TestDomain.T_EMP, testModel.getConnectionProvider());
    empModel.setEditModel(new FXEntityEditModel(TestDomain.T_EMP, testModel.getConnectionProvider()));
    new EntityTableView(empModel);

    return empModel;
  }

  @Override
  protected EntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_MASTER, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  @Override
  protected EntityEditModel createDetailEditModel() {
    return new FXEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
  }

  public static final class EntityTableModelTmp extends FXEntityListModel {

    private final Entity[] entities = initTestEntities(new Entity[5]);

    public EntityTableModelTmp() {
      super(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER);
      setEditModel(new FXEntityEditModel(TestDomain.T_DETAIL, EntityConnectionProvidersTest.CONNECTION_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery(final Criteria criteria) {
      return Arrays.asList(entities);
    }
  }

  private static Entity[] initTestEntities(final Entity[] testEntities) {
    final String[] stringValues = new String[]{"a", "b", "c", "d", "e"};
    for (int i = 0; i < testEntities.length; i++) {
      testEntities[i] = Entities.entity(TestDomain.T_DETAIL);
      testEntities[i].put(TestDomain.DETAIL_ID, (long) i+1);
      testEntities[i].put(TestDomain.DETAIL_INT, i+1);
      testEntities[i].put(TestDomain.DETAIL_STRING, stringValues[i]);
    }

    return testEntities;
  }
}
