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

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static org.junit.jupiter.api.Assertions.*;

public final class PreferencesMigratorTest {

	@Test
	void migrateV1ToV2() throws Exception {
		Preferences root = jsonPreferences();
		// application level
		root.put("applicationPanel", "{\"scaling\":100}");
		// an entity panel with table (conditions/filters model-owned, settings/columns/export view-owned), edit and detail
		Preferences employee = root.node("entityPanels").node("employees.employee");
		Preferences table = employee.node("tablePanel");
		table.put("conditions", "{\"ename\":{\"cs\":1}}");
		table.put("filters", "{\"job\":{\"ae\":0}}");
		table.put("table", "{\"auto-resize-mode\":4}");
		table.put("columns", "{\"ename\":{\"w\":100,\"i\":0}}");
		table.put("export", "{\"format\":\"csv\"}");
		employee.node("editPanel").put("custom", "value");
		// a panel-level key from an app's EntityPanel.store() override
		employee.put("panelCustom", "panelValue");
		employee.node("detailPanels").node("employees.department").node("tablePanel").put("conditions", "{\"loc\":{\"cs\":1}}");
		// auxiliary panel
		root.node("auxiliaryPanels").node("employees.lookup").node("tablePanel").put("columns", "{\"x\":{\"w\":50}}");

		PreferencesMigrator.migrate(root);

		// version stamped, v1 nodes/keys removed
		assertEquals("2", root.get("version", null));
		assertNull(root.get("applicationPanel", null));
		assertFalse(root.nodeExists("entityPanels"));
		assertFalse(root.nodeExists("auxiliaryPanels"));

		// application relocated
		assertTrue(root.get("application", "").contains("scaling"));

		// entity: model-owned vs view-owned split
		Preferences employeeV2 = root.node("entities").node("employees.employee");
		assertTrue(employeeV2.node("model").get("conditions", "").contains("cs"));
		assertTrue(employeeV2.node("model").get("filters", "").contains("ae"));
		Preferences viewTable = employeeV2.node("view").node("table");
		assertTrue(viewTable.get("settings", "").contains("auto-resize-mode")); // renamed from "table"
		assertTrue(viewTable.get("columns", "").contains("\"w\""));
		assertTrue(viewTable.get("export", "").contains("csv"));
		assertEquals("value", employeeV2.node("view").node("edit").get("custom", null));
		// panel-level override keys stay at the entity node, where a v2 EntityPanel.restore() override reads
		assertEquals("panelValue", employeeV2.get("panelCustom", null));

		// detail recursed
		assertTrue(employeeV2.node("details").node("employees.department")
						.node("model").get("conditions", "").contains("loc"));

		// auxiliary migrated with the same transform
		assertTrue(root.node("auxiliary").node("employees.lookup")
						.node("view").node("table").get("columns", "").contains("\"w\""));

		// idempotent - second run is a no-op
		PreferencesMigrator.migrate(root);
		assertEquals("2", root.get("version", null));
		assertTrue(root.nodeExists("entities"));
		assertFalse(root.nodeExists("entityPanels"));
	}

	@Test
	void freshRootNoOp() throws Exception {
		// A fresh/empty root has nothing to migrate, the version marker is written by the next store, not here,
		// so an app the user never customizes still gets no preferences file
		Preferences root = jsonPreferences();
		PreferencesMigrator.migrate(root);
		assertNull(root.get("version", null));
		assertFalse(root.nodeExists("entities"));
	}

	@Test
	void alreadyV2Untouched() {
		Preferences root = jsonPreferences();
		root.put("version", "2");
		root.node("entities").node("x").node("model").put("conditions", "{\"a\":1}");

		PreferencesMigrator.migrate(root);

		assertEquals("2", root.get("version", null));
		assertTrue(root.node("entities").node("x").node("model").get("conditions", "").contains("\"a\""));
	}
}
