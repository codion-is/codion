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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.observer.Observable;
import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.AbstractDialogBuilder;
import is.codion.swing.common.ui.dialog.ActionDialogBuilder;
import is.codion.swing.common.ui.dialog.DialogBuilder;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.component.DefaultEditComponent;
import is.codion.swing.framework.ui.component.EditComponent;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Point;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static javax.swing.JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

/**
 * Provides edit and selection dialogs for entities.
 */
public final class EntityDialogs {

	private static final Consumer<?> EMPTY_CONSUMER = value -> {};

	private EntityDialogs() {}

	/**
	 * @param editModel the edit model to use for creating component data models and applying the accepted value
	 * @param attribute the attribute to edit
	 * @param <T> the attribute type
	 * @return a new builder
	 * @see is.codion.framework.model.EntityEditModel#applyEdit(Collection, Attribute, Object)
	 */
	public static <T> EditAttributeDialogBuilder<T> editAttributeDialog(SwingEntityEditModel editModel, Attribute<T> attribute) {
		return new DefaultEditAttributeDialogBuilder<>(editModel, attribute);
	}

	/**
	 * Creates a new {@link AddEntityDialogBuilder} instance.
	 * @param editPanel the edit panel to use
	 * @return a new builder instance
	 */
	public static AddEntityDialogBuilder addEntityDialog(EntityEditPanel editPanel) {
		return new DefaultAddEntityDialogBuilder(editPanel);
	}

	/**
	 * Creates a new {@link EditEntityDialogBuilder} instance.
	 * @param editPanel the edit panel to use
	 * @return a new builder instance
	 */
	public static EditEntityDialogBuilder editEntityDialog(EntityEditPanel editPanel) {
		return new DefaultEditEntityDialogBuilder(editPanel);
	}

	/**
	 * <p>Creates a new {@link EntitySelectionDialogBuilder} instance for searching for and selecting one or more entities via a {@link EntityTablePanel}.
	 * <p>Note that calling this method configures actions and selection mode of the associated table panel.
	 * @param tablePanel the table panel to use
	 * @return a new builder instance
	 */
	public static EntitySelectionDialogBuilder selectionDialog(EntityTablePanel tablePanel) {
		return new DefaultEntitySelectionDialogBuilder(tablePanel);
	}

	/**
	 * Builds a dialog for editing single attributes for one or more entities
	 * @param <T> the attribute type
	 */
	public interface EditAttributeDialogBuilder<T> extends DialogBuilder<EditAttributeDialogBuilder<T>> {

		/**
		 * @param editComponent the edit component factory
		 * @return this builder
		 */
		EditAttributeDialogBuilder<T> editComponent(EditComponent<?, T> editComponent);

		/**
		 * <p>Provides the default value presented in the edit component.
		 * <p>By default, the default value is the current value of the attribute being edited,
		 * in the entities being edited, unless they contain multiple different values, then null is presented.
		 * @param defaultValue provides the default value to present in the editor component
		 * @return this builder
		 */
		EditAttributeDialogBuilder<T> defaultValue(Function<Collection<Entity>, T> defaultValue);

		/**
		 * Displays a dialog for editing a single attribute for the given entity
		 * @param entity the entity to edit
		 */
		void edit(Entity entity);

		/**
		 * Displays a dialog for editing a single attribute for the given entities
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
		 * @param onShown called each time the dialog is shown
		 * @return this builder instance
		 */
		AddEntityDialogBuilder onShown(Consumer<EntityEditPanel> onShown);

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
		 * @param onShown called each time the dialog is shown
		 * @return this builder instance
		 */
		EditEntityDialogBuilder onShown(Consumer<EntityEditPanel> onShown);

		/**
		 * Displays the dialog.
		 */
		void show();
	}

	/**
	 * A builder for a selection dialog based on an {@link EntityTablePanel}.
	 */
	public interface EntitySelectionDialogBuilder extends DialogBuilder<EntitySelectionDialogBuilder> {

		/**
		 * Defaults to false if no condition panel is available in the associated {@link EntityTablePanel}
		 * @param includeSearchButton true if a search button should be included
		 * @return this builder instance
		 */
		EntitySelectionDialogBuilder includeSearchButton(boolean includeSearchButton);

		/**
		 * @return a {@link SelectionStep}
		 */
		SelectionStep select();

