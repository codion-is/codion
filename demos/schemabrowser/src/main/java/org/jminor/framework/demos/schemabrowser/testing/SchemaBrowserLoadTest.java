/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.testing;

import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.swing.common.tools.ui.LoadTestPanel;
import org.jminor.swing.framework.model.SwingEntityModel;
import org.jminor.swing.framework.tools.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collections;

public final class SchemaBrowserLoadTest extends EntityLoadTestModel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  private static final UsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> SCENARIO
          = new AbstractEntityUsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel>() {
    @Override
    protected void performScenario(final SchemaBrowserAppPanel.SchemaBrowserApplicationModel application) {
      final SwingEntityModel schemaModel = application.getEntityModels().iterator().next();
      schemaModel.getTableModel().refresh();
      selectRandomRow(schemaModel.getTableModel());
      selectRandomRow(schemaModel.getDetailModels().iterator().next().getTableModel());
      selectRandomRow(schemaModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
    }
  };

  public SchemaBrowserLoadTest() {
    super(UNIT_TEST_USER, Collections.singletonList(SCENARIO));
  }

  @Override
  protected SchemaBrowserAppPanel.SchemaBrowserApplicationModel initializeApplication() throws CancelException {
    final SchemaBrowserAppPanel.SchemaBrowserApplicationModel applicationModel =
            new SchemaBrowserAppPanel.SchemaBrowserApplicationModel(
                    EntityConnectionProviders.connectionProvider(SchemaBrowser.class.getName(),
                            getClass().getSimpleName()).setUser(getUser()));
    final SwingEntityModel schemaModel = applicationModel.getEntityModel(SchemaBrowser.T_SCHEMA);
    final SwingEntityModel dbObjectModel = schemaModel.getDetailModel(SchemaBrowser.T_TABLE);
    schemaModel.addLinkedDetailModel(dbObjectModel);
    final SwingEntityModel columnModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN);
    final SwingEntityModel constraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_CONSTRAINT);
    dbObjectModel.addDetailModel(columnModel);
    dbObjectModel.addDetailModel(constraintModel);
    final SwingEntityModel columnConstraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN_CONSTRAINT);
    constraintModel.addDetailModel(columnConstraintModel);
    dbObjectModel.addLinkedDetailModel(columnModel);

    return applicationModel;
  }

  public static void main(final String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        new LoadTestPanel(new SchemaBrowserLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
