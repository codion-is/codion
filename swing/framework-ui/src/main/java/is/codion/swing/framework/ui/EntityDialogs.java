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
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.resource.MessageBundle;
import is.codion.common.value.ValueObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionState;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.AbstractDialogBuilder;
import is.codion.swing.common.ui.dialog.DialogBuilder;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.DefaultEntityComponentFactory;
import is.codion.swing.framework.ui.component.EntityComponentFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.disposeParentWindow;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.dialog.Dialogs.*;
import static is.codion.swing.framework.ui.component.EntityComponents.entityComponents;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * Provides edit and selection dialogs for entities.
 */
public final class EntityDialogs {

	private static final MessageBundle EDIT_PANEL_MESSAGES =
					messageBundle(EntityEditPanel.class, getBundle(EntityEditPanel.class.getName()));
	private static final Consumer<?> EMPTY_CONSUMER = value -> {};

	private EntityDialogs() {}

	/**
	 * @param editModel the edit model to use
	 * @param attribute the attribute to edit
	 * @param <T> the attribute type
	 * @return a new builder
	 */
	public static <T> EditAttributeDialogBuilder<T> editAttributeDialog(SwingEntityEditModel editModel, Attribute<T> attribute) {
		return new DefaultEditAttributeDialogBuilder<>(editModel, attribute);
	}

	/**
	 * Creates a new {@link AddEntityDialogBuilder} instance.
	 * @param editPanel supplies the edit panel to use
	 * @return a new builder instance
	 */
	public static AddEntityDialogBuilder addEntityDialog(Supplier<EntityEditPanel> editPanel) {
		return new DefaultAddEntityDialogBuilder(editPanel);
	}

	/**
	 * Creates a new {@link EditEntityDialogBuilder} instance.
	 * @param editPanel supplies the edit panel to use
	 * @return a new builder instance
	 */
	public static EditEntityDialogBuilder editEntityDialog(Supplier<EntityEditPanel> editPanel) {
		return new DefaultEditEntityDialogBuilder(editPanel);
	}

	/**
	 * Creates a new {@link EntitySelectionDialogBuilder} instance for searching for and selecting one or more entities from a table model.
	 * @param tableModel the table model on which to base the table panel
	 * @return a new builder instance
	 */
	public static EntitySelectionDialogBuilder selectionDialog(SwingEntityTableModel tableModel) {
		return new DefaultEntitySelectionDialogBuilder(tableModel);
	}

	/**
	 * Builds a dialog for editing single attributes for one or more entities
	 * @param <T> the attribute type
	 */
	public interface EditAttributeDialogBuilder<T> extends DialogBuilder<EditAttributeDialogBuilder<T>> {

		/**
		 * @param componentFactory the component factory, if null then the default is used
		 * @return this builder
		 */
		EditAttributeDialogBuilder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory);

		/**
		 * @param onValidationException called on validation exception
		 * @return this builder
		 */
		EditAttributeDialogBuilder<T> onValidationException(Consumer<ValidationException> onValidationException);

		/**
		 * @param onException called on exception
		 * @return this builder
		 */
		EditAttributeDialogBuilder<T> onException(Consumer<Exception> onException);

		/**
		 * Displays a dialog for editing the given entity
		 * @param entity the entity to edit
		 */
		void edit(Entity entity);

