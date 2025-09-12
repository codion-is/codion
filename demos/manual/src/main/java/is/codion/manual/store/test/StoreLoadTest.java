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
package is.codion.manual.store.test;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.rmi.RemoteEntityConnectionProvider;
import is.codion.framework.model.EntityTableModel;
import is.codion.manual.store.domain.Store;
import is.codion.manual.store.domain.Store.Customer;
import is.codion.manual.store.model.StoreApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.LoadTest.Scenario.Performer;

import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static is.codion.tools.loadtest.LoadTest.Scenario.scenario;
import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;

// tag::storeLoadTest[]
public class StoreLoadTest {

	private static final class StoreApplicationModelFactory
					implements Function<User, StoreApplicationModel> {

		@Override
		public StoreApplicationModel apply(User user) {
			EntityConnectionProvider connectionProvider =
							RemoteEntityConnectionProvider.builder()
											.user(user)
											.domain(Store.DOMAIN)
											.build();

			return new StoreApplicationModel(connectionProvider);
		}
	}

	private static class StoreScenarioPerformer
					implements Performer<StoreApplicationModel> {

		private static final Random RANDOM = new Random();

		@Override
		public void perform(StoreApplicationModel application) {
			SwingEntityModel customerModel = application.entityModels().get(Customer.TYPE);
			customerModel.tableModel().items().refresh();
			selectRandomRow(customerModel.tableModel());
		}

		private static void selectRandomRow(EntityTableModel<?> tableModel) {
			if (tableModel.items().included().size() > 0) {
				tableModel.selection().index().set(RANDOM.nextInt(tableModel.items().included().size()));
			}
		}
	}

	public static void main(String[] args) {
		LoadTest<StoreApplicationModel> loadTest =
						LoadTest.builder()
										.createApplication(new StoreApplicationModelFactory())
										.closeApplication(application -> application.connectionProvider().close())
										.user(User.parse("scott:tiger"))
										.scenarios(List.of(scenario(new StoreScenarioPerformer())))
										.name("Store LoadTest - " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
										.build();
		loadTestPanel(loadTestModel(loadTest)).run();
	}
}
// end::storeLoadTest[]