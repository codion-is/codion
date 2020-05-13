/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.schemabrowser.testing;

import dev.codion.common.model.CancelException;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.framework.db.EntityConnectionProviders;
import dev.codion.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel;
import dev.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import dev.codion.swing.common.tools.loadtest.UsageScenario;
import dev.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import dev.codion.swing.framework.model.SwingEntityModel;
import dev.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

import static java.util.Collections.singletonList;

public final class SchemaBrowserLoadTest extends EntityLoadTestModel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

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
    super(UNIT_TEST_USER, singletonList(SCENARIO));
  }

  @Override
  protected SchemaBrowserAppPanel.SchemaBrowserApplicationModel initializeApplication() throws CancelException {
    final SchemaBrowserAppPanel.SchemaBrowserApplicationModel applicationModel =
            new SchemaBrowserAppPanel.SchemaBrowserApplicationModel(
                    EntityConnectionProviders.connectionProvider().setDomainClassName(SchemaBrowser.class.getName())
                            .setClientTypeId(getClass().getSimpleName()).setUser(getUser()));
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
        new LoadTestPanel(new SchemaBrowserLoadTest()).showFrame();
      }
      catch (final Exception e) {
        e.printStackTrace();
      }
    }
  }
}