		/**
		 * Displays a dialog for editing the given entities
		 * @param entities the entities to edit
		 */
		void edit(Collection<Entity> entities);
	}

	/**
	 * A builder for a dialog for inserting entities.
	 */
	public interface AddEntityDialogBuilder extends DialogBuilder<AddEntityDialogBuilder> {

		/**
		 * @param onInsert called after a successful insert
		 * @return this builder instance
		 */
		AddEntityDialogBuilder onInsert(Consumer<Entity> onInsert);

		/**
		 * @param closeDialog false if the dialog should not be closed after insert, default true
		 * @return this builder instance
		 */
		AddEntityDialogBuilder closeDialog(boolean closeDialog);

		/**
		 * @param confirm if true then a confirmation dialog is presented before inserting, default false
		 * @return this builder instance
		 */
		AddEntityDialogBuilder confirm(boolean confirm);

		/**
		 * Displays the dialog.
		 */
		void show();
	}

	/**
	 * A builder for a dialog for editing entities.
	 */
	public interface EditEntityDialogBuilder extends DialogBuilder<EditEntityDialogBuilder> {

		/**
		 * @param entity supplies the entity to edit
		 * @return this builder instance
		 */
		EditEntityDialogBuilder entity(Supplier<Entity> entity);

		/**
		 * @param onUpdate called after a successful update
		 * @return this builder instance
		 */
		EditEntityDialogBuilder onUpdate(Consumer<Entity> onUpdate);

		/**
		 * @param confirm if true then a confirmation dialog is presented before updating, default false
		 * @return this builder instance
		 */
		EditEntityDialogBuilder confirm(boolean confirm);

		/**
		 * Displays the dialog.
		 */
		void show();
	}

	/**
	 * A builder for a selection dialog.
	 */
	public interface EntitySelectionDialogBuilder extends DialogBuilder<EntitySelectionDialogBuilder> {

		/**
		 * @param dialogSize the preferred dialog size
		 * @return this builder instance
		 */
		EntitySelectionDialogBuilder dialogSize(Dimension dialogSize);

		/**
		 * Displays table for selecting a one or more entities
		 * @return a List containing the selected entities or an empty list in case the selection was cancelled
		 */
		List<Entity> select();

		/**
		 * Displays table for selecting a single entity
		 * @return the selected entity or {@link Optional#empty()} in case the selection was cancelled
		 */
		Optional<Entity> selectSingle();
	}

	private static final class DefaultEditAttributeDialogBuilder<T> extends AbstractDialogBuilder<EditAttributeDialogBuilder<T>>
					implements EditAttributeDialogBuilder<T> {

		private static final Logger LOG = LoggerFactory.getLogger(DefaultEditAttributeDialogBuilder.class);

		private final SwingEntityEditModel editModel;
		private final Attribute<T> attribute;

		private EntityComponentFactory<T, Attribute<T>, ?> componentFactory = new EditEntityComponentFactory<>();
		private Consumer<ValidationException> onValidationException = new DefaultValidationExceptionHandler();
		private Consumer<Exception> onException = new DefaultExceptionHandler();

		private DefaultEditAttributeDialogBuilder(SwingEntityEditModel editModel, Attribute<T> attribute) {
			this.editModel = requireNonNull(editModel);
			this.attribute = requireNonNull(attribute);
		}

		@Override
		public EditAttributeDialogBuilder<T> componentFactory(EntityComponentFactory<T, Attribute<T>, ?> componentFactory) {
			this.componentFactory = componentFactory == null ? new EditEntityComponentFactory<>() : componentFactory;
			return this;
		}

		@Override
		public EditAttributeDialogBuilder<T> onValidationException(Consumer<ValidationException> onValidationException) {
			this.onValidationException = requireNonNull(onValidationException);
			return this;
		}

		@Override
		public EditAttributeDialogBuilder<T> onException(Consumer<Exception> onException) {
			this.onException = requireNonNull(onException);
			return this;
		}

		@Override
		public void edit(Entity entity) {
			edit(singleton(requireNonNull(entity)));
		}

		@Override
		public void edit(Collection<Entity> entities) {
			Set<EntityType> entityTypes = requireNonNull(entities).stream()
							.map(Entity::entityType)
							.collect(toSet());
			if (entityTypes.isEmpty()) {
				return;//no entities
			}
			if (entityTypes.size() > 1) {
				throw new IllegalArgumentException("All entities must be of the same type when editing");
			}

			EntityDefinition entityDefinition = editModel.entityDefinition();
			AttributeDefinition<T> attributeDefinition = entityDefinition.attributes().definition(attribute);
			Collection<T> values = entities.stream()
							.map(entity -> entity.get(attribute))
							.collect(toSet());
			T initialValue = values.size() == 1 ? values.iterator().next() : null;
			ComponentValue<T, ?> componentValue = editSelectedComponentValue(attribute, initialValue);
			InputValidator<T> validator = new InputValidator<>(entityDefinition, attribute, componentValue);
			inputDialog(componentValue)
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.title(FrameworkMessages.edit())
							.caption(attributeDefinition.caption())
							.validator(validator)
							.show(new SuccessfulUpdate(entities.stream()
											.map(Entity::copy)
											.collect(toList())));
		}

		private ComponentValue<T, ? extends JComponent> editSelectedComponentValue(Attribute<T> attribute, T initialValue) {
			if (componentFactory == null) {
				EditEntityComponentFactory<T, Attribute<T>, JComponent> entityComponentFactory = new EditEntityComponentFactory<>();

				return entityComponentFactory.componentValue(attribute, editModel, initialValue);
			}

			return componentFactory.componentValue(attribute, editModel, initialValue);
		}

		private final class SuccessfulUpdate implements Predicate<T> {

			private final Collection<Entity> entities;

			private SuccessfulUpdate(Collection<Entity> entities) {
				this.entities = entities;
			}

			@Override
			public boolean test(T newValue) {
				entities.forEach(entity -> entity.put(attribute, newValue));
				try {
					progressWorkerDialog(editModel.createUpdate(entities.stream()
									.filter(Entity::modified)
									.collect(toList())).prepare()::perform)
									.title(EDIT_PANEL_MESSAGES.getString("updating"))
									.owner(owner)
									.location(location)
									.locationRelativeTo(locationRelativeTo)
									.onException(e -> {})
									.execute()
									.get()
									.handle();

					return true;
				}
				catch (ValidationException e) {
					LOG.debug(e.getMessage(), e);
					onValidationException.accept(e);
				}
				catch (ExecutionException e) {
					Throwable cause = e.getCause();
					LOG.error(e.getMessage(), e);
					if (cause instanceof Exception) {
						onException.accept((Exception) cause);
					}
					else {
						onException.accept(new RuntimeException(cause));
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new RuntimeException(e);
				}

				return false;
			}
		}

		private final class DefaultExceptionHandler implements Consumer<Exception> {

			@Override
			public void accept(Exception exception) {
				Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				if (focusOwner == null) {
					focusOwner = owner;
				}
				displayExceptionDialog(exception, parentWindow(focusOwner));
			}
		}

		private final class DefaultValidationExceptionHandler implements Consumer<ValidationException> {

			@Override
			public void accept(ValidationException exception) {
				requireNonNull(exception);
				String title = editModel.entityDefinition().attributes()
								.definition(exception.attribute())
								.caption();
				showMessageDialog(locationRelativeTo == null ? owner : locationRelativeTo, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
			}
		}

		private static final class InputValidator<T> implements Predicate<T> {

			private final EntityDefinition entityDefinition;
			private final Attribute<T> attribute;
			private final ComponentValue<T, ?> componentValue;

			private InputValidator(EntityDefinition entityDefinition, Attribute<T> attribute, ComponentValue<T, ?> componentValue) {
				this.entityDefinition = entityDefinition;
				this.attribute = attribute;
				this.componentValue = componentValue;
			}

			@Override
			public boolean test(T value) {
				Entity entity = entityDefinition.entity();
				entity.put(attribute, value);
				try {
					entityDefinition.validator().validate(entity, attribute);
					componentValue.validate(value);

					return true;
				}
				catch (ValidationException | IllegalArgumentException e) {
					return false;
				}
			}
		}
	}

	private static final class EditEntityComponentFactory<T, A extends Attribute<T>, C extends JComponent> extends DefaultEntityComponentFactory<T, A, C> {

		private static final int TEXT_INPUT_PANEL_COLUMNS = 20;

		@Override
		public ComponentValue<T, C> componentValue(A attribute, SwingEntityEditModel editModel, T initialValue) {
			AttributeDefinition<T> attributeDefinition = editModel.entityDefinition()
							.attributes().definition(attribute);
			if (attributeDefinition.items().isEmpty() && attribute.type().isString()) {
				//special handling for non-item based String attributes, text field panel instead of a text field
				return (ComponentValue<T, C>) entityComponents(editModel.entityDefinition())
								.textFieldPanel((Attribute<String>) attribute)
								.initialValue((String) initialValue)
								.columns(TEXT_INPUT_PANEL_COLUMNS)
								.buildValue();
			}

			return super.componentValue(attribute, editModel, initialValue);
		}
	}

	private static final class DefaultEntitySelectionDialogBuilder extends AbstractDialogBuilder<EntitySelectionDialogBuilder>
					implements EntitySelectionDialogBuilder {

		private final SwingEntityTableModel tableModel;

		private Dimension dialogSize;

		private DefaultEntitySelectionDialogBuilder(SwingEntityTableModel tableModel) {
			this.tableModel = requireNonNull(tableModel);
		}

		@Override
		public EntitySelectionDialogBuilder dialogSize(Dimension dialogSize) {
			this.dialogSize = requireNonNull(dialogSize);
			return this;
		}

		@Override
		public List<Entity> select() {
			return new EntitySelectionDialog(tableModel, owner, location, locationRelativeTo,
							title, icon, dialogSize, false).selectEntities();
		}

		@Override
		public Optional<Entity> selectSingle() {
			List<Entity> entities = new EntitySelectionDialog(tableModel, owner, location, locationRelativeTo,
							title, icon, dialogSize, true).selectEntities();

			return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
		}
	}

	private static final class EntitySelectionDialog {

		private final EntityTablePanel entityTablePanel;

		private EntitySelectionDialog(SwingEntityTableModel tableModel, Window owner, Point location, Component locationRelativeTo,
																	ValueObserver<String> title, ImageIcon icon, Dimension dialogSize, boolean singleSelection) {
			Control okControl = Control.builder()
							.command(this::ok)
							.name(Messages.ok())
							.mnemonic(Messages.okMnemonic())
							.enabled(tableModel.selectionModel().selectionNotEmpty())
							.build();
			Control cancelControl = Control.builder()
							.command(this::cancel)
							.name(Messages.cancel())
							.mnemonic(Messages.cancelMnemonic())
							.build();
			Control searchControl = Control.builder()
						.command(this::search)
						.name(FrameworkMessages.searchVerb())
						.mnemonic(FrameworkMessages.searchMnemonic())
						.build();
			entityTablePanel = createTablePanel(tableModel, okControl, singleSelection);
			actionDialog(borderLayoutPanel()
											.centerComponent(entityTablePanel)
											.border(Borders.emptyBorder())
											.build())
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.title(title)
							.icon(icon)
							.size(dialogSize)
							.defaultAction(okControl)
							.escapeAction(cancelControl)
							.action(searchControl)
							.show();
		}

		private static EntityTablePanel createTablePanel(SwingEntityTableModel tableModel, Control okControl, boolean singleSelection) {
			tableModel.editModel().readOnly().set(true);
			EntityTablePanel tablePanel = new EntityTablePanel(tableModel, config -> config.includeSouthPanel(false));
			tablePanel.initialize();
			tablePanel.table().doubleClickAction().set(okControl);
			tablePanel.conditionPanel().state().set(ConditionState.SIMPLE);
			tablePanel.table().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
			tablePanel.table().setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION);

			return tablePanel;
		}

		private void ok() {
			disposeParentWindow(entityTablePanel);
		}

		private void cancel() {
			entityTablePanel.tableModel().selectionModel().clearSelection();
			disposeParentWindow(entityTablePanel);
		}

		private void search() {
			SwingEntityTableModel tableModel = entityTablePanel.tableModel();
			tableModel.refreshThen(items -> {
				if (tableModel.rowCount() > 0) {
					tableModel.selectionModel().selectedIndex().set(0);
					entityTablePanel.table().requestFocusInWindow();
				}
				else {
					showMessageDialog(parentWindow(entityTablePanel), FrameworkMessages.noSearchResults());
				}
			});
		}

		private List<Entity> selectEntities() {
			return entityTablePanel.tableModel().selectionModel().selectedItems().get();
		}
	}

	private static final class DefaultAddEntityDialogBuilder extends AbstractDialogBuilder<AddEntityDialogBuilder>
					implements AddEntityDialogBuilder {

		private final Supplier<EntityEditPanel> editPanelSupplier;

		private Consumer<Entity> onInsert = emptyConsumer();
		private boolean closeDialog = true;
		private boolean confirm = false;

		private DefaultAddEntityDialogBuilder(Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = requireNonNull(editPanelSupplier);
		}

		@Override
		public AddEntityDialogBuilder onInsert(Consumer<Entity> onInsert) {
			this.onInsert = requireNonNull(onInsert);
			return this;
		}

		@Override
		public AddEntityDialogBuilder closeDialog(boolean closeDialog) {
			this.closeDialog = closeDialog;
			return this;
		}

		@Override
		public AddEntityDialogBuilder confirm(boolean confirm) {
			this.confirm = confirm;
			return this;
		}

		@Override
		public void show() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			SwingEntityEditModel editModel = editPanel.editModel();
			Runnable disposeDialog = new DisposeDialog(editPanel);
			actionDialog(borderLayoutPanel()
											.centerComponent(editPanel)
											.border(emptyBorder())
											.build())
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.defaultAction(createAddControl(editPanel,
											new InsertConsumer(disposeDialog), confirm))
							.escapeAction(createCancelControl(disposeDialog))
							.title(FrameworkMessages.add() + " - " + editModel.entities()
											.definition(editModel.entityType()).caption())
							.onShown(new ClearAndRequestFocus(editPanel))
							.show();
		}

		private final class InsertConsumer implements Consumer<Collection<Entity>> {

			private final Runnable disposeDialog;

			private InsertConsumer(Runnable disposeDialog) {
				this.disposeDialog = disposeDialog;
			}

			@Override
			public void accept(Collection<Entity> inserted) {
				onInsert.accept(inserted.iterator().next());
				if (closeDialog) {
					disposeDialog.run();
				}
			}
		}

		private static Control createAddControl(EntityEditPanel editPanel,
																						Consumer<Collection<Entity>> onInsert,
																						boolean confirm) {
			return Control.builder()
							.command(editPanel.insertCommand()
											.confirm(confirm)
											.onInsert(onInsert)
											.build())
							.name(FrameworkMessages.add())
							.mnemonic(FrameworkMessages.addMnemonic())
							.onException(new EditPanelExceptionHandler(editPanel))
							.build();
		}
	}

	private static final class DefaultEditEntityDialogBuilder extends AbstractDialogBuilder<EditEntityDialogBuilder>
					implements EditEntityDialogBuilder {

		private final Supplier<EntityEditPanel> editPanelSupplier;

		private Supplier<Entity> entity;
		private Consumer<Entity> onUpdate = emptyConsumer();
		private boolean confirm = false;

		private DefaultEditEntityDialogBuilder(Supplier<EntityEditPanel> editPanelSupplier) {
			this.editPanelSupplier = requireNonNull(editPanelSupplier);
		}

		@Override
		public EditEntityDialogBuilder entity(Supplier<Entity> entity) {
			this.entity = requireNonNull(entity);
			return this;
		}

		@Override
		public EditEntityDialogBuilder onUpdate(Consumer<Entity> onUpdate) {
			this.onUpdate = requireNonNull(onUpdate);
			return this;
		}

		@Override
		public EditEntityDialogBuilder confirm(boolean confirm) {
			this.confirm = confirm;
			return this;
		}

		@Override
		public void show() {
			EntityEditPanel editPanel = editPanelSupplier.get().initialize();
			SwingEntityEditModel editModel = editPanel.editModel();
			initializeEditModel(editModel);
			actionDialog(borderLayoutPanel()
											.centerComponent(editPanel)
											.border(emptyBorder())
											.build())
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.defaultAction(createUpdateControl(editPanel,
											new UpdateConsumer(new DisposeDialog(editPanel)), confirm))
							.escapeAction(createCancelControl(new RevertAndDisposeDialog(editPanel)))
							.title(FrameworkMessages.edit() + " - " + editModel.entities()
											.definition(editModel.entityType()).caption())
							.onShown(new RequestFocus(editPanel))
							.show();
		}

		private void initializeEditModel(SwingEntityEditModel editModel) {
			if (entity != null) {
				editModel.set(entity.get());
			}
			else {
				editModel.revert();
			}
		}

		private final class UpdateConsumer implements Consumer<Collection<Entity>> {

			private final Runnable disposeDialog;

			private UpdateConsumer(Runnable disposeDialog) {
				this.disposeDialog = disposeDialog;
			}

			@Override
			public void accept(Collection<Entity> updated) {
				onUpdate.accept(updated.iterator().next());
				disposeDialog.run();
			}
		}

		private static Control createUpdateControl(EntityEditPanel editPanel,
																							 Consumer<Collection<Entity>> onUpdate,
																							 boolean confirm) {
			return Control.builder()
							.command(editPanel.updateCommand()
											.confirm(confirm)
											.onUpdate(onUpdate)
											.build())
							.name(FrameworkMessages.update())
							.mnemonic(FrameworkMessages.updateMnemonic())
							.onException(new EditPanelExceptionHandler(editPanel))
							.enabled(editPanel.editModel().modified())
							.build();
		}
	}

	private static Control createCancelControl(Runnable disposeDialog) {
		return Control.builder()
						.command(new RunnableCommand(disposeDialog))
						.name(Messages.cancel())
						.mnemonic(Messages.cancelMnemonic())
						.build();
	}

	private static <T> Consumer<T> emptyConsumer() {
		return (Consumer<T>) EMPTY_CONSUMER;
	}

	private static final class EditPanelExceptionHandler implements Consumer<Exception> {

		private final EntityEditPanel editPanel;

		private EditPanelExceptionHandler(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void accept(Exception exception) {
			editPanel.onException(exception);
		}
	}

	private static final class ClearAndRequestFocus implements Consumer<JDialog> {

		private final EntityEditPanel editPanel;

		private ClearAndRequestFocus(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void accept(JDialog dialog) {
			editPanel.clearAndRequestFocus();
		}
	}

	private static final class RequestFocus implements Consumer<JDialog> {

		private final EntityEditPanel editPanel;

		private RequestFocus(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void accept(JDialog dialog) {
			editPanel.requestInitialFocus();
		}
	}

	private static final class RunnableCommand implements Control.Command {

		private final Runnable runnable;

		private RunnableCommand(Runnable runnable) {
			this.runnable = runnable;
		}

		@Override
		public void execute() throws Exception {
			runnable.run();
		}
	}

	private static final class DisposeDialog implements Runnable {

		private final EntityEditPanel editPanel;

		private DisposeDialog(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void run() {
			disposeParentWindow(editPanel);
		}
	}

	private static final class RevertAndDisposeDialog implements Runnable {

		private final EntityEditPanel editPanel;

		private RevertAndDisposeDialog(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void run() {
			editPanel.editModel().revert();
			disposeParentWindow(editPanel);
		}
	}
}
