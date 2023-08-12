/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.client.ui.SchemaBrowserAppPanel;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.TableColumn;
import is.codion.swing.common.model.tools.loadtest.UsageScenario;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

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
  protected SchemaBrowserAppPanel.SchemaBrowserApplicationModel createApplication(User user) throws CancelException {
    SchemaBrowserAppPanel.SchemaBrowserApplicationModel applicationModel =
            new SchemaBrowserAppPanel.SchemaBrowserApplicationModel(
                    EntityConnectionProvider.builder()
                            .domainType(SchemaBrowser.DOMAIN)
                            .clientTypeId(getClass().getSimpleName())
                            .user(user)
                            .build());
    SwingEntityModel schemaModel = applicationModel.entityModel(Schema.TYPE);
    SwingEntityModel tableModel = schemaModel.detailModel(Table.TYPE);
    schemaModel.detailModelLink(tableModel).setActive(true);
    SwingEntityModel columnModel = tableModel.detailModel(TableColumn.TYPE);
    SwingEntityModel constraintModel = tableModel.detailModel(Constraint.TYPE);
    tableModel.addDetailModel(columnModel);
    tableModel.addDetailModel(constraintModel);
    SwingEntityModel constraintColumnModel = tableModel.detailModel(ConstraintColumn.TYPE);
    constraintModel.addDetailModel(constraintColumnModel);
    tableModel.detailModelLink(columnModel).setActive(true);

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new SchemaBrowserLoadTest()).run();
  }
}
