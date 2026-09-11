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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityConditions;
import is.codion.framework.model.ForeignKeyConditionModel;
import is.codion.swing.common.ui.component.multivalue.MultiValueInput;
import is.codion.swing.framework.model.SwingEntityConditions;
import is.codion.swing.framework.ui.TestDomain.Department;
import is.codion.swing.framework.ui.TestDomain.Employee;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntitySearchField;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static java.awt.event.KeyEvent.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The components are exercised on the event dispatch thread, as in an application.
 */
public final class EntityConditionComponentsTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final EntityConditionComponents components =
					new EntityConditionComponents(CONNECTION.entities().definition(Employee.TYPE));

	@Test
	void inAddsTheEntitySelectedInASingleSelectionSearchFieldOnEnter() throws Exception {
		onEventDispatchThread(() -> {
			// search models for both operands, Department being a small dataset SwingEntityConditions would use combo boxes
			ForeignKeyConditionModel condition = departmentCondition(new EntityConditions(Employee.TYPE, CONNECTION));
			MultiValueInput<?> input = (MultiValueInput<?>) components.in(condition);
			EntitySearchField searchField = (EntitySearchField) input.component();
			assertSame(condition.inSearchModel().orElseThrow(), searchField.model());

			Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
			Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
			// a search result selected is shown in the field, part of the operand before being added
			searchField.model().selection().entity().set(sales);
			assertTrue(searchField.model().selection().present().is());
			assertEquals(Set.of(sales), condition.operands().in().get());
			// Enter adds it, clearing the field for the next search
			assertTrue(enter(searchField));
			assertFalse(searchField.model().selection().present().is());
			assertEquals(Set.of(sales), condition.operands().in().get());
			searchField.model().selection().entity().set(research);
			assertTrue(enter(searchField));
			assertEquals(Set.of(sales, research), condition.operands().in().get());
			// Enter on the empty field is left alone, for the condition panel to refresh on
			assertFalse(enter(searchField));

			// the operand cleared, the condition say, the members follow
			condition.operands().in().clear();
			searchField.model().selection().entity().set(sales);
			assertEquals(Set.of(sales), condition.operands().in().get());

			// the operand set, by a master selection say, the pending selection cleared
			condition.operands().in().set(Set.of(research));
			assertFalse(searchField.model().selection().present().is());
			assertEquals(Set.of(research), condition.operands().in().get());
		});
	}

	@Test
	void inAddsTheEntitySelectedInAComboBoxOnInsert() throws Exception {
		ForeignKeyConditionModel condition = departmentCondition(new SwingEntityConditions(Employee.TYPE, CONNECTION));
		assertNotSame(condition.equalComboBoxModel().orElseThrow(), condition.inComboBoxModel().orElseThrow());
		// off the event dispatch thread, a refresh on it being asynchronous
		condition.inComboBoxModel().orElseThrow().items().refresh();
		onEventDispatchThread(() -> {
			MultiValueInput<?> input = (MultiValueInput<?>) components.in(condition);
			EntityComboBox comboBox = (EntityComboBox) input.component();
			assertSame(condition.inComboBoxModel().orElseThrow(), comboBox.model());

			Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
			Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
			// the entity selected is part of the operand before being added
			comboBox.model().selection().item().set(sales);
			assertEquals(Set.of(sales), condition.operands().in().get());
			// Insert adds it, clearing the combo box for the next
			insert(comboBox);
			assertNull(comboBox.model().selection().item().get());
			comboBox.model().selection().item().set(research);
			insert(comboBox);
			assertNull(comboBox.model().selection().item().get());
			assertEquals(Set.of(sales, research), condition.operands().in().get());
			// the EQUAL combo box untouched
			assertNull(condition.operands().equal().get());
			assertNull(condition.equalComboBoxModel().orElseThrow().selection().item().get());

			// the operand set, by a master selection say, the pending selection cleared
			comboBox.model().selection().item().set(sales);
			condition.operands().in().set(Set.of(research));
			assertNull(comboBox.model().selection().item().get());
			assertEquals(Set.of(research), condition.operands().in().get());
		});
	}

	@Test
	void equalComboBoxLinkedToTheOperand() throws Exception {
		ForeignKeyConditionModel condition = departmentCondition(new SwingEntityConditions(Employee.TYPE, CONNECTION));
		// off the event dispatch thread, a refresh on it being asynchronous
		condition.equalComboBoxModel().orElseThrow().items().refresh();
		onEventDispatchThread(() -> {
			EntityComboBox comboBox = (EntityComboBox) components.equal(condition);
			assertSame(condition.equalComboBoxModel().orElseThrow(), comboBox.model());

			Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
			Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
			comboBox.model().selection().item().set(sales);
			assertEquals(sales, condition.operands().equal().get());
			condition.operands().equal().set(research);
			assertEquals(research, comboBox.model().selection().item().get());
			condition.operands().equal().clear();
			assertNull(comboBox.model().selection().item().get());
		});
	}

	@Test
	void equalSearchFieldLinkedToTheOperand() throws Exception {
		onEventDispatchThread(() -> {
			ForeignKeyConditionModel condition = departmentCondition(new EntityConditions(Employee.TYPE, CONNECTION));
			EntitySearchField searchField = (EntitySearchField) components.equal(condition);
			assertSame(condition.equalSearchModel().orElseThrow(), searchField.model());

			Entity sales = CONNECTION.selectSingle(Department.NAME.equalTo("SALES"));
			Entity research = CONNECTION.selectSingle(Department.NAME.equalTo("RESEARCH"));
			searchField.model().selection().entity().set(sales);
			assertEquals(sales, condition.operands().equal().get());
			condition.operands().equal().set(research);
			assertEquals(research, searchField.model().selection().entity().get());
		});
	}

	private static void onEventDispatchThread(Runnable runnable) throws Exception {
		try {
			SwingUtilities.invokeAndWait(runnable);
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			}
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw e;
		}
	}

	/**
	 * Performs the Insert key binding the {@link MultiValueInput} installs on the component it wraps
	 */
	private static void insert(JComponent component) {
		Object actionKey = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.get(KeyStroke.getKeyStroke(VK_INSERT, 0));
		assertNotNull(actionKey);
		component.getActionMap().get(actionKey)
						.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, "insert"));
	}

	/**
	 * Presses Enter through the component's key listeners, in the order added, returning whether it was consumed
	 */
	private static boolean enter(JComponent component) {
		KeyEvent event = new KeyEvent(component, KEY_PRESSED, System.currentTimeMillis(), 0, VK_ENTER, CHAR_UNDEFINED);
		for (KeyListener listener : component.getKeyListeners()) {
			listener.keyPressed(event);
		}

		return event.isConsumed();
	}

	private static ForeignKeyConditionModel departmentCondition(Supplier<Map<Attribute<?>, ConditionModel<?>>> conditions) {
		return (ForeignKeyConditionModel) conditions.get().get(Employee.DEPARTMENT_FK);
	}
}
