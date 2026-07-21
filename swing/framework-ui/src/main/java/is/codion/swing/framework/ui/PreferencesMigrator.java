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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.preferences.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;

/**
 * Migrates a preferences root from the v1 layout ({@code applicationPanel}/{@code entityPanels}/{@code tablePanel}...)
 * to the v2 layout ({@code application}/{@code entities}/{@code <key>}/{@code model}+{@code view}...), preserving the
 * user's stored preferences across the layout change. Runs once, before the model and UI walks read the root:
 * <ul>
 * <li>A {@code version} marker present at the root indicates v2, nothing to do.
 * <li>No {@code version} but {@code applicationPanel}/{@code entityPanels}/{@code auxiliaryPanels} present indicates
 * v1, migrate (after backing up the file, if file based).
 * <li>Neither indicates a fresh or stray root, stamp {@code version} and move on.
 * </ul>
 * Best-effort throughout: a preferences hiccup must never cost the user the application, so any error is logged and
 * swallowed. The rebuilt tree is written through the store's atomic file write on {@link Preferences#flush()}.
 * <p>
 * This is a transition aid; the {@code version} marker makes its eventual removal clean and unambiguous.
 */
final class PreferencesMigrator {

	private static final Logger LOG = LoggerFactory.getLogger(PreferencesMigrator.class);

	private static final String VERSION = "version";
	private static final String VERSION_VALUE = "2";
	private static final String BACKUP_SUFFIX = "v1";

	// v1 vocabulary
	private static final String V1_APPLICATION = "applicationPanel";
	private static final String V1_ENTITY_PANELS = "entityPanels";
	private static final String V1_AUXILIARY_PANELS = "auxiliaryPanels";
	private static final String V1_EDIT_PANEL = "editPanel";
	private static final String V1_TABLE_PANEL = "tablePanel";
	private static final String V1_DETAIL_PANELS = "detailPanels";
	private static final String V1_TABLE_SETTINGS = "table"; // the auto-resize key under tablePanel

	// v2 vocabulary
	private static final String APPLICATION = "application";
	private static final String ENTITIES = "entities";
	private static final String AUXILIARY = "auxiliary";
	private static final String MODEL = "model";
	private static final String VIEW = "view";
	private static final String TABLE = "table";
	private static final String EDIT = "edit";
	private static final String DETAILS = "details";
	private static final String SETTINGS = "settings";

	// model-owned keys (moved from view to model), all other tablePanel keys are view state
	private static final String CONDITIONS = "conditions";
	private static final String FILTERS = "filters";

	private PreferencesMigrator() {}

	static void migrate(Preferences root) {
		requireNonNull(root);
		try {
			if (root.get(VERSION, null) != null) {
				return; // already v2
			}
			if (!legacy(root)) {
				return; // fresh or stray, nothing to migrate (the version marker is written on the next store)
			}
			LOG.info("Migrating preferences to version {}", VERSION_VALUE);
			backup(root);
			migrateApplication(root);
			migratePanels(root, V1_ENTITY_PANELS, ENTITIES);
			migratePanels(root, V1_AUXILIARY_PANELS, AUXILIARY);
			root.put(VERSION, VERSION_VALUE);
			root.flush();
			LOG.info("Preferences migrated to version {}", VERSION_VALUE);
		}
		catch (Exception e) {
			// A prefs hiccup must never cost the user the application
			LOG.error("Error migrating preferences to version {}, continuing", VERSION_VALUE, e);
		}
	}

	private static boolean legacy(Preferences root) throws BackingStoreException {
		return root.get(V1_APPLICATION, null) != null
						|| root.nodeExists(V1_ENTITY_PANELS)
						|| root.nodeExists(V1_AUXILIARY_PANELS);
	}

	private static void backup(Preferences root) {
		if (root instanceof FilePreferences) {
			((FilePreferences) root).backup(BACKUP_SUFFIX + "." + currentTimeMillis());
		}
	}

	private static void migrateApplication(Preferences root) {
		String application = root.get(V1_APPLICATION, null);
		if (application != null) {
			root.put(APPLICATION, application);
			root.remove(V1_APPLICATION);
		}
	}

	private static void migratePanels(Preferences root, String v1Parent, String v2Parent) throws BackingStoreException {
		if (root.nodeExists(v1Parent)) {
			Preferences v1 = root.node(v1Parent);
			String[] children = v1.childrenNames();
			if (children.length > 0) {
				Preferences v2 = root.node(v2Parent);
				for (String key : children) {
					migratePanel(v1.node(key), v2.node(key));
				}
			}
			v1.removeNode();
		}
	}

	private static void migratePanel(Preferences v1Panel, Preferences v2Entity) throws BackingStoreException {
		migrateTablePanel(v1Panel, v2Entity);
		migrateEditPanel(v1Panel, v2Entity);
		migrateDetailPanels(v1Panel, v2Entity);
		migratePanelKeys(v1Panel, v2Entity);
	}

	private static void migrateTablePanel(Preferences v1Panel, Preferences v2Entity) throws BackingStoreException {
		if (v1Panel.nodeExists(V1_TABLE_PANEL)) {
			Preferences tablePanel = v1Panel.node(V1_TABLE_PANEL);
			Preferences model = v2Entity.node(MODEL);
			Preferences viewTable = v2Entity.node(VIEW).node(TABLE);
			for (String key : tablePanel.keys()) {
				String value = tablePanel.get(key, "");
				if (CONDITIONS.equals(key) || FILTERS.equals(key)) {
					model.put(key, value); // model-owned state
				}
				else if (V1_TABLE_SETTINGS.equals(key)) {
					viewTable.put(SETTINGS, value); // auto-resize, renamed key
				}
				else {
					viewTable.put(key, value); // columns, export and any other view state
				}
			}
		}
	}

	private static void migrateEditPanel(Preferences v1Panel, Preferences v2Entity) throws BackingStoreException {
		if (v1Panel.nodeExists(V1_EDIT_PANEL)) {
			copy(v1Panel.node(V1_EDIT_PANEL), v2Entity.node(VIEW).node(EDIT));
		}
	}

	private static void migrateDetailPanels(Preferences v1Panel, Preferences v2Entity) throws BackingStoreException {
		if (v1Panel.nodeExists(V1_DETAIL_PANELS)) {
			Preferences v1Details = v1Panel.node(V1_DETAIL_PANELS);
			Preferences v2Details = v2Entity.node(DETAILS);
			for (String childKey : v1Details.childrenNames()) {
				migratePanel(v1Details.node(childKey), v2Details.node(childKey));
			}
		}
	}

	// Any value keys directly under the panel node came from an app's EntityPanel.store() override, default them to view/
	private static void migratePanelKeys(Preferences v1Panel, Preferences v2Entity) throws BackingStoreException {
		String[] keys = v1Panel.keys();
		if (keys.length > 0) {
			Preferences view = v2Entity.node(VIEW);
			for (String key : keys) {
				view.put(key, v1Panel.get(key, ""));
			}
		}
	}

	private static void copy(Preferences source, Preferences target) throws BackingStoreException {
		for (String key : source.keys()) {
			target.put(key, source.get(key, ""));
		}
		for (String child : source.childrenNames()) {
			copy(source.node(child), target.node(child));
		}
	}
}
