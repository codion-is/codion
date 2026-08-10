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

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Detail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import static is.codion.common.model.preferences.JsonPreferences.jsonPreferences;
import static org.junit.jupiter.api.Assertions.*;

public class EntityTablePanelPreferencesTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private Preferences preferences;
	private List<Entity> testEntities;

	@BeforeEach
	void setUp() {
		testEntities = initTestEntities(CONNECTION.entities());
		preferences = jsonPreferences();
	}

	@Test
	void columnVisibilityAndOrder() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		// Verify initial state
		assertTrue(columns.visible(Detail.STRING).is());
		assertTrue(columns.visible(Detail.INT).is());

		// Modify column visibility and order
		columns.visible(Detail.STRING).set(false);
		columns.moveColumn(columns.indexOf(Detail.DOUBLE), 0);

		// Save and restore
		tablePanel.store(preferences);
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.restore(preferences);

		// Verify restored state
		columns = tablePanel.table().columns();
		assertFalse(columns.visible(Detail.STRING).is());
		assertEquals(0, columns.indexOf(Detail.DOUBLE));
	}

	@Test
	void columnWidth() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		// Modify column widths
		TableColumn intColumn = columns.get(Detail.INT);
		TableColumn doubleColumn = columns.get(Detail.DOUBLE);
		intColumn.setWidth(150);
		intColumn.setPreferredWidth(150);
		doubleColumn.setWidth(200);
		doubleColumn.setPreferredWidth(200);

		// Save and restore
		tablePanel.store(preferences);
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.restore(preferences);

		// Verify restored widths
		columns = tablePanel.table().columns();
		assertEquals(150, columns.get(Detail.INT).getPreferredWidth());
		assertEquals(200, columns.get(Detail.DOUBLE).getPreferredWidth());
	}

	// Condition, filter and sort persistence is model-owned and covered by AbstractEntityTableModelTest.preferences()

	@Test
	void autoResizeMode() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);

		// Get initial value for comparison
		int initialMode = tablePanel.table().getAutoResizeMode();

		// Set a different auto-resize mode
		int newMode = initialMode == JTable.AUTO_RESIZE_LAST_COLUMN
						? JTable.AUTO_RESIZE_ALL_COLUMNS
						: JTable.AUTO_RESIZE_LAST_COLUMN;
		tablePanel.table().setAutoResizeMode(newMode);

		// Save and restore
		tablePanel.store(preferences);
		tablePanel = new EntityTablePanel(tableModel);

		// Verify it's back to default before applying
		assertEquals(initialMode, tablePanel.table().getAutoResizeMode());

		tablePanel.restore(preferences);

		// Verify restored auto-resize mode
		assertEquals(newMode, tablePanel.table().getAutoResizeMode());
	}

	@Test
	void emptyPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		// Remember initial state
		boolean stringVisible = columns.visible(Detail.STRING).is();
		int intWidth = columns.get(Detail.INT).getPreferredWidth();

		// Apply empty preferences (nothing saved yet)
		tablePanel.restore(preferences);

		// Verify defaults are unchanged
		assertEquals(stringVisible, columns.visible(Detail.STRING).is());
		assertEquals(intWidth, columns.get(Detail.INT).getPreferredWidth());
	}

	@Test
	void preferencesForMissingColumn() {
		// Save preferences with current columns
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		columns.get(Detail.INT).setWidth(175);
		tablePanel.store(preferences);

		// Manually inject a preference for a non-existent column
		String columnsKey = "columns";
		String currentJson = preferences.get(columnsKey, "{}");
		String modifiedJson = currentJson.replace("}", ",\"nonexistent\":{\"w\":100,\"i\":0}}");
		preferences.put(columnsKey, modifiedJson);

		// Apply preferences - should not throw, non-existent column preference is ignored
		EntityTablePanel newTablePanel = new EntityTablePanel(tableModel);
		assertDoesNotThrow(() -> newTablePanel.restore(preferences));

		// Verify valid preferences were still applied
		columns = newTablePanel.table().columns();
		assertEquals(175, columns.get(Detail.INT).getPreferredWidth());
	}

	@Test
	void newColumnNotHiddenByOldPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		// Hide STRING column and save preferences
		columns.visible(Detail.STRING).set(false);
		tablePanel.store(preferences);

		// Remove STRING from saved preferences to simulate a "new" column
		// that wasn't in the preferences when they were saved
		String columnsKey = "columns";
		String currentJson = preferences.get(columnsKey, "{}");
		// Remove string entry from JSON
		String modifiedJson = currentJson.replaceAll(",?\"string\":\\{[^}]+\\}", "");
		// Clean up any leading comma after removal
		modifiedJson = modifiedJson.replace("{,", "{");
		preferences.put(columnsKey, modifiedJson);

		// Apply preferences - STRING should be visible since it's not in preferences
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.restore(preferences);

		columns = tablePanel.table().columns();
		// New columns (not in preferences) should remain visible
		assertTrue(columns.visible(Detail.STRING).is());
	}

	@Test
	void roundTrip() {
		// Comprehensive round-trip test
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columns = tablePanel.table().columns();

		// Configure everything
		columns.visible(Detail.STRING).set(false);
		columns.moveColumn(columns.indexOf(Detail.DOUBLE), 0);
		columns.get(Detail.INT).setWidth(155);
		columns.get(Detail.INT).setPreferredWidth(155);
		tablePanel.table().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// Save
		tablePanel.store(preferences);

		// Create fresh panel and restore
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.restore(preferences);

		// Verify everything
		columns = tablePanel.table().columns();
		assertFalse(columns.visible(Detail.STRING).is());
		assertEquals(0, columns.indexOf(Detail.DOUBLE));
		assertEquals(155, columns.get(Detail.INT).getPreferredWidth());
		assertEquals(JTable.AUTO_RESIZE_OFF, tablePanel.table().getAutoResizeMode());
	}

	private static List<Entity> initTestEntities(Entities entities) {
		List<Entity> testEntities = new ArrayList<>(5);
		String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
		for (int i = 0; i < 5; i++) {
			testEntities.add(entities.entity(Detail.TYPE)
							.with(Detail.ID, (long) i + 1)
							.with(Detail.INT, i + 1)
							.with(Detail.STRING, stringValues[i])
							.build());
		}

		return testEntities;
	}
}
