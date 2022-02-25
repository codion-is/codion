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
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final UsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> SCENARIO
          = new AbstractEntityUsageScenario<SchemaBrowserAppPanel.SchemaBrowserApplicationModel>() {
    @Override
    protected void perform(SchemaBrowserAppPanel.SchemaBrowserApplicationModel application) {
      SwingEntityModel schemaModel = application.getEntityModels().iterator().next();
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
    SchemaBrowserAppPanel.SchemaBrowserApplicationModel applicationModel =
            new SchemaBrowserAppPanel.SchemaBrowserApplicationModel(
                    EntityConnectionProvider.connectionProvider().setDomainClassName(SchemaBrowser.class.getName())
                            .setClientTypeId(getClass().getSimpleName()).setUser(getUser()));
    SwingEntityModel schemaModel = applicationModel.getEntityModel(Schema.TYPE);
    SwingEntityModel dbObjectModel = schemaModel.getDetailModel(Table.TYPE);
    schemaModel.addLinkedDetailModel(dbObjectModel);
    SwingEntityModel columnModel = dbObjectModel.getDetailModel(Column.TYPE);
    SwingEntityModel constraintModel = dbObjectModel.getDetailModel(Constraint.TYPE);
    dbObjectModel.addDetailModel(columnModel);
    dbObjectModel.addDetailModel(constraintModel);
    SwingEntityModel constraintColumnModel = dbObjectModel.getDetailModel(ConstraintColumn.TYPE);
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