		/**
		 * Provides selection for single or multiple entities.
		 */
		interface SelectionStep {

			/**
			 * Displays the {@link EntityTablePanel} for selecting one or more entities
			 * @return a List containing the selected entities or an empty list in case the selection was cancelled
			 * @throws CancelException in case the selection was cancelled
			 */
			List<Entity> multiple();

			/**
			 * Displays the {@link EntityTablePanel} for selecting a single entity
			 * @return the selected entity or {@link Optional#empty()}
			 * @throws CancelException in case the selection was cancelled
			 */
			Optional<Entity> single();
		}
	}

	private static final class DefaultEditAttributeDialogBuilder<T> extends AbstractDialogBuilder<EditAttributeDialogBuilder<T>>
					implements EditAttributeDialogBuilder<T> {

		private final SwingEntityEditModel editModel;
		private final Attribute<T> attribute;

		private EditComponent<?, T> editComponent;
		private Function<Collection<Entity>, T> defaultValue = new DefaultValue();

		private DefaultEditAttributeDialogBuilder(SwingEntityEditModel editModel, Attribute<T> attribute) {
			this.editModel = requireNonNull(editModel);
			this.attribute = requireNonNull(attribute);
			this.editComponent = new DefaultEditComponent<>(attribute);
		}

		@Override
		public EditAttributeDialogBuilder<T> editComponent(EditComponent<?, T> editComponent) {
			this.editComponent = requireNonNull(editComponent);
			return this;
		}

		@Override
		public EditAttributeDialogBuilder<T> defaultValue(Function<Collection<Entity>, T> defaultValue) {
			this.defaultValue = requireNonNull(defaultValue);
			return this;
		}

		@Override
		public void edit(Entity entity) {
			edit(singleton(requireNonNull(entity)));
		}

		@Override
		public void edit(Collection<Entity> entities) {
			Set<EntityType> entityTypes = requireNonNull(entities).stream()
							.map(Entity::type)
							.collect(toSet());
			if (entityTypes.isEmpty()) {
				return;//no entities
			}
			if (entityTypes.size() > 1) {
				throw new IllegalArgumentException("All entities must be of the same type when editing");
			}

			ComponentValue<?, T> componentValue = editComponent.component(editModel.editor());
			componentValue.set(defaultValue.apply(entities));
			EditAttributePanel<T> editPanel =
							new EditAttributePanel<>(editModel, entities, attribute, componentValue,
											editComponent.caption(editModel.entityDefinition()
															.attributes().definition(attribute)).orElse(null));
			Dialogs.okCancel()
							.component(editPanel)
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.title(FrameworkMessages.edit())
							.okAction(editPanel.update())
							.cancelAction(editPanel.cancel())
							.show();
		}

		private final class DefaultValue implements Function<Collection<Entity>, T> {

			@Override
			public @Nullable T apply(Collection<Entity> entities) {
				Collection<@Nullable T> values = entities.stream()
								.map(entity -> entity.get(attribute))
								.collect(toSet());

				return values.size() == 1 ? values.iterator().next() : null;
			}
		}
	}

	private static final class DefaultEntitySelectionDialogBuilder extends AbstractDialogBuilder<EntitySelectionDialogBuilder>
					implements EntitySelectionDialogBuilder {

		private final EntityTablePanel tablePanel;

		private boolean includeSearchButton;

		private DefaultEntitySelectionDialogBuilder(EntityTablePanel tablePanel) {
			this.tablePanel = requireNonNull(tablePanel);
			try {
				tablePanel.condition();
				includeSearchButton = true;
			}
			catch (IllegalStateException e) {
				includeSearchButton = false;
			}
		}

		@Override
		public EntitySelectionDialogBuilder includeSearchButton(boolean includeSearchButton) {
			this.includeSearchButton = includeSearchButton;
			return this;
		}

		@Override
		public SelectionStep select() {
			return new DefaultSelectionStep();
		}

		private final class DefaultSelectionStep implements SelectionStep {

			@Override
			public List<Entity> multiple() {
				return new EntitySearchDialog(tablePanel, owner, location,
								locationRelativeTo, title, icon, false, includeSearchButton).selectedEntities();
			}

			@Override
			public Optional<Entity> single() {
				List<Entity> entities = new EntitySearchDialog(tablePanel, owner, location,
								locationRelativeTo, title, icon, true, includeSearchButton).selectedEntities();

				return entities.isEmpty() ? Optional.empty() : Optional.of(entities.get(0));
			}
		}
	}

