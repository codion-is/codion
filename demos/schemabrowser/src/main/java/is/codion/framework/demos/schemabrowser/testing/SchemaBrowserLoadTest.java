/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
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
    schemaModel.detailModelLink(tableModel).active().set(true);
    SwingEntityModel columnModel = tableModel.detailModel(TableColumn.TYPE);
    SwingEntityModel constraintModel = tableModel.detailModel(Constraint.TYPE);
    tableModel.addDetailModel(columnModel);
    tableModel.addDetailModel(constraintModel);
    SwingEntityModel constraintColumnModel = tableModel.detailModel(ConstraintColumn.TYPE);
    constraintModel.addDetailModel(constraintColumnModel);
    tableModel.detailModelLink(columnModel).active().set(true);

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new SchemaBrowserLoadTest().loadTestModel()).run();
  }
}
