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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertSame;

public final class DefaultEntityApplicationPanelBuilderTest {

	@Test
	void connection() {
		User user = User.user("Test");
		EntityApplicationPanel.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.onApplicationStarted(panel -> assertSame(user, panel.applicationModel().connectionProvider().user()))
						.domainType(TestDomain.DOMAIN)
						.user(user)
						.displayStartupDialog(false)
						.displayFrame(false)
						.start(false);
		User user2 = User.user("Test2");
		EntityApplicationPanel.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.onApplicationStarted(panel -> assertSame(user2, panel.applicationModel().connectionProvider().user()))
						.domainType(TestDomain.DOMAIN)
						.user(() -> user2)
						.displayStartupDialog(false)
						.displayFrame(false)
						.start(false);
		User user3 = User.user("Test3");
		EntityApplicationPanel.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.onApplicationStarted(panel -> assertSame(user3, panel.applicationModel().connectionProvider().user()))
						.user(user3)
						.connectionProvider(usr -> LocalEntityConnectionProvider.builder()
										.domain(new TestDomain())
										.user(usr)
										.build())
						.displayStartupDialog(false)
						.displayFrame(false)
						.start(false);
		User user4 = User.user("Test3");
		LocalEntityConnectionProvider connectionProvider = LocalEntityConnectionProvider.builder()
						.domain(new TestDomain())
						.user(user4)
						.build();
		EntityApplicationPanel.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.onApplicationStarted(panel -> {
							assertSame(connectionProvider, panel.applicationModel().connectionProvider());
							assertSame(user4, panel.applicationModel().connectionProvider().user());
						})
						.connectionProvider(connectionProvider)
						.displayStartupDialog(false)
						.displayFrame(false)
						.start(false);
	}

	public static class TestApplicationModel extends SwingEntityApplicationModel {

		public TestApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider);
		}
	}

	public static class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public TestApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel);
		}

		@Override
		protected List<EntityPanel> createEntityPanels() {
			return emptyList();
		}
	}
}
