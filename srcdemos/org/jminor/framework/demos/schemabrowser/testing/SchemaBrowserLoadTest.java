/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.schemabrowser.client.SchemaBrowserAppModel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.UIManager;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 04:00:43
 */
public class SchemaBrowserLoadTest extends EntityLoadTestModel {

  public SchemaBrowserLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  @Override
  protected void loadDomainModel() {
    new SchemaBrowser();
  }

  @Override
  protected void performWork(final Object applicationModel) {
    final EntityModel schemaModel = ((EntityApplicationModel) applicationModel).getMainApplicationModels().iterator().next();
    schemaModel.getTableModel().refresh();
    selectRandomRow(schemaModel.getTableModel());
    selectRandomRow(schemaModel.getDetailModels().get(0).getTableModel());
    selectRandomRow(schemaModel.getDetailModels().get(0).getDetailModels().get(0).getTableModel());
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel =
            new SchemaBrowserAppModel(new EntityDbRemoteProvider(getUser(), User.UNIT_TEST_USER +"@"+new Object(), getClass().getSimpleName()));
    final EntityModel schemaModel = applicationModel.getMainApplicationModels().iterator().next();
    schemaModel.setLinkedDetailModel(schemaModel.getDetailModels().get(0));
    final EntityModel dbObjectModel = schemaModel.getDetailModels().get(0);
    dbObjectModel.setLinkedDetailModel(dbObjectModel.getDetailModels().get(0));

    return applicationModel;
  }

  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    new LoadTestPanel(new SchemaBrowserLoadTest()).showFrame();
  }
}
