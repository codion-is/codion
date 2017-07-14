/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.AbstractEntityTableModelTest;
import org.jminor.framework.model.TestDomain;
import org.jminor.javafx.framework.ui.EntityTableView;

import javafx.embed.swing.JFXPanel;

import java.util.Arrays;
import java.util.List;

public final class FXEntityListModelTest extends AbstractEntityTableModelTest<FXEntityEditModel, FXEntityListModel> {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.createInstance());

  static {
    new JFXPanel();
  }

  @Override
  protected FXEntityListModel createTestTableModel() {
    return new EntityTableModelTmp();
  }

  @Override
  protected FXEntityListModel createMasterTableModel() {
    return new FXEntityListModel(TestDomain.T_MASTER, CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createDetailTableModel() {
    return new FXEntityListModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityListModel createEmployeeTableModelWithoutEditModel() {
    final FXEntityListModel listModel = new FXEntityListModel(TestDomain.T_EMP, CONNECTION_PROVIDER);
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
  protected FXEntityEditModel createDepartmentEditModel() {
    return new FXEntityEditModel(TestDomain.T_MASTER, CONNECTION_PROVIDER);
  }

  @Override
  protected FXEntityEditModel createDetailEditModel() {
    return new FXEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
  }

  public static final class EntityTableModelTmp extends FXEntityListModel {

    private final Entity[] entities = initTestEntities(new Entity[5]);

    public EntityTableModelTmp() {
      super(TestDomain.T_DETAIL, CONNECTION_PROVIDER);
      setEditModel(new FXEntityEditModel(TestDomain.T_DETAIL, CONNECTION_PROVIDER));
    }
    @Override
    protected List<Entity> performQuery() {
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
