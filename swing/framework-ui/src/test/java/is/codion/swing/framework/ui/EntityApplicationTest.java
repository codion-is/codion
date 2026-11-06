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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.user.User;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityApplicationTest {

	@Test
	void connection() {
		// In-memory preferences (application-level and model), so the test never touches the real user store.
		Preferences preferences = jsonPreferences();
		// Always the same super username, connection created eagerly, so must be an existing user, tests use assertSame()
		User user = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(connection -> new TestApplicationModel(connection))
						.name("Name")
						.clientType("ClientType")
						.onStarted(panel -> {
							assertSame(user, panel.model().connection().user());
							assertEquals("ClientType", panel.model().connection().clientType());
						})
						.user(user)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user2 = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(connection -> new TestApplicationModel(connection))
						// the name is not the client type
						.name("Name")
						.onStarted(panel -> {
							assertSame(user2, panel.model().connection().user());
							assertEquals(TestDomain.DOMAIN.name(), panel.model().connection().clientType());
						})
						.user(() -> user2)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user3 = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model(connection -> new TestApplicationModel(connection))
						.onStarted(panel -> assertSame(user3, panel.model().connection().user()))
						.user(user3)
						.connection(usr -> LocalEntityConnection.builder()
										.domain(new TestDomain())
										.user(usr)
										.build())
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		User user4 = User.user("sa");
		LocalEntityConnection connection = LocalEntityConnection.builder()
						.domain(new TestDomain())
						.user(user4)
						.build();
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						// a connection given, no user supplied, the application user the connection's
						.model((configured, applicationUser) -> new TestApplicationModel(configured, applicationUser))
						.onStarted(panel -> {
							assertSame(connection, panel.model().connection());
							assertSame(user4, panel.model().connection().user());
							assertSame(user4, panel.model().user());
						})
						.connection(connection)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
	}

	@Test
	void applicationUser() {
		Preferences preferences = jsonPreferences();
		// the user logging in, the connection opened as another, the database's
		User login = User.parse("john:secret");
		User database = User.user("sa");
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(preferences)
						.model((connection, user) -> new TestApplicationModel(connection, user))
						.onStarted(panel -> {
							TestApplicationModel model = panel.model();
							assertSame(login, model.user());
							assertSame(database, model.connection().user());
							// the frame title, the application user at the connection's description
							String connectionInfo = "JOHN" + model.connection().description()
											.filter(description -> !description.isEmpty())
											.map(description -> "@" + description.toUpperCase())
											.orElse("");
							assertTrue(Ancestor.ofType(JFrame.class).of(panel).get().getTitle().endsWith(connectionInfo));
							// the default username saved, the application user's
							Preferences stored = jsonPreferences();
							panel.store(stored);
							assertEquals("john", ApplicationPreferences.load(stored).defaultLoginUser().username());
						})
						.user(login)
						.connection(user -> LocalEntityConnection.builder()
										.domain(new TestDomain())
										.user(database)
										.build())
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
	}

	@Test
	void version() {
		Version version = Version.parse("1.2.3");
		// a connection given, without a client version, the version shown regardless
		LocalEntityConnection connection = LocalEntityConnection.builder()
						.domain(new TestDomain())
						.user(User.user("sa"))
						.build();
		AtomicReference<TestApplicationPanel> started = new AtomicReference<>();
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.preferences(jsonPreferences())
						.model((configured, applicationUser) -> new TestApplicationModel(configured, applicationUser))
						.connection(connection)
						.version(version)
						.onStarted(started::set)
						.startupDialog(false)
						.displayFrame(false)
						.start(false);
		TestApplicationPanel panel = started.get();
		assertFalse(panel.model().connection().clientVersion().isPresent());
		assertTrue(Ancestor.ofType(JFrame.class).of(panel).get().getTitle().contains(version.toString()));
		assertTrue(labels(panel.createAboutPanel()).contains(version.toString()));
	}

	private static List<String> labels(Container container) {
		List<String> labels = new ArrayList<>();
		for (Component component : container.getComponents()) {
			if (component instanceof JLabel) {
				labels.add(((JLabel) component).getText());
			}
			else if (component instanceof Container) {
				labels.addAll(labels((Container) component));
			}
		}

		return labels;
	}

	// Constructors of both arities, a method reference to them ambiguous between the two model() overloads
	public static class TestApplicationModel extends SwingEntityApplicationModel {

		public TestApplicationModel(EntityConnection connection) {
			super(connection, emptyList());
		}

		public TestApplicationModel(EntityConnection connection, User user) {
			super(connection, user, emptyList());
		}
	}

	public static class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public TestApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel, emptyList(), emptyList());
		}
	}
}
