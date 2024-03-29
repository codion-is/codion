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
import is.codion.framework.db.EntityConnectionProvider;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
		private final EntityConnectionProvider connectionProvider;
		private final Consumer<List<Entity>> onInsert;
		private final List<Entity> insertedEntities = new ArrayList<>();
		private final Consumer<Collection<Entity>> populateInsertedEntities = new PopulateInsertedEntities();

		private AddEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = comboBox;
			this.connectionProvider = comboBox.getModel().connectionProvider();
			this.onInsert = new EntityComboBoxOnInsert();
		}

		private AddEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = searchField;
			this.connectionProvider = searchField.model().connectionProvider();
			this.onInsert = new EntitySearchFieldOnInsert();
		}

		@Override
		public void execute() {
			EntityEditPanel editPanel = initializeEditPanel();
			editPanel.editModel().defaults();
			State cancelled = State.state();
			Value<Attribute<?>> invalidAttribute = Value.value();
			JDialog dialog = Dialogs.okCancelDialog(editPanel)
							.owner(component)
							.title(connectionProvider.entities().definition(editPanel.editModel().entityType()).caption())
							.onShown(d -> invalidAttribute.optional()
											.ifPresent(editPanel::requestComponentFocus))
							.onCancel(() -> cancelled.set(true))
							.build();
			try {
				boolean successfulInsert = false;
				while (!successfulInsert) {
					dialog.setVisible(true);
					if (cancelled.get()) {
						return;//cancelled
					}
					successfulInsert = insert(editPanel.editModel(), invalidAttribute);
					if (successfulInsert && !insertedEntities.isEmpty()) {
						onInsert.accept(insertedEntities);
					}
				}
			}
			finally {
				insertedEntities.clear();
			}
		}

		private EntityEditPanel initializeEditPanel() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			editPanel.setBorder(emptyBorder());
			editPanel.editModel().addAfterInsertListener(populateInsertedEntities);

			return editPanel;
		}

		private boolean insert(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
			try {
				editModel.insert();

				return true;
			}
			catch (ValidationException e) {
				attributeWithInvalidValue.set(e.attribute());
				JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception e) {
				Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
			}

			return false;
		}

		private final class PopulateInsertedEntities implements Consumer<Collection<Entity>> {

			@Override
			public void accept(Collection<Entity> inserted) {
				insertedEntities.clear();
				insertedEntities.addAll(inserted);
			}
		}

		private class EntityComboBoxOnInsert implements Consumer<List<Entity>> {

			@Override
			public void accept(List<Entity> inserted) {
				EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
				Entity item = inserted.get(0);
				comboBoxModel.add(item);
				comboBoxModel.setSelectedItem(item);
			}
		}

		private class EntitySearchFieldOnInsert implements Consumer<List<Entity>> {

			@Override
			public void accept(List<Entity> inserted) {
				((EntitySearchField) component).model().entities().set(inserted);
			}
		}
	}

	private static final class EditEntityCommand implements Control.Command {

		private final Supplier<EntityEditPanel> editPanelSupplier;
		private final JComponent component;
		private final EntityConnectionProvider connectionProvider;
		private final Consumer<List<Entity>> onUpdate;
		private final List<Entity> updatedEntities = new ArrayList<>();
		private final PopulateUpdatedEntities populateUpdatedEntities = new PopulateUpdatedEntities();

		private Entity entityToUpdate;

		private EditEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = comboBox;
			this.connectionProvider = comboBox.getModel().connectionProvider();
			this.onUpdate = new EntityComboBoxOnUpdate();
		}

		private EditEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = editPanelSupplier;
			this.component = searchField;
			this.connectionProvider = searchField.model().connectionProvider();
			this.onUpdate = new EntitySearchFieldOnUpdate();
		}

		@Override
		public void execute() throws Exception {
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
			editPanel.editModel().set(connectionProvider.connection().select(entityToUpdate.primaryKey()));
			State cancelled = State.state();
			Value<Attribute<?>> invalidAttribute = Value.value();
			JDialog dialog = Dialogs.okCancelDialog(editPanel)
							.owner(component)
							.title(connectionProvider.entities().definition(editPanel.editModel().entityType()).caption())
							.okEnabled(editPanel.editModel().modified())
							.onShown(d -> invalidAttribute.optional()
											.ifPresent(editPanel::requestComponentFocus))
							.onCancel(() -> cancelled.set(true))
							.build();
			try {
				boolean successfulUpdate = false;
				while (!successfulUpdate) {
					dialog.setVisible(true);
					if (cancelled.get()) {
						return;//cancelled
					}
					successfulUpdate = update(editPanel.editModel(), invalidAttribute);
					if (successfulUpdate && !updatedEntities.isEmpty()) {
						onUpdate.accept(updatedEntities);
					}
				}
			}
			finally {
				entityToUpdate = null;
				updatedEntities.clear();
			}
		}

		private EntityEditPanel initializeEditPanel() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			editPanel.setBorder(emptyBorder());
			editPanel.editModel().addAfterUpdateListener(populateUpdatedEntities);

			return editPanel;
		}

		private boolean update(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
			try {
				if (editModel.modified().get()) {
					editModel.update();
				}

				return true;
			}
			catch (ValidationException e) {
				attributeWithInvalidValue.set(e.attribute());
				JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
			}
			catch (Exception e) {
				Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
			}

			return false;
		}

		private final class EntityComboBoxOnUpdate implements Consumer<List<Entity>> {

			@Override
			public void accept(List<Entity> updated) {
				EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
				Entity item = updated.get(0);
				comboBoxModel.replace(entityToUpdate, item);
				comboBoxModel.setSelectedItem(item);
			}
		}

		private final class EntitySearchFieldOnUpdate implements Consumer<List<Entity>> {

			@Override
			public void accept(List<Entity> updated) {
				((EntitySearchField) component).model().entities().set(updated);
			}
		}

		private final class PopulateUpdatedEntities implements Consumer<Map<Entity.Key, Entity>> {

			@Override
			public void accept(Map<Entity.Key, Entity> updated) {
				updatedEntities.clear();
				updatedEntities.addAll(updated.values());
			}
		}
	}
}
