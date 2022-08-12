/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Column;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.swing.common.tools.loadtest.UsageScenario;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import javax.swing.SwingUtilities;

import static java.util.Collections.singletonList;

public final class SchemaBrowserLoadTest extends EntityLoadTestModel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final UsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> SCENARIO
          = new AbstractEntityUsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel>() {
    @Override
    protected void perform(SchemaBrowserAppPanel.SchemaBrowserApplicationModel application) {
      SwingEntityModel schemaModel = application.entityModels().iterator().next();
      schemaModel.tableModel().refresh();
      selectRandomRow(schemaModel.tableModel());
      selectRandomRow(schemaModel.detailModels().iterator().next().tableModel());
      selectRandomRow(schemaModel.detailModels().iterator().next().detailModels().iterator().next().tableModel());
    }
  };

  public SchemaBrowserLoadTest() {
    super(UNIT_TEST_USER, singletonList(SCENARIO));
  }

  @Override
  protected SchemaBrowserAppPanel.SchemaBrowserApplicationModel createApplication() throws CancelException {
    SchemaBrowserAppPanel.SchemaBrowserApplicationModel applicationModel =
            new SchemaBrowserAppPanel.SchemaBrowserApplicationModel(
                    EntityConnectionProvider.builder()
                            .domainClassName(SchemaBrowser.class.getName())
                            .clientTypeId(getClass().getSimpleName())
                            .user(getUser())
                            .build());
    SwingEntityModel schemaModel = applicationModel.entityModel(Schema.TYPE);
    SwingEntityModel dbObjectModel = schemaModel.detailModel(Table.TYPE);
    schemaModel.addLinkedDetailModel(dbObjectModel);
    SwingEntityModel columnModel = dbObjectModel.detailModel(Column.TYPE);
    SwingEntityModel constraintModel = dbObjectModel.detailModel(Constraint.TYPE);
    dbObjectModel.addDetailModel(columnModel);
    dbObjectModel.addDetailModel(constraintModel);
    SwingEntityModel constraintColumnModel = dbObjectModel.detailModel(ConstraintColumn.TYPE);
    constraintModel.addDetailModel(constraintColumnModel);
    dbObjectModel.addLinkedDetailModel(columnModel);

    return applicationModel;
  }

  public static void main(String[] args) throws Exception {
    SwingUtilities.invokeLater(new Runner());
  }

  private static final class Runner implements Runnable {
    @Override
    public void run() {
      try {
        new LoadTestPanel<>(new SchemaBrowserLoadTest()).showFrame();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
