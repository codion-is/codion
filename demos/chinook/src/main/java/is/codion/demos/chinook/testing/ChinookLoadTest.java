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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.testing;

import is.codion.common.utilities.user.User;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.model.ChinookAppModel;
import is.codion.demos.chinook.testing.scenarios.InsertDeleteAlbum;
import is.codion.demos.chinook.testing.scenarios.InsertDeleteInvoice;
import is.codion.demos.chinook.testing.scenarios.LogoutLogin;
import is.codion.demos.chinook.testing.scenarios.RaisePrices;
import is.codion.demos.chinook.testing.scenarios.RandomPlaylist;
import is.codion.demos.chinook.testing.scenarios.UpdateTotals;
import is.codion.demos.chinook.testing.scenarios.ViewAlbum;
import is.codion.demos.chinook.testing.scenarios.ViewCustomerReport;
import is.codion.demos.chinook.testing.scenarios.ViewGenre;
import is.codion.demos.chinook.testing.scenarios.ViewInvoice;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.tools.loadtest.LoadTest;
import is.codion.tools.loadtest.Scenario;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static is.codion.tools.loadtest.Scenario.scenario;
import static is.codion.tools.loadtest.model.LoadTestModel.loadTestModel;
import static is.codion.tools.loadtest.ui.LoadTestPanel.loadTestPanel;

public final class ChinookLoadTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final Collection<Scenario<EntityConnectionProvider>> SCENARIOS = List.of(
					scenario(new ViewGenre(), 10),
					scenario(new ViewCustomerReport(), 2),
					scenario(new ViewInvoice(), 10),
					scenario(new ViewAlbum(), 10),
					scenario(new UpdateTotals(), 1),
					scenario(new InsertDeleteAlbum(), 3),
					scenario(new LogoutLogin(), 1),
					scenario(new RaisePrices(), 1),
					scenario(new RandomPlaylist(), 1),
					scenario(new InsertDeleteInvoice(), 3));

	private static final class ConnectionProviderFactory implements Function<User, EntityConnectionProvider> {

		@Override
		public EntityConnectionProvider apply(User user) {
			EntityConnectionProvider connectionProvider = EntityConnectionProvider.builder()
							.domain(Chinook.DOMAIN)
							.clientType(ChinookLoadTest.class.getSimpleName())
							.clientVersion(ChinookAppModel.VERSION)
							.user(user)
							.build();
			connectionProvider.connection();

			return connectionProvider;
		}
	}

	public static void main(String[] args) {
		LoadTest<EntityConnectionProvider> loadTest =
						LoadTest.builder()
										.createApplication(new ConnectionProviderFactory())
										.closeApplication(EntityConnectionProvider::close)
										.scenarios(SCENARIOS)
										.user(UNIT_TEST_USER)
										.name("Chinook LoadTest " + EntityConnectionProvider.CLIENT_CONNECTION_TYPE.get())
										.build();
		loadTestPanel(loadTestModel(loadTest)).run();
	}
}
