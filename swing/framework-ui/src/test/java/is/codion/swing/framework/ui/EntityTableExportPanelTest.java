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

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.model.EntityExport;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static is.codion.framework.db.EntityConnection.Select.all;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntityTableExportPanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private static final NodePath<AttributeDefinition<?>> ROOT = nodePath();
	private static final NodePath<AttributeDefinition<?>> NAME = path(Employee.NAME);
	private static final NodePath<AttributeDefinition<?>> JOB = path(Employee.JOB);
	private static final NodePath<AttributeDefinition<?>> SALARY = path(Employee.SALARY);
	private static final NodePath<AttributeDefinition<?>> COMMISSION = path(Employee.COMMISSION);
	private static final NodePath<AttributeDefinition<?>> MANAGER = path(Employee.MGR_FK);
	private static final NodePath<AttributeDefinition<?>> MANAGER_NAME = path(Employee.MGR_FK, Employee.NAME);

	@Test
	void exportPreferencesDefaults() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		tablePanel.exportPanel();

		// Nothing is stored at all, the columns are not a preference, only the configuration files
		Preferences prefs = jsonPreferences();
		EntityTablePanelPreferences.store(prefs, tablePanel);
		assertEquals("", prefs.get("export", ""));
	}

	@Test
	void exportExcluded() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(false));
		assertNull(tablePanel.exportModel());
		assertNull(tablePanel.exportPanel());

		// The export preferences are left as they are, for when the export is included again
		Preferences prefs = jsonPreferences();
		String exportPreferences = "{\"configurationFiles\":[\"/configuration.json\"]}";
		prefs.put("export", exportPreferences);
		EntityTablePanelPreferences.restore(prefs, tablePanel);
		EntityTablePanelPreferences.store(prefs, tablePanel);
		assertEquals(exportPreferences, prefs.get("export", ""));
	}

	@Test
	void defaultColumns() {
		EntityTablePanel tablePanel = tablePanel();
		EntityTableExportColumns columns = tablePanel.exportPanel().model().columns();
		// the visible table columns, in order
		assertEquals(tablePanel.table().columns().visible().get(), topLevelAttributes(columns));

		tablePanel.table().columns().visible().set(asList(Employee.JOB, Employee.NAME));
		columns.reset();
		assertEquals(asList(Employee.JOB, Employee.NAME), topLevelAttributes(columns));
	}

	@Test
	void cyclicalForeignKeyExpansion() {
		SwingFilterTreeModel<AttributeDefinition<?>> model = exportColumns().tree();

		// The cyclical self-reference
		NodePath<AttributeDefinition<?>> manager = child(model, ROOT, Employee.MGR_FK);
		assertFalse(model.nodes().loaded(manager), "Not loaded initially");
		assertFalse(model.nodes().leaf(manager), "Not a leaf, showing the expand icon");

		model.expansion().expand(manager);
		assertFalse(model.nodes().children(manager).isEmpty(), "Loaded when expanded");

		// The manager's manager
		NodePath<AttributeDefinition<?>> managersManager = child(model, manager, Employee.MGR_FK);
		assertFalse(model.nodes().loaded(managersManager));

		model.expansion().expand(managersManager);
		assertFalse(model.nodes().children(managersManager).isEmpty());
	}

	@Test
	void attributeNotLoaded() {
		SwingFilterTreeModel<AttributeDefinition<?>> model = exportColumns().tree();
		NodePath<AttributeDefinition<?>> name = child(model, ROOT, Employee.NAME);
		//not a foreign key, so a leaf, which is not loaded, neither when expanded nor when refreshed
		model.expansion().expand(name);
		model.nodes().refresh(name);
		assertFalse(model.nodes().loaded(name));
	}

	@Test
	void toggle() {
		EntityTableExportColumns columns = exportColumns();
		columns.clear();

		columns.toggle(asList(NAME, JOB));
		assertEquals(asList(NAME, JOB), columns.list().items().get());
		// the added ones selected
		assertEquals(asList(NAME, JOB), columns.list().selection().items().get());
		assertTrue(columns.included(NAME));

		// added after the last selected column
		columns.list().selection().items().set(singletonList(NAME));
		columns.toggle(singletonList(SALARY));
		assertEquals(asList(NAME, SALARY, JOB), columns.list().items().get());

		// not all of them columns, the rest added, last in case of no selection
		columns.list().selection().clear();
		columns.toggle(asList(NAME, COMMISSION));
		assertEquals(asList(NAME, SALARY, JOB, COMMISSION), columns.list().items().get());

		// all of them columns, removed
		columns.toggle(asList(NAME, COMMISSION));
		assertEquals(asList(SALARY, JOB), columns.list().items().get());
		assertFalse(columns.included(NAME));

		columns.toggle(singletonList(MANAGER_NAME));
		assertEquals(1, columns.includedCount(MANAGER));
		assertEquals(3, columns.includedCount(ROOT));
	}

	@Test
	void toggleChildren() {
		EntityTableExportColumns columns = exportColumns();
		columns.clear();
		// not loaded
		assertFalse(columns.tree().nodes().loaded(MANAGER));

		columns.toggleChildren(asList(MANAGER, NAME));
		columns.tree().expansion().expand(MANAGER);
		// the attributes shown below it, the hidden ones left out, not foreign keys ignored
		assertEquals(columns.tree().nodes().children(MANAGER), columns.list().items().get());
		assertFalse(columns.included(path(Employee.MGR_FK, Employee.MGR)));

		columns.toggleChildren(singletonList(MANAGER));
		assertTrue(columns.list().items().get().isEmpty());
	}

	@Test
	void move() {
		EntityTableExportColumns columns = exportColumns();
		columns.clear();
		columns.toggle(asList(NAME, JOB, MANAGER_NAME, SALARY));

		// across foreign keys
		columns.move(singletonList(MANAGER_NAME), true);
		assertEquals(asList(NAME, MANAGER_NAME, JOB, SALARY), columns.list().items().get());
		// already first
		columns.move(asList(NAME, MANAGER_NAME), true);
		assertEquals(asList(NAME, MANAGER_NAME, JOB, SALARY), columns.list().items().get());
		// not adjacent
		columns.move(asList(NAME, JOB), false);
		assertEquals(asList(MANAGER_NAME, NAME, SALARY, JOB), columns.list().items().get());
		// already last
		columns.move(asList(SALARY, JOB), false);
		assertEquals(asList(MANAGER_NAME, NAME, SALARY, JOB), columns.list().items().get());
	}

	@Test
	void insert() {
		EntityTableExportColumns columns = exportColumns();
		columns.clear();
		columns.toggle(asList(NAME, JOB, SALARY));

		// dropped onto its own position
		columns.insert(singletonList(JOB), 1);
		assertEquals(asList(NAME, JOB, SALARY), columns.list().items().get());
		columns.insert(singletonList(JOB), 2);
		assertEquals(asList(NAME, JOB, SALARY), columns.list().items().get());

		columns.insert(singletonList(SALARY), 0);
		assertEquals(asList(SALARY, NAME, JOB), columns.list().items().get());
		// after the last
		columns.insert(singletonList(SALARY), 3);
		assertEquals(asList(NAME, JOB, SALARY), columns.list().items().get());

		// dropped from the tree, a new column inserted and a column moved
		columns.insert(asList(COMMISSION, SALARY), 1);
		assertEquals(asList(NAME, COMMISSION, SALARY, JOB), columns.list().items().get());
		assertEquals(asList(COMMISSION, SALARY), columns.list().selection().items().get());
	}

	@Test
	void showHidden() {
		EntityTableExportColumns columns = exportColumns();
		SwingFilterTreeModel<AttributeDefinition<?>> model = columns.tree();
		model.expansion().expand(MANAGER);
		int managerChildren = model.nodes().children(MANAGER).size();
		assertFalse(displayed(model, ROOT, Employee.MGR));

		columns.showHidden().set(true);
		assertTrue(displayed(model, ROOT, Employee.MGR));
		assertTrue(displayed(model, ROOT, Employee.DEPARTMENT));
		//expansion kept, the children of the expanded node now including its hidden attributes
		assertTrue(model.expansion().expanded(MANAGER));
		assertTrue(model.nodes().children(MANAGER).size() > managerChildren);

		//a hidden column stays displayed
		columns.toggle(singletonList(child(model, ROOT, Employee.MGR)));
		columns.showHidden().set(false);
		assertTrue(displayed(model, ROOT, Employee.MGR));
		assertFalse(displayed(model, ROOT, Employee.DEPARTMENT));
		assertEquals(managerChildren, model.nodes().children(MANAGER).size());

		//and hidden once removed
		columns.remove(singletonList(path(Employee.MGR)));
		assertFalse(displayed(model, ROOT, Employee.MGR));
		//and shown once added
		columns.toggle(singletonList(path(Employee.DEPARTMENT)));
		assertTrue(displayed(model, ROOT, Employee.DEPARTMENT));
	}

	@Test
	void search() {
		EntityTableExportColumns columns = exportColumns();
		SwingFilterTreeModel<AttributeDefinition<?>> model = columns.tree();
		model.expansion().expand(MANAGER);
		int visible = model.visible().size();

		// the matching attributes, along with their ancestors
		columns.search().set("ENAME");
		assertEquals(asList(NAME, MANAGER, MANAGER_NAME), visibleNodes(model));

		// along with the descendants of a matching foreign key
		columns.search().set("mgr_");
		List<NodePath<AttributeDefinition<?>>> managerChildren = model.nodes().children(MANAGER);
		assertFalse(managerChildren.isEmpty());
		assertEquals(managerChildren.size() + 1, visibleNodes(model).size());

		columns.search().set(" ");
		assertEquals(visible, model.visible().size());
	}

	@Test
	void configuration() {
		EntityTableExportColumns columns = exportColumns();
		columns.clear();
		columns.toggle(asList(JOB, NAME, MANAGER, MANAGER_NAME));
		assertEquals("job\tename\tmgr_fk\tmgr_fk ename", header(columns));

		JSONObject json = columns.toJson();
		assertTrue(new JSONObject("{\"entityType\":\"employees.employee\",\"columns\":[" +
						"{\"attributes\":[\"job\"]},{\"attributes\":[\"ename\"]},{\"attributes\":[\"mgr_fk\"]}," +
						"{\"attributes\":[\"mgr_fk\",\"ename\"]}]}").similar(json), json.toString());

		EntityTableExportColumns applied = exportColumns();
		applied.apply(json);
		assertEquals(columns.list().items().get(), applied.list().items().get());

		// the columns which no longer exist, or are named twice, skipped, any other fields ignored
		applied.apply(new JSONObject("{\"columns\":[{\"attributes\":[\"job\"],\"caption\":\"Job\"},{\"attributes\":[\"none\"]}," +
						"{\"attributes\":[\"job\",\"ename\"]},{\"attributes\":[\"mgr_fk\",\"none\"]},{\"attributes\":[\"ename\"]}," +
						"{\"attributes\":[\"job\"]}]}"));
		assertEquals(asList(JOB, NAME), applied.list().items().get());

		// a column not an object naming the attributes on its path
		assertThrows(JSONException.class, () -> applied.apply(new JSONObject("{\"columns\":[[\"job\"]]}")));
		assertThrows(JSONException.class, () -> applied.apply(new JSONObject("{\"columns\":[{\"caption\":\"Job\"}]}")));
	}

	@Test
	void firstConfigurationFormat() {
		EntityTableExportColumns columns = exportColumns();
		// as written by the first format, a foreign key preceding the attributes below it
		columns.apply(new JSONObject("{\"entityType\":\"employees.employee\",\"showHidden\":false," +
						"\"attributes\":[\"job\",\"ename\",\"mgr_fk\",{\"mgr_fk\":{\"attributes\":[\"ename\"]}}]}"));
		assertEquals(asList(JOB, NAME, MANAGER, MANAGER_NAME), columns.list().items().get());
		assertEquals("job\tename\tmgr_fk\tmgr_fk ename", header(columns));

		// in the order of first mention, an attribute no longer there skipped
		columns.apply(new JSONObject("{\"attributes\":[\"job\",{\"mgr_fk\":{\"attributes\":[\"ename\",\"none\"]}}," +
						"\"none\",\"mgr_fk\",\"ename\"]}"));
		assertEquals(asList(JOB, MANAGER, MANAGER_NAME, NAME), columns.list().items().get());
	}

	@Test
	void modified() throws Exception {
		EntityTableExportModel model = tablePanel().exportPanel().model();
		EntityTableExportColumns columns = model.columns();
		// the default columns, no configuration selected
		assertFalse(model.modified().is());
		columns.toggle(singletonList(MANAGER_NAME));
		assertTrue(model.modified().is());
		columns.toggle(singletonList(MANAGER_NAME));
		assertFalse(model.modified().is());

		Path file = Files.createTempFile("export", ".json");
		try {
			columns.clear();
			columns.toggle(asList(JOB, NAME));
			model.writeConfig(file.toFile());
			assertFalse(model.modified().is());
			assertEquals(file.toFile(), model.configurationFiles().selection().item().getOrThrow().file());

			columns.move(singletonList(NAME), true);
			assertTrue(model.modified().is());
			// moved back, as saved
			columns.move(singletonList(NAME), false);
			assertFalse(model.modified().is());
			// the table columns, the configuration still selected
			columns.reset();
			assertTrue(model.modified().is());

			// selecting no configuration, the default columns
			model.configurationFiles().selection().clear();
			assertFalse(model.modified().is());
			// and the configuration again, as saved
			model.configurationFiles().selection().item().set(model.configurationFiles().items().get().stream()
							.filter(configurationFile -> configurationFile != EntityTableExportModel.NULL_CONFIGURATION_FILE)
							.findFirst()
							.orElseThrow(AssertionError::new));
			assertEquals(asList(JOB, NAME), columns.list().items().get());
			assertFalse(model.modified().is());

			// removed from the list, the file left as is
			model.removeConfigurationFile();
			assertTrue(model.configurationFiles().selection().item().isNull());
			assertTrue(Files.exists(file));
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void wrongEntityType() throws Exception {
		EntityTableExportModel model = tablePanel().exportPanel().model();
		Path file = Files.createTempFile("export", ".json");
		try {
			Files.write(file, "{\"entityType\":\"employees.department\",\"columns\":[]}".getBytes(UTF_8));
			IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
							() -> model.addConfigurationFiles(singletonList(file.toFile())));
			assertTrue(exception.getMessage().contains(model.entityDefinition().caption()), exception.getMessage());
			assertTrue(exception.getMessage().contains(file.toFile().getAbsolutePath()), exception.getMessage());
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void rowCounts() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresher().async().set(false);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportModel model = tablePanel.exportModel();
		AtomicInteger notified = new AtomicInteger();
		model.rowCounts().addListener(notified::incrementAndGet);

		tableModel.items().refresh();
		assertTrue(notified.get() > 0);
		assertEquals(tableModel.items().included().size(), model.allCount());
		assertEquals(0, model.selectedCount());

		notified.set(0);
		tableModel.selection().indexes().set(asList(0, 1));
		assertTrue(notified.get() > 0);
		assertEquals(2, model.selectedCount());
		// the selected rows exported, following the selection
		assertTrue(model.selected().is());
	}

	@Test
	void exportTasks() throws Exception {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresh();
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportModel exportModel = tablePanel.exportModel();
		ProgressReporter<Void> progress = new ProgressReporter<Void>() {
			@Override
			public void report(int progress) {}

			@Override
			public void publish(Void... chunks) {}
		};

		String text = exportModel.exportToString().execute(progress);
		assertTrue(text.contains(tableModel.items().included().get(0).get(Employee.NAME)));

		Path file = Files.createTempFile("export", ".tsv");
		try {
			assertEquals(file, exportModel.exportToFile(file).execute(progress));
			assertEquals(text, new String(Files.readAllBytes(file), UTF_8));
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	private static String header(EntityTableExportColumns columns) {
		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(columns::attributes)
						.entities(CONNECTION.select(all(Employee.TYPE).build()).iterator())
						.output(output::append)
						.export();

		return output.substring(0, output.indexOf("\n"));
	}

	private static EntityTablePanel tablePanel() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);

		return new EntityTablePanel(tableModel, config -> config.includeExport(true));
	}

	private static EntityTableExportColumns exportColumns() {
		return tablePanel().exportPanel().model().columns();
	}

	private static List<Attribute<?>> topLevelAttributes(EntityTableExportColumns columns) {
		return columns.list().items().get().stream()
						.map(path -> path.item().attribute())
						.collect(toList());
	}

	private static List<NodePath<AttributeDefinition<?>>> visibleNodes(SwingFilterTreeModel<AttributeDefinition<?>> model) {
		return model.visible().get().stream()
						.filter(path -> !path.root())
						.collect(toList());
	}

	/**
	 * @return the path of the given attribute, preceded by the foreign keys leading to it
	 */
	private static NodePath<AttributeDefinition<?>> path(Attribute<?>... attributes) {
		NodePath<AttributeDefinition<?>> path = nodePath();
		for (Attribute<?> attribute : attributes) {
			path = path.child(CONNECTION.entities().definition(attribute.entityType()).attributes().definition(attribute));
		}

		return path;
	}

	private static NodePath<AttributeDefinition<?>> child(SwingFilterTreeModel<AttributeDefinition<?>> model,
																												NodePath<AttributeDefinition<?>> parent, Attribute<?> attribute) {
		return model.nodes().children(parent).stream()
						.filter(path -> path.item().attribute().equals(attribute))
						.findFirst()
						.orElseThrow(() -> new AssertionError(attribute + " not found below " + parent));
	}

	private static boolean displayed(SwingFilterTreeModel<AttributeDefinition<?>> model,
																	 NodePath<AttributeDefinition<?>> parent, Attribute<?> attribute) {
		return model.nodes().children(parent).stream()
						.anyMatch(path -> path.item().attribute().equals(attribute));
	}
}