	private static final class EntitySearchDialog {

		private final EntityTablePanel tablePanel;
		private final State cancelled = State.state();

		private EntitySearchDialog(EntityTablePanel tablePanel, @Nullable Window owner, @Nullable Point location,
															 @Nullable Component locationRelativeTo, @Nullable Observable<String> title, @Nullable ImageIcon icon,
															 boolean singleSelection, boolean includeSearchButton) {
			this.tablePanel = requireNonNull(tablePanel).initialize();
			Control okControl = Control.builder()
							.command(this::ok)
							.caption(Messages.ok())
							.mnemonic(Messages.okMnemonic())
							.enabled(tablePanel.tableModel().selection().empty().not())
							.build();
			configureTable(tablePanel.table(), okControl, singleSelection);
			ActionDialogBuilder<?> builder = Dialogs.action()
							.component(borderLayoutPanel()
											.center(tablePanel)
											.border(emptyBorder()))
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.title(title)
							.icon(icon)
							.defaultAction(okControl)
							.escapeAction(Control.builder()
											.command(this::cancel)
											.caption(Messages.cancel())
											.mnemonic(Messages.cancelMnemonic())
											.build());
			if (includeSearchButton) {
				builder.action(Control.builder()
								.command(this::search)
								.caption(FrameworkMessages.searchVerb())
								.mnemonic(FrameworkMessages.searchMnemonic())
								.build());
			}

			builder.show();
		}

		private static void configureTable(FilterTable<?, ?> table, Control okControl, boolean singleSelection) {
			table.doubleClick().set(okControl);
			table.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.put(KeyStroke.getKeyStroke(VK_ENTER, 0), "none");
			table.setSelectionMode(singleSelection ? SINGLE_SELECTION : MULTIPLE_INTERVAL_SELECTION);
		}

		private void ok() {
			Ancestor.window().of(tablePanel).dispose();
		}

		private void cancel() {
			tablePanel.tableModel().selection().clear();
			Ancestor.window().of(tablePanel).dispose();
			cancelled.set(true);
		}

		private void search() {
			SwingEntityTableModel tableModel = tablePanel.tableModel();
			tableModel.items().refresh(items -> {
				if (tableModel.items().included().size() > 0) {
					tableModel.selection().index().set(0);
					tablePanel.table().requestFocusInWindow();
				}
				else {
					showMessageDialog(Ancestor.window().of(tablePanel).get(), FrameworkMessages.noSearchResults());
				}
			});
		}

		private List<Entity> selectedEntities() {
			if (cancelled.is()) {
				throw new CancelException();
			}

			return tablePanel.tableModel().selection().items().get();
		}
	}

