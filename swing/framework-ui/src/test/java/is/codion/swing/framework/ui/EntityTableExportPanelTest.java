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

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

	@Test
	void exportPreferencesDefaults() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));
		EntityTableExportPanel exportPanel = tablePanel.exportPanel();

		exportPanel.model().treeModel().includeNone();
		exportPanel.model().treeModel().includeAll();

		// Store preferences - nothing to store for export since it matches defaults
		Preferences prefs = jsonPreferences();
		EntityTablePanelPreferences.store(prefs, tablePanel);

		// Nothing is stored at all, an empty export configuration is not a preference
		assertEquals("", prefs.get("export", ""));
	}

	@Test
	void cyclicalForeignKeyExpansion() {
		SwingFilterTreeModel<AttributeDefinition<?>> model = exportTreeModel().treeModel();

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
		SwingFilterTreeModel<AttributeDefinition<?>> model = exportTreeModel().treeModel();
		NodePath<AttributeDefinition<?>> name = child(model, ROOT, Employee.NAME);
		//not a foreign key, so a leaf, which is not loaded, neither when expanded nor when refreshed
		model.expansion().expand(name);
		model.nodes().refresh(name);
		assertFalse(model.nodes().loaded(name));
	}

	@Test
	void moveOntoOwnPositionDoesNotThrow() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		List<NodePath<AttributeDefinition<?>>> before = treeModel.treeModel().nodes().children(ROOT);
		assertTrue(before.size() > 3);
		//dropping a node exactly where it already sits (drop index == its own index) must not throw
		treeModel.move(singletonList(before.get(2)), 2);
		assertEquals(before, treeModel.treeModel().nodes().children(ROOT));
	}

	@Test
	void move() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		SwingFilterTreeModel<AttributeDefinition<?>> model = treeModel.treeModel();
		NodePath<AttributeDefinition<?>> manager = child(model, ROOT, Employee.MGR_FK);
		model.expansion().expand(manager);
		List<NodePath<AttributeDefinition<?>>> before = model.nodes().children(ROOT);
		List<NodePath<AttributeDefinition<?>>> moved = asList(before.get(1), before.get(2));
		model.selection().items().set(moved);

		treeModel.move(moved, true);
		assertEquals(asList(before.get(1), before.get(2), before.get(0)), model.nodes().children(ROOT).subList(0, 3));
		//selection and expansion kept, paths identifying nodes rather than positions
		assertEquals(moved, model.selection().items().get());
		assertTrue(model.expansion().expanded(manager));
		assertTrue(model.nodes().loaded(manager));
		//already first
		treeModel.move(moved, true);
		assertEquals(moved, model.nodes().children(ROOT).subList(0, 2));

		treeModel.move(moved, false);
		assertEquals(before.subList(0, 3), model.nodes().children(ROOT).subList(0, 3));
		assertEquals(moved, model.selection().items().get());

		//drag and drop, after the last
		treeModel.move(moved, before.size());
		assertEquals(moved, model.nodes().children(ROOT).subList(before.size() - 2, before.size()));
		assertEquals(moved, model.selection().items().get());
	}

	@Test
	void showHidden() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		SwingFilterTreeModel<AttributeDefinition<?>> model = treeModel.treeModel();
		NodePath<AttributeDefinition<?>> manager = child(model, ROOT, Employee.MGR_FK);
		model.expansion().expand(manager);
		int managerChildren = model.nodes().children(manager).size();
		assertFalse(displayed(model, ROOT, Employee.MGR));

		treeModel.showHidden().set(true);
		assertTrue(displayed(model, ROOT, Employee.MGR));
		assertTrue(displayed(model, ROOT, Employee.DEPARTMENT));
		//expansion kept, the children of the expanded node now including its hidden attributes
		assertTrue(model.expansion().expanded(manager));
		assertTrue(model.nodes().children(manager).size() > managerChildren);

		//an included hidden attribute stays displayed
		treeModel.toggle(singletonList(child(model, ROOT, Employee.MGR)));
		treeModel.showHidden().set(false);
		assertTrue(displayed(model, ROOT, Employee.MGR));
		assertFalse(displayed(model, ROOT, Employee.DEPARTMENT));
		assertEquals(managerChildren, model.nodes().children(manager).size());
	}

	@Test
	void hideExcluded() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		SwingFilterTreeModel<AttributeDefinition<?>> model = treeModel.treeModel();
		treeModel.includeNone();
		NodePath<AttributeDefinition<?>> manager = child(model, ROOT, Employee.MGR_FK);
		model.expansion().expand(manager);
		NodePath<AttributeDefinition<?>> managerName = child(model, manager, Employee.NAME);
		treeModel.toggle(singletonList(managerName));
		assertTrue(treeModel.included(managerName));
		assertEquals(1, treeModel.includedCount(manager));
		assertEquals(1, treeModel.includedCount(ROOT));

		treeModel.hideExcluded();
		assertEquals(asList(manager, managerName), model.visible().get());

		treeModel.showExcluded();
		assertTrue(model.visible().size() > 2);
		//excluding hides it again, the predicate reading the configuration
		treeModel.hideExcluded();
		treeModel.toggle(singletonList(managerName));
		assertEquals(0, model.visible().size());
	}

	@Test
	void includeNoneShowsExcluded() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		SwingFilterTreeModel<AttributeDefinition<?>> model = treeModel.treeModel();
		String header = header(treeModel);
		int attributes = model.visible().size();
		treeModel.includeNone();
		treeModel.toggle(asList(child(model, ROOT, Employee.NAME), child(model, ROOT, Employee.JOB)));
		//hides the excluded ones
		treeModel.applyConfiguration(treeModel.toJson());
		assertEquals(2, model.visible().size());
		//the configuration deselected, see EntityTableExportModel.configurationFileSelected()
		treeModel.showHidden().set(false);
		treeModel.includeNone();
		assertEquals(attributes, model.visible().size());
		treeModel.includeAll();
		assertEquals(attributes, model.visible().size());
		assertEquals(header, header(treeModel));
	}

	@Test
	void configuration() {
		EntityTableExportTreeModel treeModel = exportTreeModel();
		SwingFilterTreeModel<AttributeDefinition<?>> model = treeModel.treeModel();
		treeModel.includeNone();
		NodePath<AttributeDefinition<?>> job = child(model, ROOT, Employee.JOB);
		NodePath<AttributeDefinition<?>> manager = child(model, ROOT, Employee.MGR_FK);
		treeModel.toggle(asList(child(model, ROOT, Employee.NAME), job, manager));
		model.expansion().expand(manager);
		treeModel.toggle(singletonList(child(model, manager, Employee.NAME)));
		treeModel.move(singletonList(job), 0);

		String header = header(treeModel);
		assertTrue(header.startsWith("job\tename\tmgr_fk\tmgr_fk ename"), header);

		JSONObject json = treeModel.toJson();
		EntityTableExportTreeModel applied = new EntityTableExportTreeModel(Employee.TYPE, CONNECTION.entities());
		applied.applyConfiguration(json);
		assertTrue(json.similar(applied.toJson()), json + " / " + applied.toJson());
		assertEquals(header, header(applied));
		//the excluded attributes hidden
		applied.treeModel().expansion().set(applied.includedParents());
		assertEquals(asList(job, child(model, ROOT, Employee.NAME), manager, child(model, manager, Employee.NAME)),
						applied.treeModel().visible().get().stream()
										.filter(path -> !path.root())
										.collect(toList()).subList(0, 4));
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

	private static String header(EntityTableExportTreeModel treeModel) {
		StringBuilder output = new StringBuilder();
		EntityExport.builder(CONNECTION)
						.entityType(Employee.TYPE)
						.attributes(treeModel::attributes)
						.entities(CONNECTION.select(all(Employee.TYPE).build()).iterator())
						.output(output::append)
						.export();

		return output.substring(0, output.indexOf("\n"));
	}

	private static EntityTableExportTreeModel exportTreeModel() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeExport(true));

		return tablePanel.exportPanel().model().treeModel();
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
