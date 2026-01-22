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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.ConditionModel.Wildcard;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Detail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTablePanelPreferencesTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private static final String PREFERENCES_KEY = EntityTablePanelPreferencesTest.class.getName();

	private Preferences preferences;
	private List<Entity> testEntities;

	@BeforeEach
	void setUp() {
		testEntities = initTestEntities(CONNECTION_PROVIDER.entities());
		preferences = UserPreferences.file(PREFERENCES_KEY);
	}

	@AfterEach
	void tearDown() throws BackingStoreException {
		preferences.clear();
		UserPreferences.flush();
	}

	@Test
	void columnVisibilityAndOrder() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		// Verify initial state
		assertTrue(columnModel.visible(Detail.STRING).is());
		assertTrue(columnModel.visible(Detail.INT).is());

		// Modify column visibility and order
		columnModel.visible(Detail.STRING).set(false);
		columnModel.moveColumn(columnModel.getColumnIndex(Detail.DOUBLE), 0);

		// Save and restore
		tablePanel.writePreferences(preferences);
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.applyPreferences(preferences);

		// Verify restored state
		columnModel = tablePanel.table().columnModel();
		assertFalse(columnModel.visible(Detail.STRING).is());
		assertEquals(0, columnModel.getColumnIndex(Detail.DOUBLE));
	}

	@Test
	void columnWidth() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		// Modify column widths
		TableColumn intColumn = columnModel.column(Detail.INT);
		TableColumn doubleColumn = columnModel.column(Detail.DOUBLE);
		intColumn.setWidth(150);
		intColumn.setPreferredWidth(150);
		doubleColumn.setWidth(200);
		doubleColumn.setPreferredWidth(200);

		// Save and restore
		tablePanel.writePreferences(preferences);
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.applyPreferences(preferences);

		// Verify restored widths
		columnModel = tablePanel.table().columnModel();
		assertEquals(150, columnModel.column(Detail.INT).getPreferredWidth());
		assertEquals(200, columnModel.column(Detail.DOUBLE).getPreferredWidth());
	}

	@Test
	void conditionPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);

		// Modify condition preferences
		ConditionModel<?> stringCondition = tableModel.query().condition().get().get(Detail.STRING);
		stringCondition.autoEnable().set(false);
		stringCondition.caseSensitive().set(true);
		stringCondition.operands().wildcard().set(Wildcard.PREFIX);

		// Save and restore
		tablePanel.writePreferences(preferences);
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.applyPreferences(preferences);

		// Verify restored condition preferences
		stringCondition = tableModel.query().condition().get().get(Detail.STRING);
		assertFalse(stringCondition.autoEnable().is());
		assertTrue(stringCondition.caseSensitive().is());
		assertEquals(Wildcard.PREFIX, stringCondition.operands().wildcard().get());
	}

	@Test
	void autoResizeMode() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);

		// Get initial value for comparison
		int initialMode = tablePanel.table().getAutoResizeMode();

		// Set a different auto-resize mode
		int newMode = initialMode == JTable.AUTO_RESIZE_LAST_COLUMN
						? JTable.AUTO_RESIZE_ALL_COLUMNS
						: JTable.AUTO_RESIZE_LAST_COLUMN;
		tablePanel.table().setAutoResizeMode(newMode);

		// Save and restore
		tablePanel.writePreferences(preferences);
		tablePanel = new EntityTablePanel(tableModel);

		// Verify it's back to default before applying
		assertEquals(initialMode, tablePanel.table().getAutoResizeMode());

		tablePanel.applyPreferences(preferences);

		// Verify restored auto-resize mode
		assertEquals(newMode, tablePanel.table().getAutoResizeMode());
	}

	@Test
	void emptyPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		// Remember initial state
		boolean stringVisible = columnModel.visible(Detail.STRING).is();
		int intWidth = columnModel.column(Detail.INT).getPreferredWidth();

		// Apply empty preferences (nothing saved yet)
		tablePanel.applyPreferences(preferences);

		// Verify defaults are unchanged
		assertEquals(stringVisible, columnModel.visible(Detail.STRING).is());
		assertEquals(intWidth, columnModel.column(Detail.INT).getPreferredWidth());
	}

	@Test
	void preferencesForMissingColumn() {
		// Save preferences with current columns
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		columnModel.column(Detail.INT).setWidth(175);
		tablePanel.writePreferences(preferences);

		// Manually inject a preference for a non-existent column
		String columnsKey = tablePanel.preferencesKey() + "-columns";
		String currentJson = preferences.get(columnsKey, "{}");
		String modifiedJson = currentJson.replace("}", ",\"nonexistent\":{\"w\":100,\"i\":0}}");
		preferences.put(columnsKey, modifiedJson);

		// Apply preferences - should not throw, non-existent column preference is ignored
		EntityTablePanel newTablePanel = new EntityTablePanel(tableModel);
		assertDoesNotThrow(() -> newTablePanel.applyPreferences(preferences));

		// Verify valid preferences were still applied
		columnModel = newTablePanel.table().columnModel();
		assertEquals(175, columnModel.column(Detail.INT).getPreferredWidth());
	}

	@Test
	void newColumnNotHiddenByOldPreferences() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		// Hide STRING column and save preferences
		columnModel.visible(Detail.STRING).set(false);
		tablePanel.writePreferences(preferences);

		// Remove STRING from saved preferences to simulate a "new" column
		// that wasn't in the preferences when they were saved
		String columnsKey = tablePanel.preferencesKey() + "-columns";
		String currentJson = preferences.get(columnsKey, "{}");
		// Remove string entry from JSON
		String modifiedJson = currentJson.replaceAll(",?\"string\":\\{[^}]+\\}", "");
		// Clean up any leading comma after removal
		modifiedJson = modifiedJson.replace("{,", "{");
		preferences.put(columnsKey, modifiedJson);

		// Apply preferences - STRING should be visible since it's not in preferences
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.applyPreferences(preferences);

		columnModel = tablePanel.table().columnModel();
		// New columns (not in preferences) should remain visible
		assertTrue(columnModel.visible(Detail.STRING).is());
	}

	@Test
	void roundTrip() {
		// Comprehensive round-trip test
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, testEntities, CONNECTION_PROVIDER);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumnModel<Attribute<?>> columnModel = tablePanel.table().columnModel();

		// Configure everything
		columnModel.visible(Detail.STRING).set(false);
		columnModel.moveColumn(columnModel.getColumnIndex(Detail.DOUBLE), 0);
		columnModel.column(Detail.INT).setWidth(155);
		columnModel.column(Detail.INT).setPreferredWidth(155);
		tablePanel.table().setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		ConditionModel<?> stringCondition = tableModel.query().condition().get().get(Detail.STRING);
		stringCondition.autoEnable().set(false);
		stringCondition.caseSensitive().set(true);
		stringCondition.operands().wildcard().set(Wildcard.POSTFIX);

		// Save
		tablePanel.writePreferences(preferences);

		// Create fresh panel and restore
		tablePanel = new EntityTablePanel(tableModel);
		tablePanel.applyPreferences(preferences);

		// Verify everything
		columnModel = tablePanel.table().columnModel();
		assertFalse(columnModel.visible(Detail.STRING).is());
		assertEquals(0, columnModel.getColumnIndex(Detail.DOUBLE));
		assertEquals(155, columnModel.column(Detail.INT).getPreferredWidth());
		assertEquals(JTable.AUTO_RESIZE_OFF, tablePanel.table().getAutoResizeMode());

		stringCondition = tableModel.query().condition().get().get(Detail.STRING);
		assertFalse(stringCondition.autoEnable().is());
		assertTrue(stringCondition.caseSensitive().is());
		assertEquals(Wildcard.POSTFIX, stringCondition.operands().wildcard().get());
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
