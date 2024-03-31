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

import is.codion.common.i18n.Messages;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
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

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
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
		return createAddControl(new AddEntityCommand(requireNonNull(comboBox), requireNonNull(editPanelSupplier)), comboBox, keyStroke);
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
		return createAddControl(new AddEntityCommand(requireNonNull(searchField), requireNonNull(editPanelSupplier)), searchField, keyStroke);
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
		return createEditControl(new EditEntityCommand(requireNonNull(comboBox), requireNonNull(editPanelSupplier)),
						comboBox, comboBox.getModel().selectionEmpty().not(), keyStroke);
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
		return createEditControl(new EditEntityCommand(requireNonNull(searchField), requireNonNull(editPanelSupplier)),
						searchField, searchField.model().selectionEmpty().not(), keyStroke);
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

	private static Control createAddControl(AddEntityCommand addEntityCommand, JComponent component, KeyStroke keyStroke) {
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

	private static Control createEditControl(EditEntityCommand editEntityCommand, JComponent component,
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

	private static final class AddEntityCommand implements Control.Command {

		private final Supplier<EntityEditPanel> editPanelSupplier;
		private final JComponent component;
		private final Consumer<Entity> onInsert;

		private AddEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = comboBox;
			this.onInsert = new EntityComboBoxOnInsert();
		}

		private AddEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = searchField;
			this.onInsert = new EntitySearchFieldOnInsert();
		}

		@Override
		public void execute() {
			EntityEditPanel editPanel = initializeEditPanel();
			editPanel.editModel().defaults();
			State cancelled = State.state();
			Value<Attribute<?>> invalid = Value.value();
			JDialog dialog = Dialogs.okCancelDialog(editPanel)
							.owner(component)
							.title(editPanel.editModel().entities().definition(editPanel.editModel().entityType()).caption())
							.okEnabled(editPanel.editModel().valid())
							.onShown(d -> invalid.optional()
											.ifPresent(editPanel::requestComponentFocus))
							.onCancel(() -> cancelled.set(true))
							.build();
			Entity inserted = null;
			while (inserted == null) {
				dialog.setVisible(true);
				if (cancelled.get()) {
					return;
				}
				inserted = insert(editPanel.editModel(), invalid);
				if (inserted != null) {
					onInsert.accept(inserted);
				}
			}
		}

		private EntityEditPanel initializeEditPanel() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			editPanel.setBorder(emptyBorder());

			return editPanel;
		}

		private Entity insert(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
			try {
				return editModel.insert();
			}
			catch (ValidationException e) {
				attributeWithInvalidValue.set(e.attribute());
				JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception e) {
				Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
			}

			return null;
		}

		private class EntityComboBoxOnInsert implements Consumer<Entity> {

			@Override
			public void accept(Entity inserted) {
				EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
				comboBoxModel.add(inserted);
				comboBoxModel.setSelectedItem(inserted);
			}
		}

		private class EntitySearchFieldOnInsert implements Consumer<Entity> {

			@Override
			public void accept(Entity inserted) {
				((EntitySearchField) component).model().entity().set(inserted);
			}
		}
	}

	private static final class EditEntityCommand implements Control.Command {

		private final Supplier<EntityEditPanel> editPanelSupplier;
		private final JComponent component;
		private final Consumer<Entity> onUpdate;

		private EditEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = comboBox;
			this.onUpdate = new EntityComboBoxOnUpdate();
		}

		private EditEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = searchField;
			this.onUpdate = new EntitySearchFieldOnUpdate();
		}

		@Override
		public void execute() throws Exception {
			Entity entityToUpdate;
			if (component instanceof EntityComboBox) {
				if (((EntityComboBox) component).isPopupVisible()) {
					((EntityComboBox) component).hidePopup();
				}
				entityToUpdate = ((EntityComboBox) component).getModel().selectedValue();
			}
			else {
				entityToUpdate = ((EntitySearchField) component).model().entity().get();
			}
			EntityEditPanel editPanel = initializeEditPanel();
			SwingEntityEditModel editModel = editPanel.editModel();
			editModel.set(editModel.connection().select(entityToUpdate.primaryKey()));
			State cancelled = State.state();
			Value<Attribute<?>> invalid = Value.value();
			JDialog dialog = Dialogs.okCancelDialog(editPanel)
							.owner(component)
							.title(editModel.entities().definition(editModel.entityType()).caption())
							.okEnabled(State.and(editModel.modified(), editModel.valid()))
							.onShown(d -> invalid.optional()
											.ifPresent(editPanel::requestComponentFocus))
							.onCancel(() -> cancelled.set(true))
							.build();
			Entity updated = null;
			while (updated == null) {
				dialog.setVisible(true);
				if (cancelled.get()) {
					return;
				}
				updated = update(editModel, invalid);
				if (updated != null) {
					onUpdate.accept(updated);
				}
			}
		}

		private EntityEditPanel initializeEditPanel() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			editPanel.setBorder(emptyBorder());

			return editPanel;
		}

		private Entity update(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
			try {
				if (editModel.modified().get()) {
					editModel.update();
				}

				return editModel.entity();
			}
			catch (ValidationException e) {
				attributeWithInvalidValue.set(e.attribute());
				JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception e) {
				Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
			}

			return null;
		}

		private final class EntityComboBoxOnUpdate implements Consumer<Entity> {

			@Override
			public void accept(Entity updated) {
				EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
				comboBoxModel.replace(comboBoxModel.selectedValue(), updated);
				comboBoxModel.setSelectedItem(updated);
			}
		}

		private final class EntitySearchFieldOnUpdate implements Consumer<Entity> {

			@Override
			public void accept(Entity updated) {
				((EntitySearchField) component).model().entity().set(updated);
			}
		}
	}
}
