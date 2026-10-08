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
 * Copyright (c) 2024 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.proxy.ProxyBuilder;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableColumn;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.Controls.ControlsKey;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Detail;
import is.codion.swing.framework.ui.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.utilities.Operator.*;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.awt.event.ActionEvent.ACTION_PERFORMED;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.VK_A;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityTablePanelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	@Test
	void tableModelWithoutConditions() {
		// a table model of the given entities has no query conditions, so no condition panel
		EntityTablePanel tablePanel = new EntityTablePanel(SwingEntityTableModel.of(Employee.TYPE, emptyList(), CONNECTION));
		tablePanel.initialize();
		assertThrows(IllegalStateException.class, tablePanel::conditions);
	}

	@Test
	void conditionAndFilterPanels() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		// excluded, the condition and filter models unaffected, available for use programmatically
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config
						.conditions(conditions -> conditions.exclude(Employee.DEPARTMENT_FK))
						.filters(filters -> filters.exclude(Employee.NAME)));
		assertFalse(tablePanel.conditions().get().containsKey(Employee.DEPARTMENT_FK));
		assertNotNull(tableModel.query().conditions().get(Employee.DEPARTMENT_FK));
		assertFalse(tablePanel.table().filters().get().containsKey(Employee.NAME));
		assertNotNull(tableModel.filters().get(Employee.NAME));
		// a condition model of an attribute which is not a column, the foreign key source column
		assertThrows(IllegalArgumentException.class, () -> new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION),
						config -> config.conditions(conditions -> conditions.exclude(Employee.DEPARTMENT))));
		// another entity's attribute
		assertThrows(IllegalArgumentException.class, () -> new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION),
						config -> config.filters(filters -> filters.exclude(Detail.STRING))));
	}

	@Test
	void conditionComboBoxRefreshesOnEnter() throws Exception {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresher().async().set(false);
		AtomicInteger refreshed = new AtomicInteger();
		tableModel.items().refresher().result().addListener(refreshed::incrementAndGet);
		Entity accounting = CONNECTION.selectSingle(Department.ID.equalTo(10));
		SwingUtilities.invokeAndWait(() -> {
			EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
			// the condition panel components are created once the panel is first shown
			tablePanel.conditions().view().set(ConditionView.SIMPLE);
			JComboBox<?> department = (JComboBox<?>) ((ColumnConditionPanel<?>) tablePanel.conditions().get(Employee.DEPARTMENT_FK))
							.operands().equal().orElseThrow(IllegalStateException::new);
			tableModel.query().conditions().get(Employee.DEPARTMENT_FK).operands().equal().set(accounting);
			department.getActionMap().get("enterPressed").actionPerformed(new ActionEvent(department, ACTION_PERFORMED, null));
		});
		// the Enter action deferred until the combo box has committed its editor
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(1, refreshed.get());
	}

	@Test
	void excludeHiddenColumns() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertTrue(employee.contains(Employee.ID));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.COMMISSION));
			assertTrue(employee.contains(Employee.DEPARTMENT));
			assertTrue(employee.contains(Employee.HIREDATE));
			assertTrue(employee.contains(Employee.JOB));
		});
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.excludeHiddenColumns(true));
		tablePanel.table().columns().visible().set(Employee.ID, Employee.NAME, Employee.COMMISSION);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertTrue(employee.contains(Employee.ID));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.COMMISSION));
			assertFalse(employee.contains(Employee.DEPARTMENT_FK));
			assertFalse(employee.contains(Employee.HIREDATE));
			assertFalse(employee.contains(Employee.JOB));
		});
	}

	@Test
	void index() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		assertEquals(0, tablePanel.table().columns().indexOf(Detail.INT));
		assertEquals(1, tablePanel.table().columns().indexOf(Detail.DOUBLE));
		assertEquals(2, tablePanel.table().columns().indexOf(Detail.BIG_DECIMAL));
		assertEquals(3, tablePanel.table().columns().indexOf(Detail.STRING));
		assertEquals(4, tablePanel.table().columns().indexOf(Detail.DATE));
		assertEquals(5, tablePanel.table().columns().indexOf(Detail.TIME));
		assertEquals(6, tablePanel.table().columns().indexOf(Detail.TIMESTAMP));
		assertEquals(7, tablePanel.table().columns().indexOf(Detail.OFFSET));
		assertEquals(8, tablePanel.table().columns().indexOf(Detail.BOOLEAN));
		assertEquals(9, tablePanel.table().columns().indexOf(Detail.BOOLEAN_NULLABLE));
		assertEquals(10, tablePanel.table().columns().indexOf(Detail.MASTER_FK));
		assertEquals(11, tablePanel.table().columns().indexOf(Detail.DETAIL_FK));
		assertEquals(12, tablePanel.table().columns().indexOf(Detail.MASTER_NAME));
		assertEquals(13, tablePanel.table().columns().indexOf(Detail.MASTER_CODE));
		assertEquals(14, tablePanel.table().columns().indexOf(Detail.INT_ITEMS));
		assertEquals(15, tablePanel.table().columns().indexOf(Detail.INT_DERIVED));
	}

	@Test
	void enumConditionAndFilter() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		ColumnConditionPanel<?> condition = (ColumnConditionPanel<?>) tablePanel.conditions().get(Detail.ENUM_TYPE);
		assertEquals(asList(EQUAL, NOT_EQUAL, IN, NOT_IN), condition.model().operators());
		assertInstanceOf(JComboBox.class, condition.operands().equal().orElseThrow());
		ColumnConditionPanel<?> filter = (ColumnConditionPanel<?>) tablePanel.table().filters().get(Detail.ENUM_TYPE);
		assertEquals(asList(EQUAL, NOT_EQUAL, IN, NOT_IN), filter.model().operators());
		assertInstanceOf(JComboBox.class, filter.operands().equal().orElseThrow());
	}

	@Test
	void columnModel() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel);
		FilterTableColumn<Attribute<?>> column = tablePanel.table().columns().get(Detail.STRING);
		assertEquals(Detail.STRING, column.identifier());
	}

	@Test
	void summaryPanelVisibleWithoutSummaryPanelDoesNotBrick() {
		//a summary-visible state of true on a panel that ends up without a summary panel
		//must not throw during initialize() and permanently brick the panel
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeSummaries(false));
		tablePanel.summaryPanelVisible().set(true);
		assertDoesNotThrow(tablePanel::initialize);
		//downgraded to false since no summary panel is available
		assertFalse(tablePanel.summaryPanelVisible().is());
	}

	@Test
	void editableAttributesExcludesDerivedAndDenormalizedAttributes() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, CONNECTION);
		new EntityTablePanel(tableModel, config -> config.editable(attributes -> {
			assertEquals(14, attributes.size());
			assertFalse(attributes.contains(Detail.MASTER_NAME));
			assertFalse(attributes.contains(Detail.MASTER_CODE));
			assertFalse(attributes.contains(Detail.INT_DERIVED));
		}));
	}

	@Test
	void initializeReentrant() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		AtomicInteger layouts = new AtomicInteger();
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel) {
			@Override
			protected void setupControls() {
				//as a nested event loop pumped during the initialization might
				initialize();
			}

			@Override
			protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
				layouts.incrementAndGet();
				super.layoutPanel(tableComponent, southPanel);
			}
		};
		tablePanel.initialize();
		assertEquals(1, layouts.get());
	}

	@Test
	void controlSetInSetupControls() {
		SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, CONNECTION);
		CommandControl print = Control.builder().command(() -> {}).build();
		EntityTablePanel tablePanel = new EntityTablePanel(tableModel) {
			@Override
			protected void setupControls() {
				control(PRINT).set(print);
			}
		};
		tablePanel.initialize();
		assertSame(print, tablePanel.control(PRINT).get());
	}

	@Test
	void keyStrokes() {
		// every control given a key stroke is bound, the ones acting on the table on the table, the others on the panel
		List<ControlKey<?>> controlKeys = controlMap(EntityTablePanel.ControlKeys.class).keys().stream()
						.filter(controlKey -> !(controlKey instanceof ControlsKey))
						.collect(toList());
		EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION), config ->
						controlKeys.forEach(controlKey -> config.keyStroke(controlKey, keyStroke ->
										keyStroke.set(keyStroke(controlKeys.indexOf(controlKey))))));
		tablePanel.initialize();
		for (ControlKey<?> controlKey : controlKeys) {
			Control control = tablePanel.control(controlKey).get();
			if (control != null) {
				KeyStroke keyStroke = keyStroke(controlKeys.indexOf(controlKey));
				assertTrue(bound(tablePanel.table(), JComponent.WHEN_FOCUSED, keyStroke, control)
								|| bound(tablePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, keyStroke, control), controlKey.name());
			}
		}
		assertTrue(bound(tablePanel.table(), JComponent.WHEN_FOCUSED,
						keyStroke(controlKeys.indexOf(CLEAR_SELECTION)), tablePanel.control(CLEAR_SELECTION).get()));
		assertTrue(bound(tablePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
						keyStroke(controlKeys.indexOf(RESET_COLUMNS)), tablePanel.control(RESET_COLUMNS).get()));
	}

	@Test
	void keyStrokeCollision() {
		// two controls available in the same place can not have the same key stroke
		KeyStroke refresh = REFRESH.defaultKeystroke().getOrThrow();
		EntityTablePanel collision = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION), config ->
						config.keyStroke(RESET_COLUMNS, keyStroke -> keyStroke.set(refresh)));
		String message = assertThrows(IllegalStateException.class, collision::initialize).getMessage();
		assertTrue(message.contains(REFRESH.name()) && message.contains(RESET_COLUMNS.name()));
		// a table control may have the key stroke of a panel control, available while the table has the focus
		EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION), config ->
						config.keyStroke(CLEAR_SELECTION, keyStroke -> keyStroke.set(refresh)));
		tablePanel.initialize();
		assertTrue(bound(tablePanel.table(), JComponent.WHEN_FOCUSED, refresh, tablePanel.control(CLEAR_SELECTION).get()));
		assertTrue(bound(tablePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, refresh, tablePanel.control(REFRESH).get()));
	}

	@Test
	void tableControlsReplaced() {
		// the table panel controls replace the table ones, which have no key stroke
		EntityTablePanel tablePanel = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION));
		tablePanel.initialize();
		KeyStroke copyCell = COPY_CELL.defaultKeystroke().getOrThrow();
		KeyStroke focusSearchField = FOCUS_SEARCH_FIELD.defaultKeystroke().getOrThrow();
		assertTrue(bound(tablePanel.table(), JComponent.WHEN_FOCUSED, copyCell, tablePanel.control(COPY_CELL).get()));
		assertNull(tablePanel.table().getInputMap(JComponent.WHEN_FOCUSED).get(focusSearchField));
		assertTrue(bound(tablePanel, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, focusSearchField, tablePanel.control(FOCUS_SEARCH_FIELD).get()));
		// unless configured for the table
		KeyStroke keyStroke = KeyStroke.getKeyStroke(VK_A, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK);
		EntityTablePanel configured = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION), config ->
						config.table(builder -> builder.keyStroke(FilterTable.ControlKeys.FOCUS_SEARCH_FIELD, keyStroke)));
		configured.initialize();
		assertNotNull(configured.table().getInputMap(JComponent.WHEN_FOCUSED).get(keyStroke));
	}

	@Test
	void inspectQueryControl() {
		EntityTablePanel excluded = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION));
		excluded.initialize();
		assertNull(excluded.control(INSPECT_QUERY).get());
		EntityTablePanel included = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, CONNECTION),
						config -> config.includeInspector(true));
		included.initialize();
		assertNotNull(included.control(INSPECT_QUERY).get());
		String url = Database.URL.get();
		Database.URL.set(null);
		try {
			// a connection other than a local one renders via Database.instance(), unavailable without a url
			EntityConnection nonLocal = ProxyBuilder.of(EntityConnection.class)
							.delegate(CONNECTION)
							.build();
			EntityTablePanel unavailable = new EntityTablePanel(new SwingEntityTableModel(Employee.TYPE, nonLocal),
							config -> config.includeInspector(true));
			unavailable.initialize();
			assertNull(unavailable.control(INSPECT_QUERY).get());
		}
		finally {
			Database.URL.set(url);
		}
	}

	private static KeyStroke keyStroke(int index) {
		return KeyStroke.getKeyStroke(VK_A + index % 26, CTRL_DOWN_MASK | ALT_DOWN_MASK | SHIFT_DOWN_MASK | (index < 26 ? 0 : META_DOWN_MASK));
	}

	private static boolean bound(JComponent component, int condition, KeyStroke keyStroke, Control control) {
		Object actionKey = component.getInputMap(condition).get(keyStroke);

		return actionKey != null && component.getActionMap().get(actionKey) == control;
	}
}
