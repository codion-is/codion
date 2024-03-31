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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.swing.framework.ui.EntityDialogs.addEntityDialog;
import static is.codion.swing.framework.ui.EntityDialogs.editEntityDialog;
import static java.awt.ComponentOrientation.getOrientation;
import static java.util.Objects.requireNonNull;

final class EntityControls {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityControls.class.getName());

	private EntityControls() {}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog and if insert is performed
	 * adds the new entity to the {@code comboBox} and selects it.
	 * Creates a INSERT key binding on the given component for triggering the resulting Control.
	 * @param comboBox the combo box in which to select the new entity
	 * @param editPanelSupplier the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @return the add Control
	 */
	static Control createAddControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier, KeyStroke keyStroke) {
		return createAddControl(() -> addEntityDialog(editPanelSupplier)
						.owner(comboBox)
						.onInsert(new EntityComboBoxOnInsert(comboBox.getModel()))
						.addEntity(), comboBox, keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog and if insert is performed
	 * selects the new entity in the {@code searchField}.
	 * Creates a INSERT key binding on the given component for triggering the resulting Control.
	 * @param searchField the search field in which to select the new entity
	 * @param editPanelSupplier the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @return the add Control
	 */
	static Control createAddControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier, KeyStroke keyStroke) {
		return createAddControl(() -> addEntityDialog(editPanelSupplier)
						.owner(searchField)
						.onInsert(new EntitySearchFieldOnInsert(searchField.model()))
						.addEntity(), searchField, keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog displaying
	 * the selected item for editing, and replaces the updated entity in the combo box.
	 * Creates a CTRL-INSERT key binding on the given component for triggering the resulting Control.
	 * @param comboBox the combo box which selected item to edit
	 * @param editPanelSupplier the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @return the edit Control
	 */
	static Control createEditControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier, KeyStroke keyStroke) {
		return createEditControl(() -> editEntityDialog(editPanelSupplier)
						.owner(comboBox)
						.entity(() -> comboBox.getModel().selectedValue())
						.onUpdate(new EntityComboBoxOnUpdate(comboBox.getModel()))
						.editEntity(), comboBox, comboBox.getModel().selectionEmpty().not(), keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog displaying
	 * the selected item for editing, and replaces the updated entity in the search field.
	 * Creates a CTRL-INSERT key binding on the given component for triggering the resulting Control.
	 * @param searchField the search field which selected item to edit
	 * @param editPanelSupplier the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @return the edit Control
	 */
	static Control createEditControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier, KeyStroke keyStroke) {
		return createEditControl(() -> editEntityDialog(editPanelSupplier)
						.owner(searchField)
						.entity(() -> searchField.model().entity().get())
						.onUpdate(new EntitySearchFieldOnUpdate(searchField.model()))
						.editEntity(), searchField, searchField.model().selectionEmpty().not(), keyStroke);
	}

	static String validateButtonLocation(String buttonLocation) {
		requireNonNull(buttonLocation);
		if (!buttonLocation.equals(BorderLayout.WEST) && !buttonLocation.equals(BorderLayout.EAST)) {
			throw new IllegalArgumentException("Button location must be BorderLayout.WEST or BorderLayout.EAST");
		}
		return buttonLocation;
	}

	static JPanel createButtonPanel(JComponent centerComponent, boolean buttonFocusable,
																	String borderLayoutConstraints, List<AbstractButton> buttons,
																	Action... buttonActions) {
		Dimension preferredSize = centerComponent.getPreferredSize();

		return Components.panel(new BorderLayout())
						.add(centerComponent, BorderLayout.CENTER)
						.add(Components.buttonPanel(buttonActions)
										.buttonsFocusable(buttonFocusable)
										.preferredButtonSize(new Dimension(preferredSize.height, preferredSize.height))
										.buttonGap(0)
										.buttonBuilder(buttonBuilder -> buttonBuilder.onBuild(buttons::add))
										.build(), borderLayoutConstraints)
						.build();
	}

	static String defaultButtonLocation() {
		return getOrientation(Locale.getDefault()) == ComponentOrientation.LEFT_TO_RIGHT ? BorderLayout.EAST : BorderLayout.WEST;
	}

	private static Control createAddControl(Command addEntityCommand, JComponent component, KeyStroke keyStroke) {
		Control control = Control.builder(addEntityCommand)
						.smallIcon(FrameworkIcons.instance().add())
						.description(MESSAGES.getString("add_new"))
						.enabled(createComponentEnabledState(component))
						.build();

		KeyEvents.builder(keyStroke)
						.action(control)
						.enable(component);

		return control;
	}

	private static Control createEditControl(Command editEntityCommand, JComponent component,
																					 StateObserver selectionNonEmptyState, KeyStroke keyStroke) {
		Control control = Control.builder(editEntityCommand)
						.smallIcon(FrameworkIcons.instance().edit())
						.description(MESSAGES.getString("edit_selected"))
						.enabled(State.and(createComponentEnabledState(component), selectionNonEmptyState))
						.build();

		KeyEvents.builder(keyStroke)
						.action(control)
						.enable(component);

		return control;
	}

	private static State createComponentEnabledState(JComponent component) {
		State componentEnabledState = State.state(component.isEnabled());
		component.addPropertyChangeListener("enabled", changeEvent ->
						componentEnabledState.set((Boolean) changeEvent.getNewValue()));

		return componentEnabledState;
	}

	private static class EntityComboBoxOnInsert implements Consumer<Entity> {

		private final EntityComboBoxModel comboBoxModel;

		private EntityComboBoxOnInsert(EntityComboBoxModel comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
		}

		@Override
		public void accept(Entity inserted) {
			comboBoxModel.add(inserted);
			comboBoxModel.setSelectedItem(inserted);
		}
	}

	private static class EntitySearchFieldOnInsert implements Consumer<Entity> {

		private final EntitySearchModel searchModel;

		private EntitySearchFieldOnInsert(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public void accept(Entity inserted) {
			searchModel.entity().set(inserted);
		}
	}

	private static final class EntityComboBoxOnUpdate implements Consumer<Entity> {

		private final EntityComboBoxModel comboBoxModel;

		private EntityComboBoxOnUpdate(EntityComboBoxModel comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
		}

		@Override
		public void accept(Entity updated) {
			comboBoxModel.replace(comboBoxModel.selectedValue(), updated);
			comboBoxModel.setSelectedItem(updated);
		}
	}

	private static final class EntitySearchFieldOnUpdate implements Consumer<Entity> {

		private final EntitySearchModel searchModel;

		private EntitySearchFieldOnUpdate(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public void accept(Entity updated) {
			searchModel.entity().set(updated);
		}
	}
}
