/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.ui.LoadTestPanel;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.framework.server.provider.EntityDbRemoteProvider;
import org.jminor.framework.tools.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.UUID;

/**
 * User: Bjorn Darri
 * Date: 30.11.2007
 * Time: 04:00:43
 */
public final class SchemaBrowserLoadTest extends EntityLoadTestModel {

  public SchemaBrowserLoadTest() {
    super(User.UNIT_TEST_USER);
  }

  @Override
  protected void loadDomainModel() {
    new SchemaBrowser();
  }

  @Override
  protected void performWork(final Object application) {
    final EntityModel schemaModel = ((EntityApplicationModel) application).getMainApplicationModels().iterator().next();
    schemaModel.getTableModel().refresh();
    selectRandomRow(schemaModel.getTableModel());
    selectRandomRow(schemaModel.getDetailModels().iterator().next().getTableModel());
    selectRandomRow(schemaModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new DefaultEntityApplicationModel(new EntityDbRemoteProvider(getUser(),
           UUID.randomUUID(), getClass().getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        new SchemaBrowser();
      }
    };
    final EntityModel schemaModel = applicationModel.getMainApplicationModel(SchemaBrowser.T_SCHEMA);
    final EntityModel dbObjectModel = schemaModel.getDetailModel(SchemaBrowser.T_TABLE);
    schemaModel.setLinkedDetailModels(dbObjectModel);
    final EntityModel columnModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN);
    final EntityModel constraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_CONSTRAINT);
    dbObjectModel.addDetailModel(columnModel);
    dbObjectModel.addDetailModel(constraintModel);
    final EntityModel columnConstraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN_CONSTRAINT);
    constraintModel.addDetailModel(columnConstraintModel);
    dbObjectModel.setLinkedDetailModels(columnModel);

    return applicationModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
          new LoadTestPanel(new SchemaBrowserLoadTest()).showFrame();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}
