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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petstore.testing;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.petstore.domain.Petstore;
import is.codion.framework.demos.petstore.model.PetstoreAppModel;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.tools.loadtest.EntityLoadTestModel;

import static java.util.Collections.singletonList;

public final class PetstoreLoadTest extends EntityLoadTestModel<PetstoreAppModel> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  public PetstoreLoadTest() {
    super(UNIT_TEST_USER, singletonList(new AbstractUsageScenario<PetstoreAppModel>("selectRecords") {
      @Override
      protected void perform(PetstoreAppModel application) {
        SwingEntityModel categoryModel = application.entityModels().iterator().next();
        categoryModel.tableModel().selectionModel().clearSelection();
        categoryModel.tableModel().refresh();
        selectRandomRow(categoryModel.tableModel());
        selectRandomRow(categoryModel.detailModels().iterator().next().tableModel());
        selectRandomRow(categoryModel.detailModels().iterator().next().detailModels().iterator().next().tableModel());
      }
    }));
  }

  @Override
  protected PetstoreAppModel createApplication(User user) throws CancelException {
    PetstoreAppModel applicationModel = new PetstoreAppModel(
            EntityConnectionProvider.builder()
                    .domainType(Petstore.DOMAIN)
                    .clientTypeId(getClass().getSimpleName())
                    .user(user)
                    .build());
    SwingEntityModel categoryModel = applicationModel.entityModels().iterator().next();
    categoryModel.detailModelLink(categoryModel.detailModels().iterator().next()).active().set(true);
    SwingEntityModel productModel = categoryModel.detailModels().iterator().next();
    productModel.detailModelLink(productModel.detailModels().iterator().next()).active().set(true);
    SwingEntityModel itemModel = productModel.detailModels().iterator().next();
    itemModel.detailModelLink(itemModel.detailModels().iterator().next()).active().set(true);

    return applicationModel;
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new PetstoreLoadTest().loadTestModel()).run();
  }
}