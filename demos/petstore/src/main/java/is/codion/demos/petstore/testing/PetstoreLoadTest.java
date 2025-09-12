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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.petstore.testing;

import is.codion.common.user.User;
import is.codion.demos.petstore.domain.Petstore;
import is.codion.demos.petstore.model.PetstoreAppModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.LoadTest.Scenario;
import is.codion.tools.loadtest.LoadTest.Scenario.Performer;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;

public final class PetstoreLoadTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final class PetstoreAppModelFactory
					implements Function<User, PetstoreAppModel> {

		@Override
		public PetstoreAppModel apply(User user) {
			PetstoreAppModel applicationModel = new PetstoreAppModel(
							EntityConnectionProvider.builder()
											.domain(Petstore.DOMAIN)
											.clientType(PetstoreLoadTest.class.getSimpleName())
											.user(user)
											.build());
			SwingEntityModel categoryModel = applicationModel.entityModels().get().iterator().next();
			categoryModel.detailModels().active(categoryModel.detailModels().get().keySet().iterator().next()).set(true);
			SwingEntityModel productModel = categoryModel.detailModels().get().keySet().iterator().next();
			productModel.detailModels().active(productModel.detailModels().get().keySet().iterator().next()).set(true);
			SwingEntityModel itemModel = productModel.detailModels().get().keySet().iterator().next();
			itemModel.detailModels().active(itemModel.detailModels().get().keySet().iterator().next()).set(true);

			return applicationModel;
		}
	}

	private static final class PetstoreUsageScenario
					implements Performer<PetstoreAppModel> {

		private static final Random RANDOM = new Random();

		@Override
		public void perform(PetstoreAppModel application) {
			SwingEntityModel categoryModel = application.entityModels().get().iterator().next();
			categoryModel.tableModel().selection().clear();
			categoryModel.tableModel().items().refresh();
			selectRandomRow(categoryModel.tableModel());
			selectRandomRow(categoryModel.detailModels().get().keySet().iterator().next().tableModel());
			selectRandomRow(categoryModel.detailModels().get().keySet().iterator().next().detailModels().get().keySet().iterator().next().tableModel());
		}

		private static void selectRandomRow(EntityTableModel<?> tableModel) {
			if (tableModel.items().included().size() > 0) {
				tableModel.selection().index().set(RANDOM.nextInt(tableModel.items().included().size()));
			}
		}
	}

	public static void main(String[] args) {
		LoadTest<PetstoreAppModel> loadTest =
						LoadTest.builder()
										.createApplication(new PetstoreAppModelFactory())
										.closeApplication(application -> application.connectionProvider().close())
										.user(UNIT_TEST_USER)
										.scenarios(List.of(Scenario.builder(new PetstoreUsageScenario())
														.name("selectRecords")
														.build()))
										.name("Petstore LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
										.build();
		loadTestPanel(loadTestModel(loadTest)).run();
	}
}