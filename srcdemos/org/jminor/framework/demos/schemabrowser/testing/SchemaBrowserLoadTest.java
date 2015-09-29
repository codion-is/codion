/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.testing;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.db.remote.RemoteEntityConnectionProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.swing.common.ui.tools.LoadTestPanel;
import org.jminor.swing.framework.model.DefaultEntityApplicationModel;
import org.jminor.swing.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.EntityModel;
import org.jminor.swing.framework.testing.EntityLoadTestModel;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.Collections;
import java.util.UUID;

public final class SchemaBrowserLoadTest extends EntityLoadTestModel {

  private static final UsageScenario<EntityApplicationModel> SCENARIO = new AbstractEntityUsageScenario() {
    @Override
    protected void performScenario(final EntityApplicationModel application) {
      final EntityModel schemaModel = application.getEntityModels().iterator().next();
      schemaModel.getTableModel().refresh();
      selectRandomRow(schemaModel.getTableModel());
      selectRandomRow(schemaModel.getDetailModels().iterator().next().getTableModel());
      selectRandomRow(schemaModel.getDetailModels().iterator().next().getDetailModels().iterator().next().getTableModel());
    }
  };

  public SchemaBrowserLoadTest() {
    super(User.UNIT_TEST_USER, Collections.singletonList(SCENARIO));
  }

  @Override
  protected EntityApplicationModel initializeApplication() throws CancelException {
    final EntityApplicationModel applicationModel = new DefaultEntityApplicationModel(new RemoteEntityConnectionProvider(getUser(),
            UUID.randomUUID(), getClass().getSimpleName())) {
      @Override
      protected void loadDomainModel() {
        SchemaBrowser.init();
      }
    };
    final EntityModel schemaModel = applicationModel.getEntityModel(SchemaBrowser.T_SCHEMA);
    final EntityModel dbObjectModel = schemaModel.getDetailModel(SchemaBrowser.T_TABLE);
    schemaModel.addLinkedDetailModel(dbObjectModel);
    final EntityModel columnModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN);
    final EntityModel constraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_CONSTRAINT);
    dbObjectModel.addDetailModel(columnModel);
    dbObjectModel.addDetailModel(constraintModel);
    final EntityModel columnConstraintModel = dbObjectModel.getDetailModel(SchemaBrowser.T_COLUMN_CONSTRAINT);
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
