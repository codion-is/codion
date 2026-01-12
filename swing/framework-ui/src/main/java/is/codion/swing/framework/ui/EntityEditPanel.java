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
 * Copyright (c) 2009 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.db.database.Database.Operation;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.entity.exception.ValidationException.InvalidAttribute;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditModel.EditTask;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.*;
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
	 * Note that changing the shortcut keystroke after a panel
	 * has been initialized has no effect on its controls.
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
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
		public static final ControlKey<CommandControl> SELECT_INPUT_FIELD = CommandControl.key("selectInputField", keyStroke(VK_I, MENU_SHORTCUT_MASK));
		/**
		 * Displays the entity menu, if available.<br>
		 * Default key stroke: CTRL-ALT-V
		 * @see Config#INCLUDE_ENTITY_MENU
		 */
		public static final ControlKey<CommandControl> DISPLAY_ENTITY_MENU = CommandControl.key("displayEntityMenu", keyStroke(VK_V, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Displays the query inspector, if one is available.<br>
		 * Default key stroke: CTRL-ALT-Q
		 */
		public static final ControlKey<CommandControl> DISPLAY_QUERY_INSPECTOR = CommandControl.key("displayQueryInspector", keyStroke(VK_Q, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));

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

	private static final Consumer<Config> NO_CONFIGURATION = (Consumer<Config>) EMPTY_CONSUMER;

	private final Map<EntityType, EntityTablePanelPreferences> dependencyPanelPreferences = new HashMap<>();
	private final AtomicReference<Dimension> dependenciesDialogSize = new AtomicReference<>();
	private final Controls.Layout controlsLayout;
	private final State active;

	private @Nullable InsertUpdateQueryInspector queryInspector;

	final Config configuration;

	private boolean initialized = false;

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 */
	protected EntityEditPanel(SwingEntityEditModel editModel) {
		this(editModel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
	 * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
	 * @param config provides access to the panel configuration
	 */
	protected EntityEditPanel(SwingEntityEditModel editModel, Consumer<Config> config) {
		super(editModel);
		this.configuration = configure(config);
		this.active = State.state(!configuration.focusActivation);
		this.controlsLayout = createControlsLayout();
		validIndicator().set(configuration.validIndicator);
		modifiedIndicator().set(configuration.modifiedIndicator);
		createControls();
		setupFocusActivation();
		setupKeyboardActions();
		if (editModel.editor().exists().not().is()) {
			editModel.editor().defaults();
		}
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(queryInspector);
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
			Dialogs.select()
							.list(sortedDefinitions)
							.owner(this)
							.title(FrameworkMessages.selectInputField())
							.select()
							.single()
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
			LOG.debug("{} - initializing", this);
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
	 * Writes user preferences associated with this edit panel.
	 * Override to write application specific preferences.
	 * <p>Remember to call {@code super.writePreferences(preferences)} when overriding.
	 * @param preferences the preferences instance to write to
	 * @see #preferencesKey()
	 */
	public void writePreferences(Preferences preferences) {
		requireNonNull(preferences);
	}

	/**
	 * Applies any user preferences previously written via {@link #writePreferences(Preferences)}
	 * Override to apply application specific preferences.
	 * <p>Remember to call {@code super.applyPreferences(preferences)} when overriding.
	 * @param preferences the preferences instance containing the preferences to apply
	 */
	public void applyPreferences(Preferences preferences) {
		requireNonNull(preferences);
	}

	/**
	 * <p>Performs insert on the active entity.
	 * <p>If insert confirmation is enabled, confirmation is requested first using
	 * the {@link Confirmer} specified via {@link Config#insertConfirmer(Confirmer)}.
	 * @see Config#confirmInsert(boolean)
	 * @see Config#insertConfirmer(Confirmer)
	 */
	public final void insert() {
		insertCommand().execute();
	}

	/**
	 * <p>Performs delete on the active entity.
	 * <p>If delete confirmation is enabled, confirmation is requested first using
	 * the {@link Confirmer} specified via {@link Config#deleteConfirmer(Confirmer)}.
	 * @see Config#confirmDelete()
	 * @see Config#deleteConfirmer(Confirmer)
	 */
	public final void delete() {
		deleteCommand().execute();
	}

	/**
	 * <p>Performs update on the active entity.
	 * <p>If update confirmation is enabled, confirmation is requested first using
	 * the {@link Confirmer} specified via {@link Config#updateConfirmer(Confirmer)}.
	 * @see Config#confirmUpdate(boolean)
	 * @see Config#updateConfirmer(Confirmer)
	 */
	public final void update() {
		updateCommand().execute();
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
	protected final InsertCommand.Builder insertCommand() {
		return new DefaultInsertCommand.DefaultBuilder(this);
	}

	/**
	 * Returns an async update command builder
	 * @return a new async update command builder
	 */
	protected final UpdateCommand.Builder updateCommand() {
		return new DefaultUpdateCommand.DefaultBuilder(this);
	}

	/**
	 * Returns an async delete command builder
	 * @return a new async delete command builder
	 */
	protected final DeleteCommand.Builder deleteCommand() {
		return new DefaultDeleteCommand.DefaultBuilder(this);
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
			EntityDependenciesPanel.displayDependencies(singletonList(editModel().editor().get()), editModel().connectionProvider(),
							this, dependenciesDialogSize, dependencyPanelPreferences, true);
		}
		else {
			super.onException(exception);
		}
	}

	/**
	 * Displays the exception message after which the first component involved receives the focus.
	 * @param exception the validation exception
	 */
	protected void onValidationException(ValidationException exception) {
		requireNonNull(exception);
		Attribute<?> firstAttribute = firstAttribute(exception.invalidAttributes());
		InvalidAttribute invalidAttribute = exception.invalidAttributes().stream()
						.filter(invalid -> invalid.attribute().equals(firstAttribute))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		JOptionPane.showMessageDialog(this, invalidAttribute.message(), Messages.error(), JOptionPane.ERROR_MESSAGE);
		focus().request(invalidAttribute.attribute());
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
	 *      createTextField(DomainModel.USER_NAME);
	 *      createTextField(DomainModel.USER_ADDRESS);
	 *
	 *      setLayout(new GridLayout(2, 1, 5, 5));
	 *
	 *      addInputPanel(DomainModel.USER_NAME);
	 *      addInputPanel(DomainModel.USER_ADDRESS);
	 *   }
	 *}
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
	 *}
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

	/**
	 * Returns the key used to identify user preferences for this edit panel
	 * The default implementation is:
	 * {@snippet :
	 * return editModel().getClass().getSimpleName() + "-" + editModel().entityType();
	 *}
	 * Override in case this key is not unique within the application.
	 * @return the key used to identify user preferences for this edit panel
	 */
	protected String preferencesKey() {
		return editModel().getClass().getSimpleName() + "-" + editModel().entityType();
	}

	private void createControls() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel is initialized");
			}
		};
		ControlMap controlMap = configuration.controlMap;
		controlMap.controls().forEach(control -> control.addValidator(controlValueValidator));
		if (!editModel().settings().readOnly().is()) {
			if (editModel().settings().insertEnabled().is()) {
				controlMap.control(INSERT).set(createInsertControl());
			}
			if (editModel().settings().updateEnabled().is()) {
				controlMap.control(UPDATE).set(createUpdateControl());
			}
			if (editModel().settings().deleteEnabled().is()) {
				controlMap.control(DELETE).set(createDeleteControl());
			}
		}
		controlMap.control(CLEAR).set(createClearControl());
		controlMap.control(SELECT_INPUT_FIELD).set(createSelectInputComponentControl());
		if (configuration.includeQueryInspector) {
			controlMap.control(DISPLAY_QUERY_INSPECTOR).set(command(this::showQueryInspector));
		}
		if (configuration.includeEntityMenu) {
			controlMap.control(DISPLAY_ENTITY_MENU).set(createShowEntityMenuControl());
		}
	}

	private CommandControl createDeleteControl() {
		return Control.builder()
						.command(this::delete)
						.caption(FrameworkMessages.delete())
						.enabled(State.and(active,
										editModel().settings().deleteEnabled(),
										editModel().editor().exists()))
						.description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
						.mnemonic(FrameworkMessages.deleteMnemonic())
						.icon(ICONS.delete())
						.onException(this::onException)
						.build();
	}

	private CommandControl createClearControl() {
		return Control.builder()
						.command(this::clearAndRequestFocus)
						.caption(Messages.clear())
						.enabled(active)
						.description(Messages.clearTip() + ALT_PREFIX + Messages.clearMnemonic() + ")")
						.mnemonic(Messages.clearMnemonic())
						.icon(ICONS.clear())
						.build();
	}

	private CommandControl createSelectInputComponentControl() {
		return command(this::selectInputComponent);
	}

	private void showQueryInspector() {
		if (queryInspector == null) {
			queryInspector = new InsertUpdateQueryInspector(editModel());
		}
		if (queryInspector.isShowing()) {
			Ancestor.window().of(queryInspector).toFront();
		}
		else {
			Dialogs.builder()
							.component(queryInspector)
							.owner(this)
							.title(editModel().entityDefinition().caption() + " Query")
							.modal(false)
							.show();
		}
	}

	private CommandControl createShowEntityMenuControl() {
		return command(this::showEntityMenu);
	}

	private CommandControl createUpdateControl() {
		return Control.builder()
						.command(this::update)
						.caption(FrameworkMessages.update())
						.enabled(State.and(active,
										editModel().settings().updateEnabled(),
										editModel().editor().modified()))
						.description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
						.mnemonic(FrameworkMessages.updateMnemonic())
						.icon(ICONS.update())
						.onException(this::onException)
						.build();
	}

	private CommandControl createInsertControl() {
		boolean useSaveCaption = EntityEditPanel.Config.USE_SAVE_CAPTION.getOrThrow();
		char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.insertMnemonic();
		String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.insert();
		return Control.builder()
						.command(this::insert)
						.caption(caption)
						.enabled(State.and(active, editModel().settings().insertEnabled()))
						.description(FrameworkMessages.insertTip() + ALT_PREFIX + mnemonic + ")")
						.mnemonic(mnemonic)
						.icon(ICONS.add())
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
		if (configuration.modifiedWarning && editModel().editor().modified().is()) {
			Set<Attribute<?>> modified = editModel().editor().modified().attributes().get();
			if (showConfirmDialog(this, createModifiedMessage(modified),
							FrameworkMessages.modifiedWarningTitle(), JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
				if (!modified.isEmpty()) {
					focus().request(modified.iterator().next());
				}
				throw new CancelException();
			}
		}
	}

	private String createModifiedMessage(Set<Attribute<?>> modified) {
		if (modified.isEmpty()) {
			return FrameworkMessages.modifiedWarning();
		}

		return modified.stream()
						.map(attribute -> editModel().entityDefinition().attributes().definition(attribute))
						.map(AttributeDefinition::caption)
						.collect(Collectors.joining(", ", "", "\n\n" + FrameworkMessages.modifiedWarning()));
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
		configuration.controlMap.keyEvent(DISPLAY_QUERY_INSPECTOR).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
		configuration.controlMap.keyEvent(SELECT_INPUT_FIELD).ifPresent(keyEvent ->
						keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
										.enable(this));
	}

	private void showEntityMenu() {
		new EntityPopupMenu(editModel().editor().get(), editModel().connection()).show(this, 0, 0);
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private void handleException(Exception exception) {
		if (exception instanceof ValidationException ||
						exception instanceof ReferentialIntegrityException) {
			LOG.debug(exception.getMessage(), exception);
		}
		else {
			LOG.error(exception.getMessage(), exception);
		}
		onException(exception);
	}

	private Attribute<?> firstAttribute(Collection<InvalidAttribute> invalidAttributes) {
		Set<Attribute<?>> attributes = invalidAttributes.stream()
						.map(InvalidAttribute::attribute)
						.collect(toSet());
		return components().entrySet().stream()
						.filter(entry -> attributes.contains(entry.getKey()))
						.min(EntityEditPanel::compareFocusOrder)
						.map(Map.Entry::getKey)
						// Fallback to the first one, in case no component exists
						.orElse(invalidAttributes.iterator().next().attribute());
	}

	private static int compareFocusOrder(Map.Entry<Attribute<?>, EditorComponent<?>> entry1, Map.Entry<Attribute<?>, EditorComponent<?>> entry2) {
		return LAYOUT_FOCUS_TRAVERSAL_POLICY.getComparator().compare(entry1.getValue().get(), entry2.getValue().get());
	}

	private static boolean componentSelectable(@Nullable JComponent component) {
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
		 * Specifies whether to include a Query Inspector on this edit panel, triggered with CTRL-ALT-Q.
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_QUERY_INSPECTOR =
						booleanValue(EntityEditPanel.class.getName() + ".includeQueryInspector", false);

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

		/**
		 * Indicates whether the panel should ask for confirmation before inserting
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> CONFIRM_INSERT =
						booleanValue(EntityEditPanel.class.getName() + ".confirmInsert", false);

		/**
		 * Indicates whether the panel should ask for confirmation before updating
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> CONFIRM_UPDATE =
						booleanValue(EntityEditPanel.class.getName() + ".confirmUpdate", true);

		/**
		 * Indicates whether the panel should ask for confirmation before deleting
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> CONFIRM_DELETE =
						booleanValue(EntityEditPanel.class.getName() + ".confirmDelete", true);

		private static final Confirmer DEFAULT_INSERT_CONFIRMER = new InsertConfirmer();
		private static final Confirmer DEFAULT_UPDATE_CONFIRMER = new UpdateConfirmer();
		private static final Confirmer DEFAULT_DELETE_CONFIRMER = new DeleteConfirmer();

		private final EntityEditPanel editPanel;

		private boolean clearAfterInsert = true;
		private boolean requestFocusAfterInsert = true;
		private boolean focusActivation = USE_FOCUS_ACTIVATION.getOrThrow();
		private boolean includeEntityMenu = INCLUDE_ENTITY_MENU.getOrThrow();
		private boolean includeQueryInspector = INCLUDE_QUERY_INSPECTOR.getOrThrow();
		private boolean modifiedWarning = MODIFIED_WARNING.getOrThrow();
		private boolean validIndicator = VALID_INDICATOR.getOrThrow();
		private boolean modifiedIndicator = MODIFIED_INDICATOR.getOrThrow();
		private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
						ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.getOrThrow();
		private boolean confirmInsert = CONFIRM_INSERT.getOrThrow();
		private boolean confirmUpdate = CONFIRM_UPDATE.getOrThrow();
		private boolean confirmDelete = CONFIRM_DELETE.getOrThrow();
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
			this.confirmInsert = config.confirmInsert;
			this.confirmUpdate = config.confirmUpdate;
			this.confirmDelete = config.confirmDelete;
			this.insertConfirmer = config.insertConfirmer;
			this.updateConfirmer = config.updateConfirmer;
			this.deleteConfirmer = config.deleteConfirmer;
			this.includeEntityMenu = config.includeEntityMenu;
			this.includeQueryInspector = config.includeQueryInspector;
			this.modifiedWarning = config.modifiedWarning;
			this.validIndicator = config.validIndicator;
			this.modifiedIndicator = config.modifiedIndicator;
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
		 * @param includeQueryInspector true if a Query Inspector should be available in this edit panel, triggered with CTRL-ALT-Q.
		 * @return this Config instance
		 */
		public Config includeQueryInspector(boolean includeQueryInspector) {
			this.includeQueryInspector = includeQueryInspector;
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
		 * @param validIndicator specifies whether components should indicate validity
		 * @return this Config instance
		 * @see EntityEditComponentPanel#VALID_INDICATOR
		 */
		public Config validIndicator(boolean validIndicator) {
			this.validIndicator = validIndicator;
			return this;
		}

		/**
		 * @param modifiedIndicator specifies whether components should indicate modification
		 * @return this Config instance
		 * @see EntityEditComponentPanel#MODIFIED_INDICATOR
		 */
		public Config modifiedIndicator(boolean modifiedIndicator) {
			this.modifiedIndicator = modifiedIndicator;
			return this;
		}

		/**
		 * @param confirmInsert whether to confirm before inserting
		 * @return this Config instance
		 * @see #insertConfirmer(Confirmer)
		 * @see #CONFIRM_INSERT
		 */
		public Config confirmInsert(boolean confirmInsert) {
			this.confirmInsert = confirmInsert;
			return this;
		}

		/**
		 * @param confirmUpdate whether to confirm before updating
		 * @return this Config instance
		 * @see #updateConfirmer(Confirmer)
		 * @see #CONFIRM_UPDATE
		 */
		public Config confirmUpdate(boolean confirmUpdate) {
			this.confirmUpdate = confirmUpdate;
			return this;
		}

		/**
		 * @param confirmDelete whether to confirm before deleting
		 * @return this Config instance
		 * @see #deleteConfirmer(Confirmer)
		 * @see #CONFIRM_DELETE
		 */
		public Config confirmDelete(boolean confirmDelete) {
			this.confirmDelete = confirmDelete;
			return this;
		}

		/**
		 * @param insertConfirmer the insert confirmer
		 * @return this Config instance
		 * @see #confirmInsert(boolean)
		 */
		public Config insertConfirmer(Confirmer insertConfirmer) {
			this.insertConfirmer = requireNonNull(insertConfirmer);
			return this;
		}

		/**
		 * @param deleteConfirmer the delete confirmer
		 * @return this Config instance
		 * @see #confirmDelete(boolean)
		 */
		public Config deleteConfirmer(Confirmer deleteConfirmer) {
			this.deleteConfirmer = requireNonNull(deleteConfirmer);
			return this;
		}

		/**
		 * @param updateConfirmer the update confirmer
		 * @return this Config instance
		 * @see #confirmUpdate(boolean)
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
	 */
	public interface Confirmer {

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
	 * Performs insert.
	 */
	public interface InsertCommand extends Command {

		@Override
		void execute();

		/**
		 * Builds an async insert command
		 */
		interface Builder {

			/**
			 * @param confirm specifies whether confirmation should be requested, default false
			 * @return this builder instance
			 */
			Builder confirm(boolean confirm);

			/**
			 * @param onInsert called after a successful insert
			 * @return this builder instance
			 */
			Builder onInsert(Runnable onInsert);

			/**
			 * @param onInsert called after a successful insert
			 * @return this builder instance
			 */
			Builder onInsert(Consumer<Collection<Entity>> onInsert);

			/**
			 * Builds and executes this command
			 */
			void execute();

			/**
			 * @return the command
			 */
			InsertCommand build();
		}
	}

	/**
	 * Performs update.
	 */
	public interface UpdateCommand extends Command {

		@Override
		void execute();

		/**
		 * Builds an async update command
		 */
		interface Builder {

			/**
			 * @param confirm specifies whether confirmation should be requested, default true
			 * @return this builder instance
			 */
			Builder confirm(boolean confirm);

			/**
			 * @param onUpdate called after a successful update
			 * @return this builder instance
			 */
			Builder onUpdate(Runnable onUpdate);

			/**
			 * @param onUpdate called after a successful update
			 * @return this builder instance
			 */
			Builder onUpdate(Consumer<Collection<Entity>> onUpdate);

			/**
			 * Builds and executes this command
			 */
			void execute();

			/**
			 * @return the command
			 */
			UpdateCommand build();
		}
	}

	/**
	 * Performs delete.
	 */
	public interface DeleteCommand extends Command {

		@Override
		void execute();

		/**
		 * Builds an async delete command
		 */
		interface Builder {

			/**
			 * @param confirm specifies whether confirmation should be requested, default true
			 * @return this builder instance
			 */
			Builder confirm(boolean confirm);

			/**
			 * @param onDelete called after a successful delete
			 * @return this builder instance
			 */
			Builder onDelete(Runnable onDelete);

			/**
			 * @param onDelete called after a successful delete
			 * @return this builder instance
			 */
			Builder onDelete(Consumer<Collection<Entity>> onDelete);

			/**
			 * Builds and executes this command
			 */
			void execute();

			/**
			 * @return the command
			 */
			DeleteCommand build();
		}
	}

	protected static final class DefaultInsertCommand implements InsertCommand {

		private final EntityEditPanel editPanel;
		private final boolean confirm;
		private final Collection<Consumer<Collection<Entity>>> onInsert;

		private DefaultInsertCommand(DefaultBuilder builder) {
			this.editPanel = builder.editPanel;
			this.confirm = builder.confirm;
			this.onInsert = builder.onInsert;
		}

		@Override
		public void execute() {
			if (!confirm || editPanel.confirmInsert()) {
				Dialogs.progressWorker()
								.task(editPanel.editModel().insertTask().prepare()::perform)
								.title(MESSAGES.getString("inserting"))
								.owner(editPanel)
								.onResult(this::handleResult)
								.onException(editPanel::handleException)
								.execute();
			}
		}

		private void handleResult(EditTask.Result result) {
			Collection<Entity> inserted = result.handle();
			onInsert.forEach(consumer -> consumer.accept(inserted));
			if (editPanel.configuration.clearAfterInsert) {
				editPanel.editModel().editor().defaults();
			}
			if (editPanel.configuration.requestFocusAfterInsert) {
				editPanel.focus().afterInsert().request();
			}
		}

		private static final class DefaultBuilder implements Builder {

			private final EntityEditPanel editPanel;
			private final Collection<Consumer<Collection<Entity>>> onInsert = new ArrayList<>(1);

			private boolean confirm;

			private DefaultBuilder(EntityEditPanel editPanel) {
				this.editPanel = editPanel;
				this.confirm = editPanel.configuration.confirmInsert;
			}

			@Override
			public Builder confirm(boolean confirm) {
				this.confirm = confirm;
				return this;
			}

			@Override
			public Builder onInsert(Runnable onInsert) {
				requireNonNull(onInsert);

				return onInsert(inserted -> onInsert.run());
			}

			@Override
			public Builder onInsert(Consumer<Collection<Entity>> onInsert) {
				this.onInsert.add(requireNonNull(onInsert));
				return this;
			}

			@Override
			public void execute() {
				build().execute();
			}

			@Override
			public InsertCommand build() {
				return new DefaultInsertCommand(this);
			}
		}
	}

	private static final class DefaultUpdateCommand implements UpdateCommand {

		private final EntityEditPanel editPanel;
		private final boolean confirm;
		private final Collection<Consumer<Collection<Entity>>> onUpdate;

		private DefaultUpdateCommand(DefaultBuilder builder) {
			this.editPanel = builder.editPanel;
			this.confirm = builder.confirm;
			this.onUpdate = builder.onUpdate;
		}

		@Override
		public void execute() {
			if (!confirm || editPanel.confirmUpdate()) {
				Dialogs.progressWorker()
								.task(editPanel.editModel().updateTask().prepare()::perform)
								.title(MESSAGES.getString("updating"))
								.owner(editPanel)
								.onResult(this::handleResult)
								.onException(editPanel::handleException)
								.execute();
			}
		}

		private void handleResult(EditTask.Result result) {
			Collection<Entity> updated = result.handle();
			onUpdate.forEach(consumer -> consumer.accept(updated));
			editPanel.focus().afterUpdate().request();
		}

		private static final class DefaultBuilder implements Builder {

			private final EntityEditPanel editPanel;
			private final Collection<Consumer<Collection<Entity>>> onUpdate = new ArrayList<>(1);
			private boolean confirm;

			private DefaultBuilder(EntityEditPanel editPanel) {
				this.editPanel = editPanel;
				this.confirm = editPanel.configuration.confirmUpdate;
			}

			@Override
			public Builder confirm(boolean confirm) {
				this.confirm = confirm;
				return this;
			}

			@Override
			public Builder onUpdate(Runnable onUpdate) {
				requireNonNull(onUpdate);

				return onUpdate(updated -> onUpdate.run());
			}

			@Override
			public Builder onUpdate(Consumer<Collection<Entity>> onUpdate) {
				this.onUpdate.add(requireNonNull(onUpdate));
				return this;
			}

			@Override
			public void execute() {
				build().execute();
			}

			@Override
			public UpdateCommand build() {
				return new DefaultUpdateCommand(this);
			}
		}
	}

	private static final class DefaultDeleteCommand implements DeleteCommand {

		private final EntityEditPanel editPanel;
		private final boolean confirm;
		private final Collection<Consumer<Collection<Entity>>> onDelete;

		private DefaultDeleteCommand(DefaultBuilder builder) {
			this.editPanel = builder.editPanel;
			this.confirm = builder.confirm;
			this.onDelete = builder.onDelete;
		}

		@Override
		public void execute() {
			if (!confirm || editPanel.confirmDelete()) {
				Dialogs.progressWorker()
								.task(editPanel.editModel().deleteTask().prepare()::perform)
								.title(MESSAGES.getString("deleting"))
								.owner(editPanel)
								.onResult(this::handleResult)
								.onException(editPanel::handleException)
								.execute();
			}
		}

		private void handleResult(EditTask.Result result) {
			Collection<Entity> deleted = result.handle();
			onDelete.forEach(consumer -> consumer.accept(deleted));
			editPanel.focus().initial().request();
		}

		protected static final class DefaultBuilder implements Builder {

			private final EntityEditPanel editPanel;
			private final Collection<Consumer<Collection<Entity>>> onDelete = new ArrayList<>(1);
			private boolean confirm;

			private DefaultBuilder(EntityEditPanel editPanel) {
				this.editPanel = editPanel;
				this.confirm = editPanel.configuration.confirmDelete;
			}

			@Override
			public Builder confirm(boolean confirm) {
				this.confirm = confirm;
				return this;
			}

			@Override
			public Builder onDelete(Runnable onDelete) {
				requireNonNull(onDelete);

				return onDelete(deleted -> onDelete.run());
			}

			@Override
			public Builder onDelete(Consumer<Collection<Entity>> onDelete) {
				this.onDelete.add(requireNonNull(onDelete));
				return this;
			}

			@Override
			public void execute() {
				build().execute();
			}

			@Override
			public DeleteCommand build() {
				return new DefaultDeleteCommand(this);
			}
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
					editPanel = Ancestor.ofType(EntityEditPanel.class).of(focusedComponent).get();
				}
				if (editPanel != null && editPanel.configuration.focusActivation) {
					editPanel.active.set(true);
				}
			}
		}

		private static @Nullable EntityPanel entityPanel(Component focusedComponent) {
			if (focusedComponent instanceof JTabbedPane) {
				Component selectedComponent = ((JTabbedPane) focusedComponent).getSelectedComponent();
				if (selectedComponent instanceof EntityPanel) {
					return (EntityPanel) selectedComponent;
				}
			}

			return Ancestor.ofType(EntityPanel.class).of(focusedComponent).get();
		}
	}

	private static final class InsertConfirmer implements Confirmer {

		@Override
		public boolean confirm(JComponent dialogOwner) {
			return confirm(dialogOwner, FrameworkMessages.confirmInsert(), FrameworkMessages.insert());
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
