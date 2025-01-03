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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.resource.MessageBundle;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.framework.ui.EntityDialogs.addEntityDialog;
import static is.codion.swing.framework.ui.EntityDialogs.editEntityDialog;
import static java.awt.ComponentOrientation.getOrientation;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

final class EntityControls {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityControls.class, getBundle(EntityControls.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	private EntityControls() {}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanel} in a dialog and if insert is performed
	 * adds the new entity to the {@code comboBox} and selects it.
	 * Creates a key binding on the given component for triggering the resulting Control.
	 * @param comboBox the combo box in which to select the new entity
	 * @param editPanel the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @param confirm true if the insert should be confirmed
	 * @return the add Control
	 */
	static CommandControl createAddControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return createAddControl(() -> addEntityDialog(editPanel)
						.owner(comboBox)
						.confirm(confirm)
						.onInsert(new EntityComboBoxOnInsert(comboBox.getModel()))
						.show(), comboBox, keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanel} in a dialog and if insert is performed
	 * selects the new entity in the {@code searchField}.
	 * Creates a key binding on the given component for triggering the resulting Control.
	 * @param searchField the search field in which to select the new entity
	 * @param editPanel the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @param confirm true if the insert should be confirmed
	 * @return the add Control
	 */
	static CommandControl createAddControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return createAddControl(() -> addEntityDialog(editPanel)
						.owner(searchField)
						.confirm(confirm)
						.onInsert(new EntitySearchFieldOnInsert(searchField.model()))
						.show(), searchField, keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanel} in a dialog displaying
	 * the selected item for editing, and replaces the updated entity in the combo box.
	 * Creates a key binding on the given component for triggering the resulting Control.
	 * @param comboBox the combo box which selected item to edit
	 * @param editPanel the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @param confirm true if the update should be confirmed
	 * @return the edit Control
	 */
	static CommandControl createEditControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return createEditControl(() -> editEntityDialog(editPanel)
						.owner(comboBox)
						.confirm(confirm)
						.entity(() -> comboBox.getModel().selection().item().getOrThrow())
						.onUpdate(new EntityComboBoxOnUpdate(comboBox.getModel()))
						.show(), comboBox, comboBox.getModel().selection().empty().not(), keyStroke);
	}

	/**
	 * Creates a new Control which displays the edit panel provided by the {@code editPanel} in a dialog displaying
	 * the selected item for editing, and replaces the updated entity in the search field.
	 * Creates a key binding on the given component for triggering the resulting Control.
	 * @param searchField the search field which selected item to edit
	 * @param editPanel the edit panel supplier
	 * @param keyStroke the control keyStroke
	 * @param confirm true if the update should be confirmed
	 * @return the edit Control
	 */
	static CommandControl createEditControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanel, KeyStroke keyStroke, boolean confirm) {
		return createEditControl(() -> editEntityDialog(editPanel)
						.owner(searchField)
						.confirm(confirm)
						.entity(() -> searchField.model().selection().entity().getOrThrow())
						.onUpdate(new EntitySearchFieldOnUpdate(searchField.model()))
						.show(), searchField, searchField.model().selection().empty().not(), keyStroke);
	}

	static String validateButtonLocation(String buttonLocation) {
		requireNonNull(buttonLocation);
		if (!buttonLocation.equals(BorderLayout.WEST) && !buttonLocation.equals(BorderLayout.EAST)) {
			throw new IllegalArgumentException("Button location must be BorderLayout.WEST or BorderLayout.EAST");
		}
		return buttonLocation;
	}

	static JComponent createButtonPanel(JComponent centerComponent, boolean buttonFocusable,
																			String borderLayoutConstraints, List<AbstractButton> buttons,
																			Action... buttonActions) {
		if (buttonActions.length == 0) {
			return centerComponent;
		}
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

	private static CommandControl createAddControl(Command addEntityCommand, JComponent component, KeyStroke keyStroke) {
		CommandControl control = Control.builder()
						.command(addEntityCommand)
						.smallIcon(ICONS.add())
						.description(MESSAGES.getString("add_new"))
						.enabled(createComponentEnabledState(component))
						.build();
		if (keyStroke != null) {
			KeyEvents.builder(keyStroke)
							.action(control)
							.enable(component);
		}

		return control;
	}

	private static CommandControl createEditControl(Command editEntityCommand, JComponent component,
																									ObservableState selectionNonEmptyState, KeyStroke keyStroke) {
		CommandControl control = Control.builder()
						.command(editEntityCommand)
						.smallIcon(ICONS.edit())
						.description(MESSAGES.getString("edit_selected"))
						.enabled(State.and(createComponentEnabledState(component), selectionNonEmptyState))
						.build();
		if (keyStroke != null) {
			KeyEvents.builder(keyStroke)
							.action(control)
							.enable(component);
		}

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
			comboBoxModel.items().add(inserted);
			comboBoxModel.selection().item().set(inserted);
		}
	}

	private static class EntitySearchFieldOnInsert implements Consumer<Entity> {

		private final EntitySearchModel searchModel;

		private EntitySearchFieldOnInsert(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public void accept(Entity inserted) {
			searchModel.selection().entity().set(inserted);
		}
	}

	private static final class EntityComboBoxOnUpdate implements Consumer<Entity> {

		private final EntityComboBoxModel comboBoxModel;

		private EntityComboBoxOnUpdate(EntityComboBoxModel comboBoxModel) {
			this.comboBoxModel = comboBoxModel;
		}

		@Override
		public void accept(Entity updated) {
			comboBoxModel.items().replace(comboBoxModel.selection().item().get(), updated);
			comboBoxModel.selection().item().set(updated);
		}
	}

	private static final class EntitySearchFieldOnUpdate implements Consumer<Entity> {

		private final EntitySearchModel searchModel;

		private EntitySearchFieldOnUpdate(EntitySearchModel searchModel) {
			this.searchModel = searchModel;
		}

		@Override
		public void accept(Entity updated) {
			searchModel.selection().entity().set(updated);
		}
	}
}
