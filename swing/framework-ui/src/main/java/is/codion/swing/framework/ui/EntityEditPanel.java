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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.DeleteEntities;
import is.codion.framework.model.EntityEditModel.InsertEntities;
import is.codion.framework.model.EntityEditModel.UpdateEntities;
import is.codion.swing.common.ui.Cursors;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_V;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

	private static final MessageBundle MESSAGES =
					messageBundle(EntityEditPanel.class, getBundle(EntityEditPanel.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();
	private static final Consumer<?> EMPTY_CONSUMER = value -> {};

	/**
	 * The controls available for {@link EntityEditPanel}s.
	 * Note that changing the shortcut keystroke after the panel
	 * has been initialized has no effect.
	 */
	public static final class ControlKeys {

		/**
		 * Performs an insert.
		 */
		public static final ControlKey<CommandControl> INSERT = CommandControl.key("insert");
		/**
		 * Performs an update.
		 */
		public static final ControlKey<CommandControl> UPDATE = CommandControl.key("update");
		/**
		 * Performs a delete.
		 */
		public static final ControlKey<CommandControl> DELETE = CommandControl.key("delete");
		/**
		 * Clears the input fields.
		 */
		public static final ControlKey<CommandControl> CLEAR = CommandControl.key("clear");
		/**
		 * Displays a dialog for selecting an input field.<br>
		 * Default key stroke: CTRL-I
		 */
		public static final ControlKey<CommandControl> SELECT_INPUT_FIELD = CommandControl.key("selectInputField", keyStroke(VK_I, CTRL_DOWN_MASK));
		/**
		 * Displays the entity menu, if available.<br>
		 * Default key stroke: CTRL-ALT-V
		 * @see Config#INCLUDE_ENTITY_MENU
		 */
		public static final ControlKey<CommandControl> DISPLAY_ENTITY_MENU = CommandControl.key("displayEntityMenu", keyStroke(VK_V, CTRL_DOWN_MASK | ALT_DOWN_MASK));

		private ControlKeys() {}
	}

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
						.addPropertyChangeListener("focusOwner", new FocusActivationListener());
	}

	private static final String ALT_PREFIX = " (ALT-";

	/**
	 * The mechanism for restricting a single active EntityEditPanel at a time
	 */
	private static final State.Group ACTIVE_STATE_GROUP = State.group();

	private static final Consumer<Config> NO_CONFIGURATION = emptyConsumer();

	private final Controls.Layout controlsLayout;
	private final State active;

	final Config configuration;

	private boolean initialized = false;

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 */
	public EntityEditPanel(SwingEntityEditModel editModel) {
		this(editModel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 * @param config provides access to the panel configuration
	 */
	public EntityEditPanel(SwingEntityEditModel editModel, Consumer<Config> config) {
		super(editModel);
		this.configuration = configure(config);
		this.active = State.state(!configuration.focusActivation);
		this.controlsLayout = createControlsLayout();
		createControls();
		setupFocusActivation();
		setupKeyboardActions();
		if (editModel.editor().exists().not().get()) {
			editModel.editor().defaults();
		}
	}

	@Override
	public final String toString() {
		return editModel().toString();
	}

	/**
	 * @return a {@link State} controlling whether this panel is active, enabled and ready to receive input
	 */
	public final State active() {
		return active;
	}

	/**
	 * Clears the underlying edit model and requests the initial focus.
	 * @see EntityEditModel.EntityEditor#defaults()
	 * @see #focus()
	 */
	public final void clearAndRequestFocus() {
		editModel().editor().defaults();
		focus().initial().request();
	}

	/**
	 * <p>Displays a dialog allowing the user the select an input component which should receive the keyboard focus.
	 * <p>If only one input component is available that component is selected automatically without displaying a selection dialog.
	 * <p>If no component is available, f.ex. when the panel is not visible or none of the available components is focusable, this method does nothing.
	 * <p>Input components can be excluded from this selection using {@link Config#excludeFromSelection(Collection)}
	 * @see InputFocus#request(Attribute)
	 */
	public final void selectInputComponent() {
		Collection<Attribute<?>> attributes = selectComponentAttributes();
		if (attributes.size() == 1) {
			focus().request(attributes.iterator().next());
		}
		else if (!attributes.isEmpty()) {
			Entities entities = editModel().entities();
			List<AttributeDefinition<?>> sortedDefinitions = attributes.stream()
							.map(attribute -> entities.definition(attribute.entityType()).attributes().definition(attribute))
							.sorted(new AttributeDefinitionComparator())
							.collect(toList());
			Dialogs.selectionDialog(sortedDefinitions)
							.owner(this)
							.title(FrameworkMessages.selectInputField())
							.selectSingle()
							.ifPresent(attributeDefinition -> focus().request(attributeDefinition.attribute()));
		}
	}

	/**
	 * Initializes this EntityEditPanel.
	 * This method marks this panel as initialized which prevents it from running again,
	 * whether an exception occurs or not.
	 * @return this EntityEditPanel instance
	 */
	public final EntityEditPanel initialize() {
		if (!initialized) {
			try {
				setupControls();
				bindEvents();
				initializeUI();
			}
			finally {
				initialized = true;
			}
		}

		return this;
	}

	/**
	 * <p>Performs insert on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#insertConfirmer(Confirmer)}.
	 * <p>Note that the default insert {@link Confirmer} simply returns true, so in order to implement
	 * an insert confirmation you must set the {@link Confirmer} via {@link Config#insertConfirmer(Confirmer)}.
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true in case of successful insert, false otherwise
	 * @see Config#insertConfirmer(Confirmer)
	 */
	public final boolean insertWithConfirmation() {
		if (confirmInsert()) {
			return insert();
		}

		return false;
	}

	/**
	 * <p>Performs insert on the active entity without asking for confirmation.
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true in case of successful insert, false otherwise
	 */
	public final boolean insert() {
		try {
			editModel().insert();
			if (configuration.clearAfterInsert) {
				editModel().editor().defaults();
			}
			if (configuration.requestFocusAfterInsert) {
				focus().afterInsert().request();
			}

			return true;
		}
		catch (Exception exception) {
			handleException(exception);
		}

		return false;
	}

	/**
	 * <p>Performs delete on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#deleteConfirmer(Confirmer)}.
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true if the delete operation was successful
	 * @see Config#deleteConfirmer(Confirmer)
	 */
	public final boolean deleteWithConfirmation() {
		if (confirmDelete()) {
			return delete();
		}

		return false;
	}

	/**
	 * <p>Performs delete on the active entity without asking for confirmation
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true if the delete operation was successful
	 */
	public final boolean delete() {
		try {
			editModel().delete();
			focus().initial().request();

			return true;
		}
		catch (Exception exception) {
			handleException(exception);
		}

		return false;
	}

	/**
	 * <p>Performs update on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#updateConfirmer(Confirmer)}.
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true if the update operation was successful
	 * @see Config#updateConfirmer(Confirmer)
	 */
	public final boolean updateWithConfirmation() {
		if (confirmUpdate()) {
			return update();
		}

		return false;
	}

	/**
	 * <p>Performs update on the active entity without asking for confirmation.
	 * <p>This method executes synchronously on the Event Dispatch Thread.
	 * @return true if the update operation was successful
	 */
	public final boolean update() {
		try {
			editModel().update();
			focus().afterUpdate().request();

			return true;
		}
		catch (Exception exception) {
			handleException(exception);
		}

		return false;
	}

	/**
	 * @return true if this panel has been initialized
	 * @see #initialize()
	 */
	public final boolean initialized() {
		return initialized;
	}

	/**
	 * @return true if confirmed
	 */
	protected final boolean confirmInsert() {
		return configuration.insertConfirmer.confirm(this);
	}

	/**
	 * @return true if confirmed
	 */
	protected final boolean confirmUpdate() {
		return configuration.updateConfirmer.confirm(this);
	}

	/**
	 * @return true if confirmed
	 */
	protected final boolean confirmDelete() {
		return configuration.deleteConfirmer.confirm(this);
	}

	/**
	 * Returns an async insert command builder
	 * @return a new async insert command builder
	 */
	protected final InsertCommandBuilder insertCommand() {
		return new DefaultInsertCommandBuilder();
	}

	/**
	 * Returns an async update command builder
	 * @return a new async update command builder
	 */
	protected final UpdateCommandBuilder updateCommand() {
		return new DefaultUpdateCommandBuilder();
	}

	/**
	 * Returns an async delete command builder
	 * @return a new async delete command builder
	 */
	protected final DeleteCommandBuilder deleteCommand() {
		return new DefaultDeleteCommandBuilder();
	}

	/**
	 * Propagates the exception to {@link #onValidationException(ValidationException)} or
	 * {@link #onReferentialIntegrityException(ReferentialIntegrityException)} depending on type,
	 * otherwise forwards to the super implementation.
	 * @param exception the exception to handle
	 */
	@Override
	protected void onException(Exception exception) {
		if (exception instanceof ValidationException) {
			onValidationException((ValidationException) exception);
		}
		else if (exception instanceof ReferentialIntegrityException) {
			onReferentialIntegrityException((ReferentialIntegrityException) exception);
		}
		else {
			super.onException(exception);
		}
	}

	/**
	 * Called when a {@link ReferentialIntegrityException} occurs. If a {@link Operation#DELETE} operation is being
	 * performed and the referential integrity error handling is {@link ReferentialIntegrityErrorHandling#DISPLAY_DEPENDENCIES},
	 * the dependencies of the entity involved are displayed to the user, otherwise {@link #onException(Exception)} is called.
	 * @param exception the exception
	 * @see Config#referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
	 */
	protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
		requireNonNull(exception);
		if (exception.operation() == Operation.DELETE && configuration.referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
			displayDependencies();
		}
		else {
			super.onException(exception);
		}
	}

	/**
	 * Displays the exception message after which the component involved receives the focus.
	 * @param exception the exception
	 */
	protected void onValidationException(ValidationException exception) {
		requireNonNull(exception);
		String title = editModel().entities()
						.definition(exception.attribute().entityType())
						.attributes()
						.definition(exception.attribute())
						.caption();
		JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
		focus().request(exception.attribute());
	}

	/**
	 * Override to set up any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(ControlKey)
	 */
	protected void setupControls() {}

	/**
	 * Initializes this EntityEditPanel UI, that is, creates and lays out the components
	 * required for editing the underlying entity type.
	 * {@snippet :
	 *   protected void initializeUI() {
	 *      focus().initial().set(DomainModel.USER_NAME);
	 *
	 *      createTextField(DomainModel.USER_NAME);
	 *      createTextField(DomainModel.USER_ADDRESS);
	 *
	 *      setLayout(new GridLayout(2, 1, 5, 5));
	 *
	 *      addInputPanel(DomainModel.USER_NAME);
	 *      addInputPanel(DomainModel.USER_ADDRESS);
	 *   }
	 * }
	 */
	protected abstract void initializeUI();

	/**
	 * Configures the controls.
	 * <p>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * {@snippet :
	 *   configureControls(layout -> layout
	 *           .separator()
	 *           .control(createCustomControl()))
	 * }
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#INSERT ControlKeys#INSERT}
	 *   <li>{@link ControlKeys#UPDATE ControlKeys#UPDATE}
	 *   <li>{@link ControlKeys#DELETE ControlKeys#DELETE}
	 *   <li>{@link ControlKeys#CLEAR ControlKeys#CLEAR}
	 * </ul>
	 * @param controlsConfig provides access to the controls configuration
	 * @see Controls.Layout#clear()
	 */
	protected final void configureControls(Consumer<Controls.Layout> controlsConfig) {
		requireNonNull(controlsConfig).accept(controlsLayout);
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code controlKey},
	 * an empty {@link Value} if no such control is available.
	 * @param <T> the control type
	 * @param controlKey the control key
	 * @return the {@link Value} containing the control associated with {@code controlKey}
	 */
	protected final <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		return configuration.controlMap.control(requireNonNull(controlKey));
	}

	/**
	 * Returns a {@link Controls} instance containing all the controls configured via {@link #configureControls(Consumer)}.
	 * @return the {@link Controls} provided by this edit panel
	 * @throws IllegalStateException in case the panel has not been initialized
	 * @see #initialized()
	 * @see #configureControls(Consumer)
	 */
	protected final Controls controls() {
		if (!initialized()) {
			throw new IllegalStateException("Method must be called after the panel is initialized");
		}

		return controlsLayout.create(configuration.controlMap);
	}

	private void createControls() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel is initialized");
			}
		};
		ControlMap controlMap = configuration.controlMap;
		controlMap.controls().forEach(control -> control.addValidator(controlValueValidator));
		if (!editModel().readOnly().get()) {
			if (editModel().insertEnabled().get()) {
				controlMap.control(INSERT).set(createInsertControl());
			}
			if (editModel().updateEnabled().get()) {
				controlMap.control(UPDATE).set(createUpdateControl());
			}
			if (editModel().deleteEnabled().get()) {
				controlMap.control(DELETE).set(createDeleteControl());
			}
		}
		controlMap.control(CLEAR).set(createClearControl());
		controlMap.control(SELECT_INPUT_FIELD).set(createSelectInputComponentControl());
		if (configuration.includeEntityMenu) {
			controlMap.control(DISPLAY_ENTITY_MENU).set(createShowEntityMenuControl());
		}
	}

	private CommandControl createDeleteControl() {
		return Control.builder()
						.command(deleteCommand()
										.confirm(true)
										.build())
						.name(FrameworkMessages.delete())
						.enabled(State.and(active,
										editModel().deleteEnabled(),
										editModel().editor().exists()))
						.description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
						.mnemonic(FrameworkMessages.deleteMnemonic())
						.smallIcon(ICONS.delete())
						.onException(this::onException)
						.build();
	}

	private CommandControl createClearControl() {
		return Control.builder()
						.command(this::clearAndRequestFocus)
						.name(Messages.clear())
						.enabled(active)
						.description(Messages.clearTip() + ALT_PREFIX + Messages.clearMnemonic() + ")")
						.mnemonic(Messages.clearMnemonic())
						.smallIcon(ICONS.clear())
						.build();
	}

	private CommandControl createSelectInputComponentControl() {
		return command(this::selectInputComponent);
	}

	private CommandControl createShowEntityMenuControl() {
		return command(this::showEntityMenu);
	}

	private CommandControl createUpdateControl() {
		return Control.builder()
						.command(updateCommand()
										.confirm(true)
										.build())
						.name(FrameworkMessages.update())
						.enabled(State.and(active,
										editModel().updateEnabled(),
										editModel().editor().modified()))
						.description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
						.mnemonic(FrameworkMessages.updateMnemonic())
						.smallIcon(ICONS.update())
						.onException(this::onException)
						.build();
	}

	private CommandControl createInsertControl() {
		boolean useSaveCaption = EntityEditPanel.Config.USE_SAVE_CAPTION.getOrThrow();
		char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.insertMnemonic();
		String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.insert();
		return Control.builder()
						.command(insertCommand()
										.confirm(true)
										.build())
						.name(caption)
						.enabled(State.and(active, editModel().insertEnabled()))
						.description(FrameworkMessages.insertTip() + ALT_PREFIX + mnemonic + ")")
						.mnemonic(mnemonic)
						.smallIcon(ICONS.add())
						.onException(this::onException)
						.build();
	}

	private void bindEvents() {
		editModel().editor().changing().addConsumer(this::beforeEntity);
	}

	private Collection<Attribute<?>> selectComponentAttributes() {
		return components().keySet().stream()
						.filter(attribute -> !configuration.excludeFromSelection.contains(attribute))
						.filter(attribute -> componentSelectable(component(attribute).get()))
						.collect(collectingAndThen(toList(), Collections::unmodifiableCollection));
	}

	private void beforeEntity(Entity entity) {
		if (configuration.modifiedWarning
						&& editModel().editor().modified().get()
						&& !Objects.equals(editModel().editor(), entity)
						&& showConfirmDialog(this,
						FrameworkMessages.modifiedWarning(), FrameworkMessages.modifiedWarningTitle(),
						JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
			throw new CancelException();
		}
	}

	private void setupFocusActivation() {
		if (configuration.focusActivation) {
			ACTIVE_STATE_GROUP.add(active);
		}
	}

	private void setupKeyboardActions() {
		configuration.controlMap.keyEvent(DISPLAY_ENTITY_MENU).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_INPUT_FIELD).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
	}

	private void showEntityMenu() {
		new EntityPopupMenu(editModel().editor().get(), editModel().connection()).show(this, 0, 0);
	}

	private void displayDependencies() {
		Map<EntityType, Collection<Entity>> dependencies = entityDependencies();
		if (dependencies.isEmpty()) {
			JOptionPane.showMessageDialog(this, MESSAGES.getString("unknown_dependent_records"),
							MESSAGES.getString("no_dependencies_title"), JOptionPane.INFORMATION_MESSAGE);
		}
		else {
			EntityDependenciesPanel dependenciesPanel = new EntityDependenciesPanel(dependencies, editModel().connectionProvider());
			int gap = Layouts.GAP.getOrThrow();
			dependenciesPanel.setBorder(createEmptyBorder(0, gap, 0, gap));
			Dialogs.componentDialog(dependenciesPanel)
							.owner(this)
							.modal(false)
							.title(FrameworkMessages.dependencies())
							.onShown(dialog -> dependenciesPanel.requestSelectedTableFocus())
							.show();
		}
	}

	private Map<EntityType, Collection<Entity>> entityDependencies() {
		setCursor(Cursors.WAIT);
		try {
			return editModel().connectionProvider().connection().dependencies(singletonList(editModel().editor().get()));
		}
		catch (DatabaseException e) {
			displayException(e);

			return emptyMap();
		}
		finally {
			setCursor(Cursors.DEFAULT);
		}
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private void handleException(Exception exception) {
		if (exception instanceof ValidationException || exception instanceof ReferentialIntegrityException) {
			LOG.debug(exception.getMessage(), exception);
		}
		else {
			LOG.error(exception.getMessage(), exception);
		}
		onException(exception);
	}

	private static boolean componentSelectable(JComponent component) {
		return component != null &&
						component.isDisplayable() &&
						component.isVisible() &&
						component.isEnabled() &&
						focusable(component);
	}

	private static boolean focusable(JComponent component) {
		if (component instanceof JSpinner) {
			return ((JSpinner.DefaultEditor) ((JSpinner) component).getEditor()).getTextField().isFocusable();
		}

		return component.isFocusable();
	}

	private static Controls.Layout createControlsLayout() {
		return Controls.layout(asList(
						INSERT,
						UPDATE,
						DELETE,
						CLEAR
		));
	}

	/**
	 * Contains configuration settings for a {@link EntityEditPanel} which must be set before the panel is initialized.
	 */
	public static final class Config {

		/**
		 * Specifies whether the add/insert button caption should be 'Save' (mnemonic S), instead of 'Add' (mnemonic A)
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> USE_SAVE_CAPTION =
						booleanValue(EntityEditPanel.class.getName() + ".useSaveCaption", false);

		/**
		 * Specifies whether to include a {@link EntityPopupMenu} on this edit panel, triggered with CTRL-ALT-V by default.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
						booleanValue(EntityEditPanel.class.getName() + ".includeEntityMenu", true);

		/**
		 * Specifies whether edit panels should be activated when the panel (or its parent EntityPanel) receives focus
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> USE_FOCUS_ACTIVATION =
						booleanValue(EntityEditPanel.class.getName() + ".useFocusActivation", true);

		/**
		 * Indicates whether the panel should ask for confirmation before discarding unsaved modifications
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> MODIFIED_WARNING =
						booleanValue(EntityEditPanel.class.getName() + ".modifiedWarning", false);

		private static final Confirmer DEFAULT_INSERT_CONFIRMER = Confirmer.NONE;
		private static final Confirmer DEFAULT_UPDATE_CONFIRMER = new UpdateConfirmer();
		private static final Confirmer DEFAULT_DELETE_CONFIRMER = new DeleteConfirmer();

		private final EntityEditPanel editPanel;

		private boolean clearAfterInsert = true;
		private boolean requestFocusAfterInsert = true;
		private boolean focusActivation = USE_FOCUS_ACTIVATION.getOrThrow();
		private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.getOrThrow();
		private boolean modifiedWarning = MODIFIED_WARNING.getOrThrow();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
						ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
		private Confirmer insertConfirmer = DEFAULT_INSERT_CONFIRMER;
		private Confirmer deleteConfirmer = DEFAULT_DELETE_CONFIRMER;
		private Confirmer updateConfirmer = DEFAULT_UPDATE_CONFIRMER;
		private Set<Attribute<?>> excludeFromSelection = emptySet();

		final ControlMap controlMap;

		private Config(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
			this.controlMap = controlMap(ControlKeys.class);
		}

		private Config(Config config) {
			this.editPanel = config.editPanel;
			this.controlMap = config.controlMap.copy();
			this.clearAfterInsert = config.clearAfterInsert;
			this.requestFocusAfterInsert = config.requestFocusAfterInsert;
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.focusActivation = config.focusActivation;
			this.insertConfirmer = config.insertConfirmer;
			this.updateConfirmer = config.updateConfirmer;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeEntityMenu = config.includeEntityMenu;
			this.modifiedWarning = config.modifiedWarning;
			this.excludeFromSelection = unmodifiableSet(new HashSet<>(config.excludeFromSelection));
		}

		/**
		 * @return the edit panel
		 */
		public EntityEditPanel editPanel() {
			return editPanel;
		}

		/**
		 * @param controlKey the control key
		 * @param keyStroke provides access to the {@link Value} controlling the keyStroke for the given control
		 * @return this Config instance
		 */
		public Config keyStroke(ControlKey<?> controlKey, Consumer<Value<KeyStroke>> keyStroke) {
			requireNonNull(keyStroke).accept(controlMap.keyStroke(controlKey));
			return this;
		}

		/**
		 * @param clearAfterInsert controls whether the UI should be cleared after insert has been performed
		 * @return this Config instance
		 */
		public Config clearAfterInsert(boolean clearAfterInsert) {
			this.clearAfterInsert = clearAfterInsert;
			return this;
		}

		/**
		 * @param requestFocusAfterInsert controls whether the UI should request focus after insert has been performed
		 * @return this Config instance
		 * @see InputFocus#afterInsert()
		 */
		public Config requestFocusAfterInsert(boolean requestFocusAfterInsert) {
			this.requestFocusAfterInsert = requestFocusAfterInsert;
			return this;
		}

		/**
		 * @param referentialIntegrityErrorHandling controls which action to take on a referential integrity error on delete
		 * @return this Config instance
		 */
		public Config referentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
			this.referentialIntegrityErrorHandling = requireNonNull(referentialIntegrityErrorHandling);
			return this;
		}

		/**
		 * @param focusActivation true if the edit panel should be activated when it or its parent EntityPanel receives focus
		 * @return this Config instance
		 * @see #USE_FOCUS_ACTIVATION
		 */
		public Config focusActivation(boolean focusActivation) {
			this.focusActivation = focusActivation;
			return this;
		}

		/**
		 * @param includeEntityMenu true if an entity menu should be included
		 * @return this Config instance
		 * @see #INCLUDE_ENTITY_MENU
		 */
		public Config includeEntityMenu(boolean includeEntityMenu) {
			this.includeEntityMenu = includeEntityMenu;
			return this;
		}

		/**
		 * @param modifiedWarning specifies whether this edit panel presents a warning before discarding unsaved modifications
		 * @return this Config instance
		 * @see #MODIFIED_WARNING
		 * @see EntityEditModel.EntityEditor#modified()
		 */
		public Config modifiedWarning(boolean modifiedWarning) {
			this.modifiedWarning = modifiedWarning;
			return this;
		}

		/**
		 * @param insertConfirmer the insert confirmer
		 * @return this Config instance
		 */
		public Config insertConfirmer(Confirmer insertConfirmer) {
			this.insertConfirmer = requireNonNull(insertConfirmer);
			return this;
		}

		/**
		 * @param deleteConfirmer the delete confirmer
		 * @return this Config instance
		 */
		public Config deleteConfirmer(Confirmer deleteConfirmer) {
			this.deleteConfirmer = requireNonNull(deleteConfirmer);
			return this;
		}

		/**
		 * @param updateConfirmer the update confirmer
		 * @return this Config instance
		 */
		public Config updateConfirmer(Confirmer updateConfirmer) {
			this.updateConfirmer = requireNonNull(updateConfirmer);
			return this;
		}

		/**
		 * Specifies the attributes that should be excluded when presenting a component selection list.
		 * @param excludeFromSelection a {@link Collection} containing the attributes to exclude from component selection
		 * @return this Config instance
		 * @throws IllegalArgumentException in case an attribute is not found in the underlying entity
		 * @see #selectInputComponent()
		 */
		public Config excludeFromSelection(Collection<Attribute<?>> excludeFromSelection) {
			this.excludeFromSelection = unmodifiableSet(new HashSet<>(excludeFromSelection));
			return this;
		}
	}

	/**
	 * Handles displaying confirmation messages for common actions to the user.
	 * @see #NONE
	 */
	public interface Confirmer {

		/**
		 * A convenience instance indicating no confirmation is needed.
		 */
		Confirmer NONE = dialogOwner -> true;

		/**
		 * Returns true if the action is confirmed, presents a confirmation dialog to the user if required.
		 * @param dialogOwner the owner for the dialog
		 * @return true if the action is confirmed
		 */
		boolean confirm(JComponent dialogOwner);

		/**
		 * Shows a confirmation dialog
		 * @param dialogOwner the dialog owner
		 * @param message the dialog message
		 * @param title the dialog title
		 * @return true if the action is confirmed
		 */
		default boolean confirm(JComponent dialogOwner, String message, String title) {
			return showConfirmDialog(dialogOwner, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
		}
	}

	/**
	 * Builds an async insert command
	 */
	public interface InsertCommandBuilder {

		/**
		 * @param confirm true if confirmation should be performed, default false
		 * @return this builder instance
		 */
		InsertCommandBuilder confirm(boolean confirm);

		/**
		 * @param onInsert called after a successful insert
		 * @return this builder instance
		 */
		InsertCommandBuilder onInsert(Consumer<Collection<Entity>> onInsert);

		/**
		 * @return the command
		 */
		Command build();
	}

	/**
	 * Builds an async update command
	 */
	public interface UpdateCommandBuilder {

		/**
		 * @param confirm true if confirmation should be performed, default true
		 * @return this builder instance
		 */
		UpdateCommandBuilder confirm(boolean confirm);

		/**
		 * @param onUpdate called after a successful update
		 * @return this builder instance
		 */
		UpdateCommandBuilder onUpdate(Consumer<Collection<Entity>> onUpdate);

		/**
		 * @return the command
		 */
		Command build();
	}

	/**
	 * Builds an async delete command
	 */
	public interface DeleteCommandBuilder {

		/**
		 * @param confirm true if confirmation should be performed, default true
		 * @return this builder instance
		 */
		DeleteCommandBuilder confirm(boolean confirm);

		/**
		 * @param onDelete called after a successful delete
		 * @return this builder instance
		 */
		DeleteCommandBuilder onDelete(Consumer<Collection<Entity>> onDelete);

		/**
		 * @return the command
		 */
		Command build();
	}

	private final class InsertCommand implements Command {

		private final boolean confirm;
		private final Consumer<Collection<Entity>> onInsert;

		private InsertCommand(DefaultInsertCommandBuilder builder) {
			this.confirm = builder.confirm;
			this.onInsert = builder.onInsert;
		}

		@Override
		public void execute() {
			if (!confirm || confirmInsert()) {
				progressWorkerDialog(editModel().createInsert().prepare()::perform)
								.title(MESSAGES.getString("inserting"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(EntityEditPanel.this::handleException)
								.execute();
			}
		}

		private void handleResult(InsertEntities.Result result) {
			onInsert.accept(result.handle());
			if (configuration.clearAfterInsert) {
				editModel().editor().defaults();
			}
			if (configuration.requestFocusAfterInsert) {
				focus().afterInsert().request();
			}
		}
	}

	private final class UpdateCommand implements Command {

		private final boolean confirm;
		private final Consumer<Collection<Entity>> onUpdate;

		private UpdateCommand(DefaultUpdateCommandBuilder builder) {
			this.confirm = builder.confirm;
			this.onUpdate = builder.onUpdate;
		}

		@Override
		public void execute() {
			if (!confirm || confirmUpdate()) {
				progressWorkerDialog(editModel().createUpdate().prepare()::perform)
								.title(MESSAGES.getString("updating"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(EntityEditPanel.this::handleException)
								.execute();
			}
		}

		private void handleResult(UpdateEntities.Result result) {
			onUpdate.accept(result.handle());
			focus().afterUpdate().request();
		}
	}

	private final class DeleteCommand implements Command {

		private final boolean confirm;
		private final Consumer<Collection<Entity>> onDelete;

		private DeleteCommand(DefaultDeleteCommandBuilder builder) {
			this.confirm = builder.confirm;
			this.onDelete = builder.onDelete;
		}

		@Override
		public void execute() {
			if (!confirm || confirmDelete()) {
				progressWorkerDialog(editModel().createDelete().prepare()::perform)
								.title(MESSAGES.getString("deleting"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(EntityEditPanel.this::handleException)
								.execute();
			}
		}

		private void handleResult(DeleteEntities.Result result) {
			onDelete.accept(result.handle());
			focus().initial().request();
		}
	}

	private final class DefaultInsertCommandBuilder implements InsertCommandBuilder {

		private boolean confirm;
		private Consumer<Collection<Entity>> onInsert = emptyConsumer();

		@Override
		public InsertCommandBuilder confirm(boolean confirm) {
			this.confirm = confirm;
			return this;
		}

		@Override
		public InsertCommandBuilder onInsert(Consumer<Collection<Entity>> onInsert) {
			this.onInsert = requireNonNull(onInsert);
			return this;
		}

		@Override
		public Command build() {
			return new InsertCommand(this);
		}
	}

	private final class DefaultUpdateCommandBuilder implements UpdateCommandBuilder {

		private boolean confirm = true;
		private Consumer<Collection<Entity>> onUpdate = emptyConsumer();

		@Override
		public UpdateCommandBuilder confirm(boolean confirm) {
			this.confirm = confirm;
			return this;
		}

		@Override
		public UpdateCommandBuilder onUpdate(Consumer<Collection<Entity>> onUpdate) {
			this.onUpdate = requireNonNull(onUpdate);
			return this;
		}

		@Override
		public Command build() {
			return new UpdateCommand(this);
		}
	}

	private final class DefaultDeleteCommandBuilder implements DeleteCommandBuilder {

		private boolean confirm = true;
		private Consumer<Collection<Entity>> onDelete = emptyConsumer();

		@Override
		public DeleteCommandBuilder confirm(boolean confirm) {
			this.confirm = confirm;
			return this;
		}

		@Override
		public DeleteCommandBuilder onDelete(Consumer<Collection<Entity>> onDelete) {
			this.onDelete = requireNonNull(onDelete);
			return this;
		}

		@Override
		public Command build() {
			return new DeleteCommand(this);
		}
	}

	private static <T> Consumer<T> emptyConsumer() {
		return (Consumer<T>) EMPTY_CONSUMER;
	}

	private static final class FocusActivationListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent changeEvent) {
			Component focusedComponent = (Component) changeEvent.getNewValue();
			if (focusedComponent != null) {
				EntityEditPanel editPanel = null;
				EntityPanel entityPanel = entityPanel(focusedComponent);
				if (entityPanel != null) {
					if (entityPanel.containsEditPanel()) {
						editPanel = entityPanel.editPanel();
					}
				}
				else {
					editPanel = parentOfType(EntityEditPanel.class, focusedComponent);
				}
				if (editPanel != null && editPanel.configuration.focusActivation) {
					editPanel.active.set(true);
				}
			}
		}

		private static EntityPanel entityPanel(Component focusedComponent) {
			if (focusedComponent instanceof JTabbedPane) {
				Component selectedComponent = ((JTabbedPane) focusedComponent).getSelectedComponent();
				if (selectedComponent instanceof EntityPanel) {
					return (EntityPanel) selectedComponent;
				}
			}

			return parentOfType(EntityPanel.class, focusedComponent);
		}
	}

	private static final class UpdateConfirmer implements Confirmer {

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return confirm(dialogOwner, FrameworkMessages.confirmUpdate(), FrameworkMessages.update());
		}
	}

	private static final class DeleteConfirmer implements Confirmer {

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return confirm(dialogOwner, FrameworkMessages.confirmDelete(1), FrameworkMessages.delete());
		}
	}
}
