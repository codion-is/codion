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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.filter.SortOrder;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static is.codion.swing.framework.ui.EntityApplicationPanel.ControlKeys.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityApplicationPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@AfterEach
	void tearDown() {
		Thread.setDefaultUncaughtExceptionHandler(null);
	}

	@Test
	void test() {
		EntityConnection.CLIENT_CONNECTION_TYPE.set(EntityConnection.CONNECTION_TYPE_LOCAL);
		// In-memory preferences for both the application level and the model, so the test never touches the real store.
		Preferences preferences = jsonPreferences();
		EntityApplication.builder(TestApplicationModel.class, TestApplicationPanel.class)
						.domain(TestDomain.DOMAIN)
						.user(UNIT_TEST_USER)
						.preferences(preferences)
						.model(TestApplicationModel::new)
						.uncaughtExceptionHandler(false)
						.saveDefaultUsername(false)
						.displayFrame(false)
						.mainMenu(true)
						.startupDialog(false)
						.start(false);
	}

	@Test
	void preferencesModelAndViewRoundTrip() {
		// The model walk and the UI walk share the entities/<key> node, writing disjoint model/ and view/ subtrees
		Preferences entities = jsonPreferences();

		SwingEntityModel model = new SwingEntityModel(Employee.TYPE, CONNECTION);
		model.tableModel().query().conditions().get(Employee.NAME).caseSensitive().set(true); // model state
		EntityPanel panel = new EntityPanel(model);
		panel.tablePanel().table().columns().visible(Employee.COMMISSION).set(false); // view state

		model.store(entities);
		panel.store(entities.node(panel.preferencesKey()));

		SwingEntityModel restoredModel = new SwingEntityModel(Employee.TYPE, CONNECTION);
		EntityPanel restoredPanel = new EntityPanel(restoredModel);
		restoredModel.restore(entities);
		restoredPanel.restore(entities.node(restoredPanel.preferencesKey()));

		assertTrue(restoredModel.tableModel().query().conditions().get(Employee.NAME).caseSensitive().is());
		assertFalse(restoredPanel.tablePanel().table().columns().visible(Employee.COMMISSION).is());
	}

	@Test
	void dependencyScratchRoundTrip() {
		// The dependency panel captures model (sort) and view (columns) state into a single in-memory scratch node
		Preferences scratch = jsonPreferences();

		SwingEntityModel model = new SwingEntityModel(Employee.TYPE, CONNECTION);
		EntityPanel panel = new EntityPanel(model);
		model.tableModel().sort().ascending(Employee.NAME); // model state
		panel.tablePanel().table().columns().visible(Employee.COMMISSION).set(false); // view state

		model.tableModel().store(scratch);
		panel.tablePanel().store(scratch);

		SwingEntityModel restoredModel = new SwingEntityModel(Employee.TYPE, CONNECTION);
		EntityPanel restoredPanel = new EntityPanel(restoredModel);
		restoredModel.tableModel().restore(scratch);
		restoredPanel.tablePanel().restore(scratch);

		assertEquals(SortOrder.ASCENDING, restoredModel.tableModel().sort().columns().get(Employee.NAME).sortOrder());
		assertFalse(restoredPanel.tablePanel().table().columns().visible(Employee.COMMISSION).is());
	}

	private static final class TestApplicationModel extends SwingEntityApplicationModel {

		public TestApplicationModel(EntityConnection connection) {
			super(connection, singletonList(new SwingEntityModel(Employee.TYPE, connection)));
		}
	}

	// Its lookup panel in the Lookup menu
	private static final class LookupApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public LookupApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel, TestApplicationPanel.createPanels(applicationModel), singletonList(EntityPanel.builder()
							.entityType(Employee.TYPE)
							.panel(connection -> new EntityPanel(new SwingEntityModel(Employee.TYPE, connection)))));
		}
	}

	private static final class TestApplicationPanel extends EntityApplicationPanel<TestApplicationModel> {

		public TestApplicationPanel(TestApplicationModel applicationModel) {
			super(applicationModel, createPanels(applicationModel), emptyList());
		}

		private static List<EntityPanel> createPanels(TestApplicationModel applicationModel) {
			return singletonList(new EntityPanel(applicationModel.models().get(Employee.TYPE)));
		}
	}

	@Test
	void mainMenu() {
		// the defaults, the empty Tools menu left out, as is the Lookup menu without lookup panels
		TestApplicationPanel panel = initialized(() -> new TestApplicationPanel(new TestApplicationModel(CONNECTION)), configured -> {});
		JMenuBar menuBar = panel.createMenuBar().orElseThrow(IllegalStateException::new);
		assertEquals(asList(caption(panel, FILE_MENU), caption(panel, VIEW_MENU), caption(panel, HELP_MENU)), menus(menuBar));
		assertEquals(singletonList(caption(panel, EXIT)), items(menuBar.getMenu(0)));
		assertEquals(asList(caption(panel, SELECT_LOOK_AND_FEEL), caption(panel, SELECT_SCALING), "|",
						caption(panel, ALWAYS_ON_TOP)), items(menuBar.getMenu(1)));
		List<String> help = new ArrayList<>(asList(caption(panel, HELP), caption(panel, KEYBOARD_SHORTCUTS), "|"));
		Controls log = panel.control(LOG_CONTROLS).getOrThrow();
		if (log.size() > 0) {
			help.addAll(asList(log.caption().orElse(null), "|"));
		}
		help.add(caption(panel, ABOUT));
		assertEquals(help, items(menuBar.getMenu(2)));
	}

	@Test
	void configureMainMenu() {
		// added to a menu, after a control, and a menu after another, the defaults left as they are
		CommandControl custom = Control.builder()
						.command(() -> {})
						.caption("custom")
						.build();
		CommandControl language = Control.builder()
						.command(() -> {})
						.caption("language")
						.build();
		Controls reports = Controls.builder()
						.caption("reports")
						.control(custom)
						.build();
		TestApplicationPanel panel = initialized(() -> new TestApplicationPanel(new TestApplicationModel(CONNECTION)),
						configured -> configured.configureMainMenu(layout -> layout
										.controls(TOOLS_MENU, tools -> tools
														.control(custom))
										.controls(VIEW_MENU, view -> view
														.after(SELECT_SCALING)
														.control(language))
										.after(TOOLS_MENU)
										.control(reports)));
		JMenuBar menuBar = panel.createMenuBar().orElseThrow(IllegalStateException::new);
		assertEquals(asList(caption(panel, FILE_MENU), caption(panel, VIEW_MENU), caption(panel, TOOLS_MENU), "reports",
						caption(panel, HELP_MENU)), menus(menuBar));
		assertEquals(asList(caption(panel, SELECT_LOOK_AND_FEEL), caption(panel, SELECT_SCALING), "language", "|",
						caption(panel, ALWAYS_ON_TOP)), items(menuBar.getMenu(1)));
		assertEquals(singletonList("custom"), items(menuBar.getMenu(2)));
		// before the panel is initialized only
		assertThrows(IllegalStateException.class, () -> panel.configureMainMenu(layout -> {}));
	}

	@Test
	void menuControls() {
		// a control and a menu replaced, and the Lookup menu of the lookup panels
		CommandControl exit = Control.builder()
						.command(() -> {})
						.caption("exit")
						.build();
		Controls help = Controls.builder()
						.caption("help")
						.control(Control.builder()
										.command(() -> {})
										.caption("about"))
						.build();
		LookupApplicationPanel panel = initialized(() -> new LookupApplicationPanel(new TestApplicationModel(CONNECTION)),
						configured -> {
							configured.control(EXIT).set(exit);
							configured.control(HELP_MENU).set(help);
						});
		JMenuBar menuBar = panel.createMenuBar().orElseThrow(IllegalStateException::new);
		assertEquals(asList(caption(panel, FILE_MENU), caption(panel, VIEW_MENU), caption(panel, LOOKUP_MENU),
						caption(panel, HELP_MENU)), menus(menuBar));
		assertEquals(singletonList("exit"), items(menuBar.getMenu(0)));
		assertEquals(singletonList(CONNECTION.entities().definition(Employee.TYPE).caption()), items(menuBar.getMenu(2)));
		assertEquals(singletonList("about"), items(menuBar.getMenu(3)));
	}

	@Test
	void initializeReentrant() {
		boolean userPreferences = EntityApplicationModel.USER_PREFERENCES.getOrThrow();
		EntityApplicationModel.USER_PREFERENCES.set(false);
		try {
			TestApplicationPanel panel = new TestApplicationPanel(new TestApplicationModel(CONNECTION));
			AtomicInteger initializations = new AtomicInteger();
			panel.initialized().addListener(() -> {
				initializations.incrementAndGet();
				//as a nested event loop pumped during the initialization might
				panel.initialize();
			});
			panel.initialize();
			assertEquals(1, initializations.get());
		}
		finally {
			EntityApplicationModel.USER_PREFERENCES.set(userPreferences);
		}
	}

	// Created and initialized without the user preferences, which a panel reads when created, configured in between
	private static <T extends EntityApplicationPanel<?>> T initialized(Supplier<T> panel, Consumer<T> configuration) {
		boolean userPreferences = EntityApplicationModel.USER_PREFERENCES.getOrThrow();
		EntityApplicationModel.USER_PREFERENCES.set(false);
		try {
			T applicationPanel = panel.get();
			configuration.accept(applicationPanel);
			applicationPanel.initialize();

			return applicationPanel;
		}
		finally {
			EntityApplicationModel.USER_PREFERENCES.set(userPreferences);
		}
	}

	private static String caption(EntityApplicationPanel<?> panel, ControlKey<?> controlKey) {
		return panel.control(controlKey).getOrThrow().caption().orElse(null);
	}

	private static List<String> menus(JMenuBar menuBar) {
		List<String> menus = new ArrayList<>();
		for (int i = 0; i < menuBar.getMenuCount(); i++) {
			menus.add(menuBar.getMenu(i).getText());
		}

		return menus;
	}

	// The menu's items, by their text, a separator as |
	private static List<String> items(JMenu menu) {
		List<String> items = new ArrayList<>();
		for (Component component : menu.getMenuComponents()) {
			if (component instanceof JMenuItem) {
				items.add(((JMenuItem) component).getText());
			}
			else if (component instanceof JSeparator) {
				items.add("|");
			}
		}

		return items;
	}
}
