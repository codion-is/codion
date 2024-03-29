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
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static is.codion.swing.common.ui.Utilities.parentOfType;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_V;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel {

	private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityEditPanel.class.getName());

	/**
	 * The standard controls available in a edit panel
	 */
	public enum EditControl {
		INSERT, UPDATE, DELETE, CLEAR
	}

	private static final String ALT_PREFIX = " (ALT-";

	/**
	 * The mechanism for restricting a single active EntityEditPanel at a time
	 */
	private static final State.Group ACTIVE_STATE_GROUP = State.group();

	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	private final Config configuration;
	private final Map<EditControl, Value<Control>> controls;
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
		this.active = State.state(!this.configuration.focusActivation);
		this.controls = createControlsMap();
		setupFocusActivation();
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
	 * Returns a {@link Value} containing the control associated with {@code controlCode},
	 * an empty {@link Value} if no such control is available.
	 * Note that standard controls are populated during initialization, so until then, these values may be empty.
	 * @param editControl the control code
	 * @return the {@link Value} containing the control associated with {@code controlCode}
	 */
	public final Value<Control> control(EditControl editControl) {
		return controls.get(requireNonNull(editControl));
	}

	/**
	 * Returns a {@link Controls} instance containing all the controls this edit panel provides via {@link #createControls()}.
	 * @return the {@link Controls} provided by this edit panel
	 * @throws IllegalStateException in case the panel has not been initialized
	 * @see #initialized()
	 * @see #createControls()
	 */
	public final Controls controls() {
		if (!initialized()) {
			throw new IllegalStateException("Method must be called after the panel is initialized");
		}

		return createControls();
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
	 * Propagates the exception to {@link #onValidationException(ValidationException)} or
	 * {@link #onReferentialIntegrityException(ReferentialIntegrityException)} depending on type,
	 * otherwise forwards to the super implementation.
	 * @param exception the exception to handle
	 */
	@Override
	protected void onException(Throwable exception) {
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
	 * the dependencies of the entity involved are displayed to the user, otherwise {@link #onException(Throwable)} is called.
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
						.definition(exception.attribute().entityType()).attributes()
						.definition(exception.attribute())
						.caption();
		JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
		requestComponentFocus(exception.attribute());
	}

	/**
	 * Creates a Controls instance containing all the controls available in this edit panel
	 * @return the Controls available in this edit panel
	 */
	protected Controls createControls() {
		return Controls.controls(Stream.of(EditControl.values())
						.map(controls::get)
						.map(Value::optional)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.toArray(Control[]::new));
	}

	/**
	 * Override to setup any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(EditControl)
	 */
	protected void setupControls() {}

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

	private void setupStandardControls() {
		if (!editModel().readOnly().get()) {
			setupEditControls();
		}
		if (configuration.editControls.contains(EditControl.CLEAR)) {
			controls.get(EditControl.CLEAR).mapNull(this::createClearControl);
		}
	}

	private void setupEditControls() {
		if (editModel().insertEnabled().get() && configuration.editControls.contains(EditControl.INSERT)) {
			controls.get(EditControl.INSERT).mapNull(this::createInsertControl);
		}
		if (editModel().updateEnabled().get() && configuration.editControls.contains(EditControl.UPDATE)) {
			controls.get(EditControl.UPDATE).mapNull(this::createUpdateControl);
		}
		if (editModel().deleteEnabled().get() && configuration.editControls.contains(EditControl.DELETE)) {
			controls.get(EditControl.DELETE).mapNull(this::createDeleteControl);
		}
	}

	private Control createDeleteControl() {
		return Control.builder(new DeleteCommand())
						.name(FrameworkMessages.delete())
						.enabled(State.and(active,
										editModel().deleteEnabled(),
										editModel().exists()))
						.description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
						.mnemonic(FrameworkMessages.deleteMnemonic())
						.smallIcon(FrameworkIcons.instance().delete())
						.build();
	}

	private Control createClearControl() {
		return Control.builder(this::clearAndRequestFocus)
						.name(Messages.clear())
						.enabled(active)
						.description(Messages.clearTip() + ALT_PREFIX + Messages.clearMnemonic() + ")")
						.mnemonic(Messages.clearMnemonic())
						.smallIcon(FrameworkIcons.instance().clear())
						.build();
	}

	private Control createUpdateControl() {
		return Control.builder(new UpdateCommand())
						.name(FrameworkMessages.update())
						.enabled(State.and(active,
										editModel().updateEnabled(),
										editModel().exists(),
										editModel().modified()))
						.description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
						.mnemonic(FrameworkMessages.updateMnemonic())
						.smallIcon(FrameworkIcons.instance().update())
						.build();
	}

	private Control createInsertControl() {
		boolean useSaveCaption = Config.USE_SAVE_CAPTION.get();
		char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.addMnemonic();
		String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.add();
		return Control.builder(new InsertCommand())
						.name(caption)
						.enabled(State.and(active, editModel().insertEnabled()))
						.description(FrameworkMessages.addTip() + ALT_PREFIX + mnemonic + ")")
						.mnemonic(mnemonic)
						.smallIcon(FrameworkIcons.instance().add())
						.build();
	}

	private void bindEvents() {
		if (Config.INCLUDE_ENTITY_MENU.get()) {
			KeyEvents.builder(VK_V)
							.modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(Control.control(this::showEntityMenu))
							.enable(this);
		}
		editModel().confirmOverwriteEvent().addDataListener(confirmationState -> {
			int result = showConfirmDialog(Utilities.parentWindow(EntityEditPanel.this),
							FrameworkMessages.unsavedDataWarning(), FrameworkMessages.unsavedDataWarningTitle(),
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
			confirmationState.set(result == JOptionPane.YES_OPTION);
		});
	}

	private void setupFocusActivation() {
		if (configuration.focusActivation) {
			ACTIVE_STATE_GROUP.add(active);
			KeyboardFocusManager.getCurrentKeyboardFocusManager()
							.addPropertyChangeListener("focusOwner", new FocusActivationListener());
		}
	}

	private void showEntityMenu() {
		new EntityPopupMenu(editModel().entity(), editModel().connection()).show(this, 0, 0);
	}

	private Map<EditControl, Value<Control>> createControlsMap() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel has been initialized");
			}
		};

		return unmodifiableMap(Stream.of(EditControl.values())
						.collect(toMap(Function.identity(), controlCode -> {
							Value<Control> value = Value.value();
							value.addValidator(controlValueValidator);

							return value;
						})));
	}

	private static Config configure(Consumer<Config> configuration) {
		Config config = new Config();
		requireNonNull(configuration).accept(config);

		return new Config(config);
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
		 * Specifies whether to include a {@link EntityPopupMenu} on this edit panel, triggered with CTRL-ALT-V.<br>
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

		private static final Confirmer DEFAULT_INSERT_CONFIRMER = new InsertConfirmer();
		private static final Confirmer DEFAULT_UPDATE_CONFIRMER = new UpdateConfirmer();
		private static final Confirmer DEFAULT_DELETE_CONFIRMER = new DeleteConfirmer();

		private final Set<EditControl> editControls;

		private boolean clearAfterInsert = true;
		private boolean requestFocusAfterInsert = true;
		private boolean focusActivation = USE_FOCUS_ACTIVATION.get();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
						ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();
		private Confirmer insertConfirmer = DEFAULT_INSERT_CONFIRMER;
		private Confirmer deleteConfirmer = DEFAULT_DELETE_CONFIRMER;
		private Confirmer updateConfirmer = DEFAULT_UPDATE_CONFIRMER;

		private Config() {
			this.editControls = new HashSet<>(Arrays.asList(EditControl.values()));
		}

		private Config(Config config) {
			this.editControls = new HashSet<>(config.editControls);
			this.clearAfterInsert = config.clearAfterInsert;
			this.requestFocusAfterInsert = config.requestFocusAfterInsert;
			this.referentialIntegrityErrorHandling = config.referentialIntegrityErrorHandling;
			this.focusActivation = config.focusActivation;
			this.insertConfirmer = config.insertConfirmer;
			this.updateConfirmer = config.updateConfirmer;
			this.deleteConfirmer = config.deleteConfirmer;
		}

		/**
		 * @param editControls if specified only controls with those keys are initialized, null or an empty array will result in no controls being initialized
		 * @return this Config instance
		 */
		public Config editControls(EditControl... editControls) {
			this.editControls.clear();
			this.editControls.addAll(validateControlCodes(editControls));
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

		private static Set<EditControl> validateControlCodes(EditControl[] editControls) {
			if (editControls == null) {
				return emptySet();
			}
			for (EditControl editControl : editControls) {
				requireNonNull(editControl, "controlCode");
			}

			return new HashSet<>(Arrays.asList(editControls));
		}
	}

	/**
	 * Handles displaying confirmation messages for common actions to the user.
	 */
	public interface Confirmer {

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

		@Override
		public void execute() throws ValidationException {
			if (confirmInsert()) {
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

		private void onException(Throwable exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
	}

	private final class UpdateCommand implements Control.Command {

		@Override
		public void execute() throws ValidationException {
			if (confirmUpdate()) {
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

		private void onException(Throwable exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
	}

	private final class DeleteCommand implements Control.Command {

		@Override
		public void execute() {
			if (confirmDelete()) {
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

		private void onException(Throwable exception) {
			LOG.error(exception.getMessage(), exception);
			EntityEditPanel.this.onException(exception);
		}
	}

	private class FocusActivationListener implements PropertyChangeListener {

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

				if (editPanel == EntityEditPanel.this && configuration.focusActivation) {
					editPanel.active.set(true);
				}
			}
		}

		private EntityPanel entityPanel(Component focusedComponent) {
			if (focusedComponent instanceof JTabbedPane) {
				Component selectedComponent = ((JTabbedPane) focusedComponent).getSelectedComponent();
				if (selectedComponent instanceof EntityPanel) {
					return (EntityPanel) selectedComponent;
				}
			}

			return parentOfType(EntityPanel.class, focusedComponent);
		}
	}

	private static final class InsertConfirmer implements Confirmer {

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return true;
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
