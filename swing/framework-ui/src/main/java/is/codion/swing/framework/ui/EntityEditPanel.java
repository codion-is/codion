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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.resources.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.Delete;
import is.codion.framework.model.EntityEditModel.Insert;
import is.codion.framework.model.EntityEditModel.Update;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static is.codion.common.resources.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static is.codion.swing.framework.ui.EntityEditPanel.EntityEditPanelControl.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_I;
import static java.awt.event.KeyEvent.VK_V;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toMap;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

	private static final MessageBundle MESSAGES =
					messageBundle(EntityEditPanel.class, getBundle(EntityEditPanel.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * The controls available for {@link EntityEditPanel}s.
	 * Note that changing the shortcut keystroke after the panel
	 * has been initialized has no effect.
	 */
	public enum EntityEditPanelControl implements KeyboardShortcuts.Shortcut {
		/**
		 * Performs an insert.
		 */
		INSERT,
		/**
		 * Performs an update.
		 */
		UPDATE,
		/**
		 * Performs a delete.
		 */
		DELETE,
		/**
		 * Clears the input fields.
		 */
		CLEAR,
		/**
		 * Displays a dialog for selecting an input field.<br>
		 * Default key stroke: CTRL-I
		 */
		SELECT_INPUT_FIELD(keyStroke(VK_I, CTRL_DOWN_MASK)),
		/**
		 * Displays the entity menu, if available.<br>
		 * Default key stroke: CTRL-ALT-V
		 * @see Config#INCLUDE_ENTITY_MENU
		 */
		DISPLAY_ENTITY_MENU(keyStroke(VK_V, CTRL_DOWN_MASK | ALT_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		EntityEditPanelControl() {
			this(null);
		}

		EntityEditPanelControl(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public Optional<KeyStroke> defaultKeystroke() {
			return Optional.ofNullable(defaultKeystroke);
		}
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

	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	private final Config configuration;
	private final Controls.Config<EntityEditPanelControl> controlsConfiguration;
	private final Map<EntityEditPanelControl, Value<Control>> controls;
	private final State active;

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
	 * @param entityComponents the entity components instance to use when creating components
	 */
	public EntityEditPanel(SwingEntityEditModel editModel, EntityComponents entityComponents) {
		this(editModel, entityComponents, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 * @param configuration provides access to the panel configuration
	 */
	public EntityEditPanel(SwingEntityEditModel editModel, Consumer<Config> configuration) {
		this(editModel, new EntityComponents(editModel.entityDefinition()), configuration);
	}

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 * @param entityComponents the entity components instance to use when creating components
	 * @param configuration provides access to the panel configuration
	 */
	public EntityEditPanel(SwingEntityEditModel editModel, EntityComponents entityComponents, Consumer<Config> configuration) {
		super(editModel, entityComponents);
		this.configuration = configure(configuration);
		this.controlsConfiguration = createControlsConfiguration();
		this.active = State.state(!this.configuration.focusActivation);
		this.controls = createControlsMap();
		setupFocusActivation();
		setupKeyboardActions();
		if (editModel.exists().not().get()) {
			editModel.defaults();
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
	 * @see EntityEditModel#defaults()
	 * @see #requestInitialFocus()
	 */
	public final void clearAndRequestFocus() {
		editModel().defaults();
		requestInitialFocus();
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code control},
	 * an empty {@link Value} if no such control is available.
	 * Note that standard controls are populated during initialization, so until then, these values may be empty.
	 * @param control the control
	 * @return the {@link Value} containing the control associated with {@code control}
	 */
	public final Value<Control> control(EntityEditPanelControl control) {
		return controls.get(requireNonNull(control));
	}

	/**
	 * Returns a {@link Controls} instance containing all the controls configured via {@link #configureControls(Consumer)}.
	 * @return the {@link Controls} provided by this edit panel
	 * @throws IllegalStateException in case the panel has not been initialized
	 * @see #initialized()
	 * @see #configureControls(Consumer)
	 */
	public final Controls controls() {
		if (!initialized()) {
			throw new IllegalStateException("Method must be called after the panel is initialized");
		}

		return controlsConfiguration.create();
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
				setupStandardControls();
				configureControls();
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
	 * Performs insert on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#insertConfirmer(Confirmer)}.
	 * Note that the default insert {@link Confirmer} simply returns true, so in order to implement
	 * a insert confirmation you must set the {@link Confirmer} via {@link Config#insertConfirmer(Confirmer)}.
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
	 * Performs insert on the active entity without asking for confirmation
	 * @return true in case of successful insert, false otherwise
	 */
	public final boolean insert() {
		try {
			editModel().insert();
			if (configuration.clearAfterInsert) {
				editModel().defaults();
			}
			if (configuration.requestFocusAfterInsert) {
				requestAfterInsertFocus();
			}

			return true;
		}
		catch (ValidationException e) {
			LOG.debug(e.getMessage(), e);
			onException(e);
		}
		catch (Exception e) {
			LOG.error(e.getMessage(), e);
			onException(e);
		}

		return false;
	}

	/**
	 * Performs delete on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#deleteConfirmer(Confirmer)}.
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
	 * Performs delete on the active entity without asking for confirmation
	 * @return true if the delete operation was successful
	 */
	public final boolean delete() {
		try {
			editModel().delete();
			requestInitialFocus();

			return true;
		}
		catch (ReferentialIntegrityException e) {
			LOG.debug(e.getMessage(), e);
			onException(e);
		}
		catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			onException(ex);
		}

		return false;
	}

	/**
	 * Performs update on the active entity after asking for confirmation using the {@link Confirmer}
	 * specified via {@link Config#updateConfirmer(Confirmer)}.
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
	 * Performs update on the active entity without asking for confirmation.
	 * @return true if the update operation was successful
	 */
	public final boolean update() {
		try {
			editModel().update();
			requestAfterUpdateFocus();

			return true;
		}
		catch (ValidationException e) {
			LOG.debug(e.getMessage(), e);
			onException(e);
		}
		catch (Exception ex) {
			LOG.error(ex.getMessage(), ex);
			onException(ex);
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
	 * Returns an async insert command
	 * @param confirm true if confirmation should be performed
	 * @return a new async insert command
	 */
	protected final Control.Command insertCommand(boolean confirm) {
		return new InsertCommand(confirm);
	}

	/**
	 * Returns an async update command
	 * @param confirm true if confirmation should be performed
	 * @return a new async update command
	 */
	protected final Control.Command updateCommand(boolean confirm) {
		return new UpdateCommand(confirm);
	}

	/**
	 * Returns an async delete command
	 * @param confirm true if confirmation should be performed
	 * @return a new async delete command
	 */
	protected final Control.Command deleteCommand(boolean confirm) {
		return new DeleteCommand(confirm);
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
			displayDependenciesDialog(singletonList(editModel().entity()), editModel().connectionProvider(),
							this, MESSAGES.getString("unknown_dependent_records"));
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
		requestComponentFocus(exception.attribute());
	}

	/**
	 * Override to configure any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(EntityEditPanelControl)
	 */
	protected void configureControls() {}

	/**
	 * Initializes this EntityEditPanel UI, that is, creates and lays out the components
	 * required for editing the underlying entity type.
	 * <pre>
	 *   protected void initializeUI() {
	 *      initialFocusAttribute().set(DomainModel.USER_NAME);
	 *
	 *      createTextField(DomainModel.USER_NAME);
	 *      createTextField(DomainModel.USER_ADDRESS);
	 *
	 *      setLayout(new GridLayout(2, 1, 5, 5);
	 *
	 *      addInputPanel(DomainModel.USER_NAME);
	 *      addInputPanel(DomainModel.USER_ADDRESS);
	 *  }
	 * </pre>
	 */
	protected abstract void initializeUI();

	/**
	 * Configures the popup menu controls.<br>
	 * Note that the {@link Controls.Config} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * <pre>
	 *   configureControls(config -> config
	 *           .separator()
	 *           .control(createCustomControl()))
	 * </pre>
	 * @param controlsConfig provides access to the controls configuration
	 * @see Controls.Config#clear()
	 */
	protected final void configureControls(Consumer<Controls.Config<EntityEditPanelControl>> controlsConfig) {
		requireNonNull(controlsConfig).accept(controlsConfiguration);
	}

	private void setupStandardControls() {
		if (!editModel().readOnly().get()) {
			setupEditControls();
		}
		controls.get(CLEAR).mapNull(this::createClearControl);
		controls.get(SELECT_INPUT_FIELD).mapNull(this::createSelectInputComponentControl);
		if (configuration.includeEntityMenu) {
			controls.get(DISPLAY_ENTITY_MENU).mapNull(this::createShowEntityMenuControl);
		}
	}

	private void setupEditControls() {
		if (editModel().insertEnabled().get()) {
			controls.get(INSERT).mapNull(this::createInsertControl);
		}
		if (editModel().updateEnabled().get()) {
			controls.get(UPDATE).mapNull(this::createUpdateControl);
		}
		if (editModel().deleteEnabled().get()) {
			controls.get(DELETE).mapNull(this::createDeleteControl);
		}
	}

	private Control createDeleteControl() {
		return Control.builder(deleteCommand(true))
						.name(FrameworkMessages.delete())
						.enabled(State.and(active,
										editModel().deleteEnabled(),
										editModel().exists()))
						.description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
						.mnemonic(FrameworkMessages.deleteMnemonic())
						.smallIcon(ICONS.delete())
						.onException(this::onException)
						.build();
	}

	private Control createClearControl() {
		return Control.builder(this::clearAndRequestFocus)
						.name(Messages.clear())
						.enabled(active)
						.description(Messages.clearTip() + ALT_PREFIX + Messages.clearMnemonic() + ")")
						.mnemonic(Messages.clearMnemonic())
						.smallIcon(ICONS.clear())
						.build();
	}

	private Control createSelectInputComponentControl() {
		return Control.control(this::selectInputComponent);
	}

	private Control createShowEntityMenuControl() {
		return Control.control(this::showEntityMenu);
	}

	private Control createUpdateControl() {
		return Control.builder(updateCommand(true))
						.name(FrameworkMessages.update())
						.enabled(State.and(active,
										editModel().updateEnabled(),
										editModel().exists(),
										editModel().modified()))
						.description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
						.mnemonic(FrameworkMessages.updateMnemonic())
						.smallIcon(ICONS.update())
						.onException(this::onException)
						.build();
	}

	private Control createInsertControl() {
		boolean useSaveCaption = EntityEditPanel.Config.USE_SAVE_CAPTION.get();
		char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.insertMnemonic();
		String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.insert();
		return Control.builder(insertCommand(true))
						.name(caption)
						.enabled(State.and(active, editModel().insertEnabled()))
						.description(FrameworkMessages.insertTip() + ALT_PREFIX + mnemonic + ")")
						.mnemonic(mnemonic)
						.smallIcon(ICONS.add())
						.onException(this::onException)
						.build();
	}

	private void bindEvents() {
		editModel().confirmOverwriteEvent().addConsumer(confirmationState -> {
			int result = showConfirmDialog(Utilities.parentWindow(EntityEditPanel.this),
							FrameworkMessages.unsavedDataWarning(), FrameworkMessages.unsavedDataWarningTitle(),
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			confirmationState.set(result == JOptionPane.YES_OPTION);
		});
	}

	private void setupFocusActivation() {
		if (configuration.focusActivation) {
			ACTIVE_STATE_GROUP.add(active);
		}
	}

	private void setupKeyboardActions() {
		configuration.shortcuts.keyStroke(DISPLAY_ENTITY_MENU).optional().ifPresent(keyStroke ->
						control(DISPLAY_ENTITY_MENU).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
		configuration.shortcuts.keyStroke(SELECT_INPUT_FIELD).optional().ifPresent(keyStroke ->
						control(SELECT_INPUT_FIELD).optional().ifPresent(control ->
										KeyEvents.builder(keyStroke)
														.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
														.action(control)
														.enable(this)));
	}

	private void showEntityMenu() {
		new EntityPopupMenu(editModel().entity(), editModel().connection()).show(this, 0, 0);
	}

	private Map<EntityEditPanelControl, Value<Control>> createControlsMap() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel has been initialized");
			}
		};

		return unmodifiableMap(Stream.of(EntityEditPanelControl.values())
						.collect(toMap(Function.identity(), controlCode -> Value.<Control>nullable()
										.validator(controlValueValidator)
										.build())));
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private Controls.Config<EntityEditPanelControl> createControlsConfiguration() {
		return Controls.config(identifier -> control(identifier).optional(), asList(
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
		 * Specifies whether the add/insert button caption should be 'Save' (mnemonic S), instead of 'Add' (mnemonic A)<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> USE_SAVE_CAPTION =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.useSaveCaption", false);

		/**
		 * Specifies whether to include a {@link EntityPopupMenu} on this edit panel, triggered with CTRL-ALT-V by default.<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.includeEntityMenu", true);

		/**
		 * Specifies whether edit panels should be activated when the panel (or its parent EntityPanel) receives focus<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> USE_FOCUS_ACTIVATION =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.useFocusActivation", true);

		/**
		 * The default keyboard shortcut keyStrokes.
		 */
		public static final KeyboardShortcuts<EntityEditPanelControl> KEYBOARD_SHORTCUTS = keyboardShortcuts(EntityEditPanelControl.class);

		private static final Confirmer DEFAULT_INSERT_CONFIRMER = Confirmer.NONE;
		private static final Confirmer DEFAULT_UPDATE_CONFIRMER = new UpdateConfirmer();
		private static final Confirmer DEFAULT_DELETE_CONFIRMER = new DeleteConfirmer();

		private final EntityEditPanel editPanel;
		private final KeyboardShortcuts<EntityEditPanelControl> shortcuts;

		private boolean clearAfterInsert = true;
		private boolean requestFocusAfterInsert = true;
		private boolean focusActivation = USE_FOCUS_ACTIVATION.get();
		private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.get();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
						ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
		private Confirmer insertConfirmer = DEFAULT_INSERT_CONFIRMER;
		private Confirmer deleteConfirmer = DEFAULT_DELETE_CONFIRMER;
		private Confirmer updateConfirmer = DEFAULT_UPDATE_CONFIRMER;

		private Config(EntityEditPanel editPanel) {
			this.editPanel = editPanel;
			this.shortcuts = KEYBOARD_SHORTCUTS.copy();
		}

		private Config(Config config) {
			this.editPanel = config.editPanel;
			this.shortcuts = config.shortcuts.copy();
			this.clearAfterInsert = config.clearAfterInsert;
			this.requestFocusAfterInsert = config.requestFocusAfterInsert;
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.focusActivation = config.focusActivation;
			this.insertConfirmer = config.insertConfirmer;
			this.updateConfirmer = config.updateConfirmer;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeEntityMenu = config.includeEntityMenu;
		}

		/**
		 * @return the edit panel
		 */
		public EntityEditPanel editPanel() {
			return editPanel;
		}

		/**
		 * @param shortcuts provides this panels {@link KeyboardShortcuts} instance.
		 * @return this Config instance
		 */
		public Config keyStrokes(Consumer<KeyboardShortcuts<EntityEditPanelControl>> shortcuts) {
			requireNonNull(shortcuts).accept(this.shortcuts);
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
		 * @see EntityEditComponentPanel#requestInitialFocus()
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
		 * @param includeEntityMenu true if a entity menu should be included
		 * @return this Config instance
		 * @see #INCLUDE_ENTITY_MENU
		 */
		public Config includeEntityMenu(boolean includeEntityMenu) {
			this.includeEntityMenu = includeEntityMenu;
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
		 * Returns true if the action is confirmed, presents an OK/Cancel confirm dialog to the user if required.
		 * @param dialogOwner the owner for the dialog
		 * @return true if the action is confirmed
		 */
		boolean confirm(JComponent dialogOwner);

		/**
		 * Shows a confirmation dialog
		 * @param dialogOwner the dialog owner
		 * @param message the dialog message
		 * @param title the dialog title
		 * @return true if OK was pressed
		 */
		default boolean confirm(JComponent dialogOwner, String message, String title) {
			return showConfirmDialog(dialogOwner, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
		}
	}

	private final class InsertCommand implements Control.Command {

		private final boolean confirm;

		private InsertCommand(boolean confirm) {
			this.confirm = confirm;
		}

		@Override
		public void execute() throws ValidationException {
			if (!confirm || confirmInsert()) {
				progressWorkerDialog(editModel().createInsert().prepare()::perform)
								.title(MESSAGES.getString("inserting"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(this::onException)
								.execute();
			}
		}

		private void handleResult(Insert.Result result) {
			result.handle();
			if (configuration.clearAfterInsert) {
				editModel().defaults();
			}
			if (configuration.requestFocusAfterInsert) {
				requestAfterInsertFocus();
			}
		}

		private void onException(Exception exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
	}

	private final class UpdateCommand implements Control.Command {

		private final boolean confirm;

		private UpdateCommand(boolean confirm) {
			this.confirm = confirm;
		}

		@Override
		public void execute() throws ValidationException {
			if (!confirm || confirmUpdate()) {
				progressWorkerDialog(editModel().createUpdate().prepare()::perform)
								.title(MESSAGES.getString("updating"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(this::onException)
								.execute();
			}
		}

		private void handleResult(Update.Result result) {
			result.handle();
			requestAfterUpdateFocus();
		}

		private void onException(Exception exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
	}

	private final class DeleteCommand implements Control.Command {

		private final boolean confirm;

		private DeleteCommand(boolean confirm) {
			this.confirm = confirm;
		}

		@Override
		public void execute() {
			if (!confirm || confirmDelete()) {
				progressWorkerDialog(editModel().createDelete().prepare()::perform)
								.title(MESSAGES.getString("deleting"))
								.owner(EntityEditPanel.this)
								.onResult(this::handleResult)
								.onException(this::onException)
								.execute();
			}
		}

		private void handleResult(Delete.Result result) {
			result.handle();
			requestInitialFocus();
		}

		private void onException(Exception exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
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