	private static final class DefaultAddEntityDialogBuilder extends AbstractDialogBuilder<AddEntityDialogBuilder>
					implements AddEntityDialogBuilder {

		private final EntityEditPanel editPanel;
		private final Collection<Consumer<EntityEditPanel>> onShownConsumers = new ArrayList<>(1);

		private Consumer<Entity> onInsert = emptyConsumer();
		private boolean closeDialog = true;
		private boolean confirm = false;

		private DefaultAddEntityDialogBuilder(EntityEditPanel editPanel) {
			this.editPanel = requireNonNull(editPanel);
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
		public AddEntityDialogBuilder onShown(Consumer<EntityEditPanel> onShown) {
			onShownConsumers.add(requireNonNull(onShown));
			return this;
		}

		@Override
		public void show() {
			SwingEntityEditModel editModel = editPanel.editModel();
			Runnable disposeDialog = new DisposeDialog(editPanel);
			Dialogs.action()
							.component(borderLayoutPanel()
											.center(editPanel.initialize())
											.border(emptyBorder()))
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.defaultAction(createAddControl(editPanel,
											new OnInsert(disposeDialog), confirm))
							.escapeAction(createCancelControl(disposeDialog))
							.title(FrameworkMessages.add() + " - " + editModel.entities()
											.definition(editModel.entityType()).caption())
							.onShown(new OnShown(editPanel, onShownConsumers))
							.show();
		}

		private final class OnInsert implements Consumer<Collection<Entity>> {

			private final Runnable disposeDialog;

			private OnInsert(Runnable disposeDialog) {
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
							.caption(FrameworkMessages.insert())
							.mnemonic(FrameworkMessages.insertMnemonic())
							.onException(new EditPanelExceptionHandler(editPanel))
							.build();
		}

		private static final class OnShown implements Consumer<JDialog> {

			private final EntityEditPanel editPanel;
			private final Collection<Consumer<EntityEditPanel>> onShownConsumers;

			private OnShown(EntityEditPanel editPanel, Collection<Consumer<EntityEditPanel>> onShownConsumers) {
				this.editPanel = editPanel;
				this.onShownConsumers = onShownConsumers;
			}

			@Override
			public void accept(JDialog dialog) {
				editPanel.clearAndRequestFocus();
				onShownConsumers.forEach(onShown -> onShown.accept(editPanel));
			}
		}
	}

	private static final class DefaultEditEntityDialogBuilder extends AbstractDialogBuilder<EditEntityDialogBuilder>
					implements EditEntityDialogBuilder {

		private final EntityEditPanel editPanel;
		private final Collection<Consumer<EntityEditPanel>> onShownConsumers = new ArrayList<>(1);

		private @Nullable Supplier<Entity> entity;
		private Consumer<Entity> onUpdate = emptyConsumer();
		private boolean confirm = false;

		private DefaultEditEntityDialogBuilder(EntityEditPanel editPanel) {
			this.editPanel = requireNonNull(editPanel);
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
		public EditEntityDialogBuilder onShown(Consumer<EntityEditPanel> onShown) {
			onShownConsumers.add(requireNonNull(onShown));
			return this;
		}

		@Override
		public void show() {
			SwingEntityEditModel editModel = editPanel.editModel();
			initializeEditModel(editModel);
			Dialogs.action()
							.component(borderLayoutPanel()
											.center(editPanel.initialize())
											.border(emptyBorder()))
							.owner(owner)
							.location(location)
							.locationRelativeTo(locationRelativeTo)
							.defaultAction(createUpdateControl(editPanel,
											new OnUpdate(new DisposeDialog(editPanel)), confirm))
							.escapeAction(createCancelControl(new RevertAndDisposeDialog(editPanel)))
							.title(FrameworkMessages.edit() + " - " + editModel.entities()
											.definition(editModel.entityType()).caption())
							.onShown(new OnShown(editPanel, onShownConsumers))
							.show();
		}

		private void initializeEditModel(SwingEntityEditModel editModel) {
			if (entity != null) {
				editModel.editor().set(entity.get());
			}
			else {
				editModel.editor().revert();
			}
		}

		private final class OnUpdate implements Consumer<Collection<Entity>> {

			private final Runnable disposeDialog;

			private OnUpdate(Runnable disposeDialog) {
				this.disposeDialog = disposeDialog;
			}

			@Override
			public void accept(Collection<Entity> updatedEntities) {
				onUpdate.accept(updatedEntities.iterator().next());
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
							.caption(FrameworkMessages.update())
							.mnemonic(FrameworkMessages.updateMnemonic())
							.onException(new EditPanelExceptionHandler(editPanel))
							.enabled(editPanel.editModel().editor().modified())
							.build();
		}

		private static final class OnShown implements Consumer<JDialog> {

			private final EntityEditPanel editPanel;
			private final Collection<Consumer<EntityEditPanel>> onShownConsumers;

			private OnShown(EntityEditPanel editPanel, Collection<Consumer<EntityEditPanel>> onShownConsumers) {
				this.editPanel = editPanel;
				this.onShownConsumers = onShownConsumers;
			}

			@Override
			public void accept(JDialog dialog) {
				editPanel.focus().initial().request();
				onShownConsumers.forEach(onShown -> onShown.accept(editPanel));
			}
		}
	}

	private static Control createCancelControl(Runnable disposeDialog) {
		return Control.builder()
						.command(new RunnableCommand(disposeDialog))
						.caption(Messages.cancel())
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
			Ancestor.window().of(editPanel).dispose();
		}
	}

	private static final class RevertAndDisposeDialog implements Runnable {

		private final EntityEditPanel editPanel;

		private RevertAndDisposeDialog(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
		}

		@Override
		public void run() {
			editPanel.editModel().editor().revert();
			Ancestor.window().of(editPanel).dispose();
		}
	}
}
