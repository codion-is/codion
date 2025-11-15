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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.ModelLink;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.border.Borders;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlIcon;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

import static is.codion.common.utilities.Configuration.*;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.command;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.key.KeyEvents.MENU_SHORTCUT_MASK;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityEditPanel.ControlKeys.SELECT_INPUT_FIELD;
import static is.codion.swing.framework.ui.EntityPanel.ControlKeys.*;
import static is.codion.swing.framework.ui.EntityPanel.ControlKeys.REFRESH;
import static is.codion.swing.framework.ui.EntityPanel.Direction.*;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static is.codion.swing.framework.ui.EntityPanel.WindowType.DIALOG;
import static is.codion.swing.framework.ui.EntityPanel.WindowType.FRAME;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.SwingConstants.VERTICAL;

/**
 * A panel representing an Entity via a EntityModel, which facilitates browsing and editing of records.
 * {@snippet :
 *   EntityConnectionProvider connectionProvider = createConnectionProvider();
 *   SwingEntityModel entityModel = new SwingEntityModel(Employee.TYPE, connectionProvider);
 *   EntityPanel entityPanel = new EntityPanel(entityModel);
 *   entityPanel.initialize();
 *   JFrame frame = new JFrame();
 *   frame.add(entityPanel);
 *   frame.pack();
 *   frame.setVisible(true);
 *}
 */
public class EntityPanel extends JPanel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityPanel.class, getBundle(EntityPanel.class.getName()));
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * The possible states of a detail or edit panel.
	 */
	public enum PanelState {
		/**
		 * Embedded in the master panel
		 */
		EMBEDDED,
		/**
		 * Displayed in a window or dialog
		 */
		WINDOW,
		/**
		 * Hidden
		 */
		HIDDEN
	}

	/**
	 * The navigation directions.
	 */
	public enum Direction {
		/**
		 * Navigate up to the parent panel, if one exists
		 */
		UP,
		/**
		 * Navigate down to the currently active detail panel
		 */
		DOWN,
		/**
		 * Navigate to the next panel
		 */
		RIGHT,
		/**
		 * Navigate to the previous panel
		 */
		LEFT
	}

	/**
	 * Specifies the window type.
	 */
	public enum WindowType {
		/**
		 * Display panels in a JFrame
		 */
		FRAME,
		/**
		 * Display panels in a JDialog
		 */
		DIALOG
	}

	/**
	 * The standard controls available in a entity panel
	 * <p>Note: CTRL in key stroke descriptions represents the platform menu shortcut key (CTRL on Windows/Linux, ⌘ on macOS).
	 */
	public static final class ControlKeys {

		/**
		 * Requests focus for the edit panel (intial focus component).<br>
		 * Default key stroke: CTRL-E
		 */
		public static final ControlKey<CommandControl> REQUEST_EDIT_PANEL_FOCUS = CommandControl.key("requestEditPanelFocus", keyStroke(VK_E, MENU_SHORTCUT_MASK));
		/**
		 * Toggles the edit panel between hidden, embedded and dialog.<br>
		 * Default key stroke: CTRL-ALT-E
		 */
		public static final ControlKey<CommandControl> TOGGLE_EDIT_PANEL = CommandControl.key("toggleEditPanel", keyStroke(VK_E, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Navigates to the parent panel, if one is available.<br>
		 * Default key stroke: CTRL-ALT-UP ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_UP = CommandControl.key("navigateUp", keyStroke(VK_UP, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Navigates to the selected child panel, if one is available.<br>
		 * Default key stroke: CTRL-ALT-DOWN ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_DOWN = CommandControl.key("navigateDown", keyStroke(VK_DOWN, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Navigates to the sibling panel on the right, if one is available.<br>
		 * Default key stroke: CTRL-ALT-RIGHT ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_RIGHT = CommandControl.key("navigateRight", keyStroke(VK_RIGHT, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Navigates to the sibling panel on the left, if one is available.<br>
		 * Default key stroke: CTRL-ALT-LEFT ARROW
		 */
		public static final ControlKey<CommandControl> NAVIGATE_LEFT = CommandControl.key("navigateLeft", keyStroke(VK_LEFT, MENU_SHORTCUT_MASK | ALT_DOWN_MASK));
		/**
		 * Refreshes the table.
		 */
		public static final ControlKey<CommandControl> REFRESH = CommandControl.key("refresh");
		/**
		 * The edit panel controls.
		 */
		public static final ControlKey<Controls> EDIT_CONTROLS = Controls.key("editControls");

		private ControlKeys() {}
	}

	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	private final SwingEntityModel entityModel;
	private final DetailPanels detailPanels = new DetailPanels();
	private final @Nullable EntityEditPanel editPanel;
	private final @Nullable EntityTablePanel tablePanel;
	private final @Nullable JPanel editControlPanel;
	private final JPanel mainPanel;
	private final DetailLayout detailLayout;
	private final DetailController detailController;
	private final Event<EntityPanel> activatedEvent = Event.event();
	private final Value<PanelState> editPanelState;
	private final UnaryOperator<PanelState> editPanelStateMapper;

	private final Config configuration;
	private final Controls.Layout controlsLayout;

	private @Nullable EntityPanel parentPanel;
	private @Nullable EntityPanel previousPanel;
	private @Nullable EntityPanel nextPanel;
	private @Nullable Window editWindow;

	private boolean initialized = false;

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 */
	public EntityPanel(SwingEntityModel entityModel) {
		this(requireNonNull(entityModel), NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param config provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, Consumer<Config> config) {
		this(requireNonNull(entityModel), null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, config);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityEditPanel editPanel) {
		this(requireNonNull(entityModel), editPanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param config provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityEditPanel editPanel, Consumer<Config> config) {
		this(requireNonNull(entityModel), editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, config);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param tablePanel the table panel
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityTablePanel tablePanel) {
		this(entityModel, tablePanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param tablePanel the table panel
	 * @param config provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityTablePanel tablePanel, Consumer<Config> config) {
		this(entityModel, null, tablePanel, config);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param tablePanel the table panel
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityEditPanel editPanel, @Nullable EntityTablePanel tablePanel) {
		this(entityModel, editPanel, tablePanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param tablePanel the table panel
	 * @param config provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, @Nullable EntityEditPanel editPanel, @Nullable EntityTablePanel tablePanel,
										 Consumer<Config> config) {
		this.entityModel = requireNonNull(entityModel);
		this.editPanel = editPanel;
		this.tablePanel = tablePanel;
		this.configuration = configure(config);
		this.editControlPanel = createEditControlPanel();
		this.mainPanel = borderLayoutPanel()
						.minimumSize(new Dimension(0, 0))
						.border(configuration.border)
						.build();
		this.controlsLayout = createControlsLayout();
		this.detailLayout = configuration.detailLayout.apply(this);
		this.detailController = detailLayout.controller().orElse(new DetailController() {});
		this.editPanelStateMapper = panelStateMapper(configuration.enabledEditStates);
		this.editPanelState = Value.builder()
						.nonNull(configuration.initialEditState)
						.consumer(this::updateEditState)
						.build();
		createControls();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(editControlPanel, mainPanel, tablePanel, editPanel);
		if (detailPanels != null) {
			Utilities.updateUI(detailPanels.get());
		}
		if (detailLayout != null) {
			detailLayout.updateUI();
		}
	}

	/**
	 * @return the EntityModel
	 */
	public final SwingEntityModel model() {
		return entityModel;
	}

	/**
	 * @return the EntityEditModel
	 */
	public final SwingEntityEditModel editModel() {
		return entityModel.editModel();
	}

	/**
	 * @return the EntityTableModel
	 * @throws IllegalStateException in case no table model is available
	 */
	public final SwingEntityTableModel tableModel() {
		return entityModel.tableModel();
	}

	/**
	 * @return the parent panel or an empty Optional in case of a root panel
	 */
	public final Optional<EntityPanel> parentPanel() {
		return Optional.ofNullable(parentPanel);
	}

	/**
	 * @return the detail panels
	 */
	public final DetailPanels detailPanels() {
		return detailPanels;
	}

	/**
	 * Initializes this EntityPanel, in case of some specific initialization code you can override the
	 * {@link #initializeUI()} method and add your code there. Calling this method a second time has no effect.
	 * @return this EntityPanel instance
	 */
	public final EntityPanel initialize() {
		if (!initialized) {
			try {
				setupControls();
				setFocusCycleRoot(true);
				setupEditAndTablePanelControls();
				initializeEditPanel();
				initializeUI();
				initializeTablePanel();
				setupKeyboardActions();
			}
			finally {
				initialized = true;
			}
		}

		return this;
	}

	/**
	 * @return the edit panel
	 * @throws IllegalStateException in case no edit panel is available
	 * @see #containsEditPanel()
	 */
	public final EntityEditPanel editPanel() {
		if (editPanel == null) {
			throw new IllegalStateException("No edit panel available");
		}

		return editPanel;
	}

	/**
	 * @return true if this panel contains an edit panel.
	 */
	public final boolean containsEditPanel() {
		return editPanel != null;
	}

	/**
	 * @return the table panel
	 * @throws IllegalStateException in case no table panel is available
	 * @see #containsTablePanel()
	 */
	public final EntityTablePanel tablePanel() {
		if (tablePanel == null) {
			throw new IllegalStateException("No table panel available");
		}

		return tablePanel;
	}

	/**
	 * @return true if this panel contains a table panel.
	 */
	public final boolean containsTablePanel() {
		return tablePanel != null;
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code controlKey},
	 * an empty {@link Value} if no such control is available.
	 * @param <T> the control type
	 * @param controlKey the control key
	 * @return the {@link Value} containing the control associated with {@code controlKey}
	 */
	public final <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		return configuration.controlMap.control(requireNonNull(controlKey));
	}

	/**
	 * Enables the given key event on this panel
	 * @param keyEventBuilder the key event builder
	 */
	public final void addKeyEvent(KeyEvents.Builder keyEventBuilder) {
		requireNonNull(keyEventBuilder).enable(this);
		if (editControlPanel != null) {
			keyEventBuilder.enable(editControlPanel);
		}
	}

	/**
	 * Disables the given key event on this panel
	 * @param keyEventBuilder the key event builder
	 */
	public final void removeKeyEvent(KeyEvents.Builder keyEventBuilder) {
		requireNonNull(keyEventBuilder).disable(this);
		if (editControlPanel != null) {
			keyEventBuilder.disable(editControlPanel);
		}
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + configuration.caption;
	}

	/**
	 * @return the caption used when presenting this entity panel
	 */
	public final String caption() {
		return configuration.caption;
	}

	/**
	 * @return the description used when presenting this entity panel
	 */
	public final Optional<String> description() {
		return Optional.ofNullable(configuration.description);
	}

	/**
	 * @return the icon used when presenting this entity panel
	 */
	public final Optional<ImageIcon> icon() {
		return Optional.ofNullable(configuration.icon);
	}

	/**
	 * @return an observer notified when this panel is activated
	 * @see #activate()
	 */
	public final Observer<EntityPanel> activated() {
		return activatedEvent.observer();
	}

	/**
	 * <p>Activates this panel, by initializing it, bringing its parent window to front and requesting initial focus.
	 * <p>It is up the {@link is.codion.swing.framework.ui.EntityApplicationPanel.ApplicationLayout} (for top level panels)
	 * and the {@link DetailController} (for detail panels) to make sure this panel is displayed when activated.
	 * @see #activated()
	 * @see is.codion.swing.framework.ui.EntityApplicationPanel.ApplicationLayout#activated(EntityPanel)
	 * @see DetailController#activated(EntityPanel)
	 */
	public final void activate() {
		activatedEvent.accept(this);
		initialize();
		requestInitialFocus();
	}

	/**
	 * Displays the exception in a dialog, with the dialog owner as the current focus owner
	 * or this panel if none is available.
	 * @param exception the exception to display
	 */
	public final void displayException(Exception exception) {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null) {
			focusOwner = EntityPanel.this;
		}
		Dialogs.displayException(exception, parentWindow(focusOwner));
	}

	/**
	 * @return the {@link Value} controlling the edit panel state, either {@link PanelState#HIDDEN},
	 * {@link PanelState#EMBEDDED} or {@link PanelState#WINDOW}
	 */
	public final Value<PanelState> editPanelState() {
		return editPanelState;
	}

	/**
	 * Requests focus for this panel. If an edit panel is available and not hidden, the component
	 * defined as the initial focus component by {@link EntityEditComponentPanel#focus()} gets the input focus.
	 * If no edit panel is available the table panel gets the focus, otherwise the first child
	 * component of this {@link EntityPanel} is used.
	 * @see EntityEditPanel#focus()
	 */
	public final void requestInitialFocus() {
		if (editPanel != null && editPanel.isShowing()) {
			editPanel.focus().initial().request();
		}
		else if (tablePanel != null) {
			tablePanel.table().requestFocusInWindow();
		}
		else if (getComponentCount() > 0) {
			getComponents()[0].requestFocusInWindow();
		}
		else {
			requestFocusInWindow();
		}
	}

	/**
	 * Saves user preferences for this entity panel and its detail panels.
	 * <p>Remember to call {@code super.writePreferences(preferences)} when overriding.
	 * @param preferences the Preferences instance into which to save the preferences
	 * @see #preferencesKey()
	 * @see EntityEditPanel#writePreferences(Preferences)
	 * @see EntityTablePanel#writePreferences(Preferences)
	 */
	public void writePreferences(Preferences preferences) {
		requireNonNull(preferences);
		if (containsEditPanel()) {
			editPanel.writePreferences(preferences);
		}
		if (containsTablePanel()) {
			tablePanel.writePreferences(preferences);
		}
		detailPanels.get().forEach(detailPanel -> detailPanel.writePreferences(preferences));
	}

	/**
	 * Applies any user preferences previously written via {@link #writePreferences(Preferences)}
	 * for this panel and its detail panels.
	 * <p>Remember to call {@code super.applyPreferences(preferences)} when overriding.
	 * @param preferences the Preferences instance containing the preferences to apply
	 * @see #preferencesKey()
	 * @see EntityEditPanel#applyPreferences(Preferences)
	 * @see EntityTablePanel#applyPreferences(Preferences)
	 */
	public void applyPreferences(Preferences preferences) {
		requireNonNull(preferences);
		if (containsEditPanel()) {
			editPanel.applyPreferences(preferences);
		}
		if (containsTablePanel()) {
			tablePanel.applyPreferences(preferences);
		}
		detailPanels.get().forEach(detailPanel -> detailPanel.applyPreferences(preferences));
	}

	/**
	 * @return a {@link Builder.EntityTypeStep}
	 */
	public static Builder.EntityTypeStep builder() {
		return EntityPanelBuilder.ENTITY_TYPE;
	}

	//#############################################################################################
	// Begin - initialization methods
	//#############################################################################################

	/**
	 * Initializes this EntityPanels UI.
	 * @see #detailLayout()
	 * @see #createMainComponent()
	 * @see #mainPanel()
	 */
	protected void initializeUI() {
		setLayout(borderLayout());
		add(createMainComponent(), BorderLayout.CENTER);
	}

	/**
	 * Override to set up any custom controls. This default implementation is empty.
	 * This method is called after all standard controls have been initialized.
	 * @see #control(ControlKey)
	 */
	protected void setupControls() {}

	/**
	 * Returns the key used to identify user preferences for this panel
	 * The default implementation is:
	 * {@snippet :
	 * return model().getClass().getSimpleName() + "-" + model().entityType();
	 *}
	 * Override in case this key is not unique within the application.
	 * @return the key used to identify user preferences for this panel
	 */
	protected String preferencesKey() {
		return model().getClass().getSimpleName() + "-" + model().entityType();
	}

	/**
	 * Configures the controls layout.<br>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * {@snippet :
	 *   configureControls(layout -> layout
	 *           .separator()
	 *           .control(createCustomControl()))
	 *}
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#EDIT_CONTROLS ControlKeys#EDIT_CONTROLS}
	 *   <li>{@link ControlKeys#REFRESH ControlKeys#REFRESH}
	 * </ul>
	 * @param controlsLayout provides access to the controls layout configuration
	 * @see Controls.Layout#clear()
	 */
	protected final void configureControls(Consumer<Controls.Layout> controlsLayout) {
		requireNonNull(controlsLayout).accept(this.controlsLayout);
	}

	/**
	 * Creates the main component, which is {@link #mainPanel()} in case of no detail panels
	 * or the result of {@link DetailLayout#layout()} in case of one or more detail panels.
	 * @return the main component to base this entity panel on
	 */
	protected final JComponent createMainComponent() {
		if (detailPanels.get().isEmpty()) {
			return mainPanel();
		}

		return detailLayout.layout().orElseGet(this::mainPanel);
	}

	/**
	 * @return the main panel containing the table, edit and control panels
	 */
	protected final JPanel mainPanel() {
		if (editControlPanel != null && editControlPanel.getComponents().length == 0) {
			editControlPanel.add(configuration.editBasePanel.apply(editPanel), BorderLayout.CENTER);
		}
		if (tablePanel != null && mainPanel.getComponents().length == 0) {
			mainPanel.add(tablePanel, BorderLayout.CENTER);
		}
		if (configuration.includeControls && editControlPanel != null) {
			Controls controls = controlsLayout.create(configuration.controlMap);
			if (controls.size() > 0) {
				JComponent controlComponent = configuration.controlComponent.apply(controls);
				if (controlComponent != null) {
					editControlPanel.add(controlComponent, configuration.controlComponentConstraints);
				}
			}
		}
		if (containsEditPanel()) {
			updateEditState(configuration.initialEditState);
		}

		return mainPanel;
	}

	/**
	 * Sets up the keyboard actions.
	 * @see ControlKeys
	 */
	protected final void setupKeyboardActions() {
		if (containsTablePanel()) {
			tablePanel.configuration.controlMap.keyEvent(REQUEST_TABLE_FOCUS).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			tablePanel.configuration.controlMap.keyEvent(TOGGLE_CONDITION_VIEW).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			tablePanel.configuration.controlMap.keyEvent(SELECT_CONDITION).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			tablePanel.configuration.controlMap.keyEvent(TOGGLE_FILTER_VIEW).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			tablePanel.configuration.controlMap.keyEvent(SELECT_FILTER).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			tablePanel.configuration.controlMap.keyEvent(REQUEST_SEARCH_FIELD_FOCUS).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this));
			if (editControlPanel != null) {
				tablePanel.configuration.controlMap.keyEvent(REQUEST_TABLE_FOCUS).ifPresent(keyEvent ->
								keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.enable(editControlPanel));
				tablePanel.configuration.controlMap.keyEvent(TOGGLE_CONDITION_VIEW).ifPresent(keyEvent ->
								keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.enable(editControlPanel));
				tablePanel.configuration.controlMap.keyEvent(SELECT_CONDITION).ifPresent(keyEvent ->
								keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.enable(editControlPanel));
				tablePanel.configuration.controlMap.keyEvent(TOGGLE_FILTER_VIEW).ifPresent(keyEvent ->
								keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.enable(editControlPanel));
				tablePanel.configuration.controlMap.keyEvent(SELECT_FILTER).ifPresent(keyEvent ->
								keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.enable(editControlPanel));
			}
		}
		if (editControlPanel != null && editPanel != null) {
			configuration.controlMap.keyEvent(REQUEST_EDIT_PANEL_FOCUS).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this, editControlPanel));
			editPanel.configuration.controlMap.keyEvent(SELECT_INPUT_FIELD).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this, editControlPanel));
			configuration.controlMap.keyEvent(TOGGLE_EDIT_PANEL).ifPresent(keyEvent ->
							keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.enable(this, editControlPanel));
		}
		if (configuration.keyboardNavigation) {
			setupNavigation();
		}
	}

	/**
	 * Sets up the navigation keyboard shortcuts.
	 * @see ControlKeys#NAVIGATE_UP
	 * @see ControlKeys#NAVIGATE_DOWN
	 * @see ControlKeys#NAVIGATE_LEFT
	 * @see ControlKeys#NAVIGATE_RIGHT
	 */
	protected final void setupNavigation() {
		configuration.controlMap.keyEvent(NAVIGATE_UP).ifPresent(keyEvent ->
						addKeyEvent(keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)));
		configuration.controlMap.keyEvent(NAVIGATE_DOWN).ifPresent(keyEvent ->
						addKeyEvent(keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)));
		configuration.controlMap.keyEvent(NAVIGATE_LEFT).ifPresent(keyEvent ->
						addKeyEvent(keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)));
		configuration.controlMap.keyEvent(NAVIGATE_RIGHT).ifPresent(keyEvent ->
						addKeyEvent(keyEvent.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)));
	}

	/**
	 * @return a Control instance for requesting edit panel focus
	 */
	private CommandControl createRequestEditPanelFocusControl() {
		return command(this::requestEditPanelFocus);
	}

	/**
	 * @return a Control instance for toggling the edit panel state
	 */
	private CommandControl createToggleEditPanelControl() {
		return Control.builder()
						.command(this::toggleEditPanelState)
						.icon(ICONS.editPanel())
						.description(MESSAGES.getString("toggle_edit"))
						.build();
	}

	/**
	 * @return a Control instance for refreshing the table model
	 */
	private CommandControl createRefreshTableControl() {
		return Control.builder()
						.command(tableModel().items()::refresh)
						.caption(Messages.refresh())
						.enabled(editPanel == null ? null : editPanel.active())
						.description(Messages.refreshTip() + " (ALT-" + Messages.refreshMnemonic() + ")")
						.mnemonic(Messages.refreshMnemonic())
						.icon(ICONS.refresh())
						.build();
	}

	/**
	 * Initializes the edit panel, if one is available.
	 */
	protected final void initializeEditPanel() {
		if (editPanel != null) {
			editPanel.initialize();
			configuration.controlMap.control(EDIT_CONTROLS).set(editPanel.controls());
		}
	}

	/**
	 * Initializes the table panel, if one is available.
	 */
	protected final void initializeTablePanel() {
		if (tablePanel != null) {
			tablePanel.initialize();
			if (tablePanel.table().doubleClick().isNull()) {
				tablePanel.table().doubleClick().set(command(new ShowHiddenEditPanel()));
			}
		}
	}

	/**
	 * @return the detail layout used by this panel
	 */
	protected final DetailLayout detailLayout() {
		return detailLayout;
	}

	/**
	 * @return the detail controller used by this panel
	 */
	protected final DetailController detailController() {
		return detailController;
	}

	final void writeLegacyPreferences() {
		if (containsTablePanel()) {
			tablePanel.writeLegacyPreferences();
		}
		detailPanels.get().forEach(EntityPanel::writeLegacyPreferences);
	}

	final void applyLegacyPreferences() {
		if (containsTablePanel()) {
			tablePanel.applyLegacyPreferences();
		}
		detailPanels.get().forEach(EntityPanel::applyLegacyPreferences);
	}

	private @Nullable JPanel createEditControlPanel() {
		if (editPanel == null) {
			return null;
		}

		return borderLayoutPanel()
						.minimumSize(new Dimension(0, 0))
						.mouseListener(new ActivateOnMouseClickListener())
						.build();
	}

	final void setParentPanel(EntityPanel parentPanel) {
		if (this.parentPanel != null) {
			throw new IllegalStateException("Parent panel has already been set for " + this);
		}
		this.parentPanel = requireNonNull(parentPanel);
	}

	final void linkSiblings(List<EntityPanel> entityPanels) {
		if (entityPanels.size() > 1) {
			int index = entityPanels.indexOf(this);
			if (index != -1) {
				previousPanel = entityPanels.get(index == 0 ? entityPanels.size() - 1 : index - 1);
				previousPanel.nextPanel = this;
				nextPanel = entityPanels.get(index == entityPanels.size() - 1 ? 0 : index + 1);
				nextPanel.previousPanel = this;
			}
		}
	}

	final WindowType windowType() {
		return configuration.windowType;
	}

	private void setupEditAndTablePanelControls() {
		if (containsTablePanel() && containsEditPanel() && configuration.includeToggleEditPanelControl) {
			control(TOGGLE_EDIT_PANEL).optional().ifPresent(control ->
							tablePanel.addToolBarControls(Controls.builder()
											.control(control)
											.build()));
		}
		if (containsEditPanel()) {
			editPanel.control(SELECT_INPUT_FIELD).map(control ->
							control.copy(this::selectInputComponent).build());
		}
	}

	//#############################################################################################
	// End - initialization methods
	//#############################################################################################

	private void requestEditPanelFocus() {
		if (editPanelState.is(HIDDEN)) {
			editPanelState.map(editPanelStateMapper);
		}
		editPanel().focus().initial().request();
	}

	private void selectInputComponent() {
		if (editPanelState.is(HIDDEN)) {
			editPanelState.map(editPanelStateMapper);
		}
		editPanel().selectInputComponent();
	}

	private void updateEditState(PanelState newState) {
		switch (newState) {
			case HIDDEN:
				disposeEditWindow();
				mainPanel.remove(editControlPanel);
				break;
			case EMBEDDED:
				disposeEditWindow();
				mainPanel.add(editControlPanel, configuration.editPanelContstraints);
				break;
			case WINDOW:
				displayEditWindow();
				break;
		}
		revalidate();
		requestInitialFocus();
	}

	private void displayEditWindow() {
		if (configuration.windowType == FRAME) {
			displayEditFrame(editControlPanel);
		}
		else if (configuration.windowType == DIALOG) {
			displayEditDialog(editControlPanel);
		}
	}

	private void displayEditFrame(JPanel editControlPanel) {
		editWindow = Frames.builder()
						.component(borderLayoutPanel()
										.center(editControlPanel)
										.border(Borders.emptyBorder())
										.build())
						.locationRelativeTo(tablePanel == null ? this : tablePanel)
						.title(configuration.caption)
						.icon(configuration.icon)
						.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
						.onClosed(windowEvent -> editPanelState.set(HIDDEN))
						.show();
	}

	private void displayEditDialog(JPanel editControlPanel) {
		editWindow = Dialogs.builder()
						.component(borderLayoutPanel()
										.center(editControlPanel)
										.border(Borders.emptyBorder()))
						.owner(this)
						.locationRelativeTo(tablePanel == null ? this : tablePanel)
						.title(configuration.caption)
						.icon(configuration.icon)
						.modal(false)
						.disposeOnEscape(configuration.disposeEditDialogOnEscape)
						.onClosed(windowEvent -> editPanelState.set(HIDDEN))
						.show();
	}

	private void disposeEditWindow() {
		if (editWindow != null) {
			editWindow.dispose();
			editWindow = null;
		}
	}

	private void toggleEditPanelState() {
		editPanelState.map(editPanelStateMapper);
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private static Controls.Layout createControlsLayout() {
		return Controls.layout(asList(EDIT_CONTROLS, REFRESH));
	}

	private void createControls() {
		Value.Validator<Control> controlValueValidator = control -> {
			if (initialized) {
				throw new IllegalStateException("Controls must be configured before the panel is initialized");
			}
		};
		ControlMap controlMap = configuration.controlMap;
		controlMap.controls().forEach(control -> control.addValidator(controlValueValidator));
		controlMap.control(REQUEST_EDIT_PANEL_FOCUS).set(createRequestEditPanelFocusControl());
		controlMap.control(TOGGLE_EDIT_PANEL).set(createToggleEditPanelControl());
		controlMap.control(NAVIGATE_UP).set(command(new Navigate(UP)));
		controlMap.control(NAVIGATE_DOWN).set(command(new Navigate(DOWN)));
		controlMap.control(NAVIGATE_LEFT).set(command(new Navigate(LEFT)));
		controlMap.control(NAVIGATE_RIGHT).set(command(new Navigate(RIGHT)));
		if (containsTablePanel()) {
			controlMap.control(REFRESH).set(createRefreshTableControl());
		}
	}

	private final class ShowHiddenEditPanel implements Control.Command {

		@Override
		public void execute() {
			if (containsEditPanel() && editPanelState.is(HIDDEN)) {
				editPanelState.map(editPanelStateMapper);
			}
			Window editPanelWindow = parentWindow(editControlPanel);
			if (editPanelWindow != null) {
				editPanelWindow.toFront();
			}
		}
	}

	private final class Navigate implements Control.Command {

		private final Direction direction;

		private Navigate(Direction direction) {
			this.direction = direction;
		}

		@Override
		public void execute() {
			switch (direction) {
				case LEFT:
					if (previousPanel != null) {
						previousPanel.activate();
					}
					break;
				case RIGHT:
					if (nextPanel != null) {
						nextPanel.activate();
					}
					break;
				case UP:
					if (parentPanel != null) {
						parentPanel.activate();
					}
					break;
				case DOWN:
					if (!detailPanels.get().isEmpty()) {
						navigateDown();
					}
					break;
				default:
					throw new IllegalArgumentException("Unknown direction: " + direction);
			}
		}

		private void navigateDown() {
			detailPanels.active().stream()
							.findFirst()
							.orElse(detailPanels.panels.get(0))
							.activate();
		}
	}

	/**
	 * Manages the detail panels for a {@link EntityPanel}
	 */
	public final class DetailPanels {

		private final List<EntityPanel> panels = new ArrayList<>();
		private final Event<EntityPanel> panelAdded = Event.event();

		private DetailPanels() {}

		/**
		 * Adds the given detail panels and sets this panel as their parent panel
		 * @param detailPanels the detail panels
		 * @throws IllegalStateException if the panel has already been initialized
		 * @throws IllegalArgumentException if this panel already contains a given detail panel
		 */
		public void add(EntityPanel... detailPanels) {
			for (EntityPanel detailPanel : requireNonNull(detailPanels)) {
				add(detailPanel);
			}
		}

		/**
		 * Returns all detail panels.
		 * @return the detail panels
		 */
		public Collection<EntityPanel> get() {
			return unmodifiableCollection(panels);
		}

		/**
		 * Returns the first detail panel found based on the given {@code entityType}
		 * @param entityType the entityType of the detail panel to retrieve
		 * @return the detail panel of the given type
		 * @throws IllegalArgumentException in case a panel based on the given entityType was not found
		 */
		public EntityPanel get(EntityType entityType) {
			requireNonNull(entityType);
			return panels.stream()
							.filter(detailPanel -> detailPanel.model().entityType().equals(entityType))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Detail panel for entity: " + entityType + " not found in panel: " + this));
		}

		/**
		 * Returns the detail panels which models have an active link to this panels model.
		 * @return the currently linked detail EntityPanels, if any
		 * @see ModelLink#active()
		 */
		public Collection<EntityPanel> active() {
			return panels.stream()
							.filter(detailPanel -> entityModel.detailModels().active().contains(detailPanel.entityModel))
							.collect(toList());
		}

		/**
		 * @return an {@link Observer} notified when a detail panel is added to this panel
		 * @see #add(EntityPanel...)
		 */
		public Observer<EntityPanel> added() {
			return panelAdded.observer();
		}

		/**
		 * Adds the given detail panel and sets this panel as the parent panel of the given detail panel.
		 * @param detailPanel the detail panel to add
		 * @throws IllegalStateException if the panel has already been initialized
		 * @throws IllegalArgumentException if this panel already contains the given detail panel
		 */
		private void add(EntityPanel detailPanel) {
			if (initialized) {
				throw new IllegalStateException("Detail panels must be added before the panel is initialized");
			}
			if (panels.contains(requireNonNull(detailPanel))) {
				throw new IllegalArgumentException("Panel already contains detail panel: " + detailPanel);
			}
			addDetailPanelAndLinkSiblings(detailPanel);
			detailPanel.setParentPanel(EntityPanel.this);
			panelAdded.accept(detailPanel);
		}

		private void addDetailPanelAndLinkSiblings(EntityPanel detailPanel) {
			if (!panels.isEmpty()) {
				detailPanel.previousPanel = panels.get(panels.size() - 1);
				detailPanel.previousPanel.nextPanel = detailPanel;
				detailPanel.nextPanel = panels.get(0);
				detailPanel.nextPanel.previousPanel = detailPanel;
			}
			panels.add(detailPanel);
		}
	}

	/**
	 * <p>Manages display requests for an {@link EntityPanel}.
	 * <p>Use {@link Display#request()} to request that this {@link EntityPanel} is displayed in its parent panel.
	 * {@link is.codion.swing.framework.ui.EntityApplicationPanel.ApplicationLayout} and {@link DetailLayout} implementations
	 * are responsible for responding to these requests and making sure the panel is displayed.
	 */
	public final class Display {

		private final Event<EntityPanel> request = Event.event();

		private Display() {}

		/**
		 * Requests that this panel be displayed on its parent panel and
		 * brings its parent window to the front, if one is available.
		 * @see #requested()
		 */
		public void request() {
			request.accept(EntityPanel.this);
			Window parentWindow = parentWindow(EntityPanel.this);
			if (parentWindow != null) {
				parentWindow.toFront();
			}
			Window editPanelWindow = parentWindow(editControlPanel);
			if (editPanelWindow != null) {
				editPanelWindow.toFront();
			}
		}

		/**
		 * @return an {@link Observer} notified when a display request for this panel has been issued
		 * @see #request()
		 */
		public Observer<EntityPanel> requested() {
			return request.observer();
		}
	}

	/**
	 * Contains configuration settings for a {@link EntityPanel} which must be set before the panel is initialized.
	 */
	public static final class Config {

		/**
		 * Indicates whether keyboard navigation will be enabled
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> KEYBOARD_NAVIGATION =
						booleanValue(EntityPanel.class.getName() + ".keyboardNavigation", true);

		/**
		 * Indicates whether entity edit panel dialogs should be closed on escape
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> DISPOSE_EDIT_DIALOG_ON_ESCAPE =
						booleanValue(EntityPanel.class.getName() + ".disposeEditDialogOnEscape", true);

		/**
		 * Specifies whether a control for toggling the edit panel is available to the user
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_TOGGLE_EDIT_PANEL_CONTROL =
						booleanValue(EntityPanel.class.getName() + ".includeToggleEditPanelControl", true);

		/**
		 * Specifies whether the edit controls (Save, update, delete, clear, refresh) should be on a toolbar instead of a button panel
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: false
		 * </ul>
		 */
		public static final PropertyValue<Boolean> TOOLBAR_CONTROLS =
						booleanValue(EntityPanel.class.getName() + ".toolbarControls", false);

		/**
		 * Specifies how detail and edit panels should be displayed.
		 * <ul>
		 * <li>Value type: {@link WindowType}
		 * <li>Default value: {@link WindowType#DIALOG}
		 * </ul>
		 */
		public static final PropertyValue<WindowType> WINDOW_TYPE =
						enumValue(EntityPanel.class.getName() + ".windowType", WindowType.class, DIALOG);

		/**
		 * Specifies where the control panel should be placed in a BorderLayout
		 * <ul>
		 * <li>Value type: String
		 * <li>Default value: {@link BorderLayout#EAST}
		 * </ul>
		 * @see #TOOLBAR_CONTROLS
		 */
		public static final PropertyValue<String> CONTROL_PANEL_CONSTRAINTS =
						stringValue(EntityPanel.class.getName() + ".controlPanelConstraints", BorderLayout.EAST);

		/**
		 * Specifies where the control toolbar should be placed in a BorderLayout
		 * <ul>
		 * <li>Value type: String
		 * <li>Default value: BorderLayout.WEST
		 * </ul>
		 * @see #TOOLBAR_CONTROLS
		 */
		public static final PropertyValue<String> CONTROL_TOOLBAR_CONSTRAINTS =
						stringValue(EntityPanel.class.getName() + ".controlToolBarConstraints", BorderLayout.WEST);

		/**
		 * Specifies where the edit panel should be placed in a BorderLayout
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: BorderLayout.NORTH
		 * </ul>
		 */
		public static final PropertyValue<String> EDIT_PANEL_CONSTRAINTS =
						stringValue(EntityPanel.class.getName() + ".editPanelConstraints", BorderLayout.NORTH);

		/**
		 * Specifies whether entity panels should include controls by default
		 * <ul>
		 * <li>Value type: Boolean
		 * <li>Default value: true
		 * </ul>
		 */
		public static final PropertyValue<Boolean> INCLUDE_CONTROLS =
						booleanValue(EntityPanel.class.getName() + ".includeControls", true);

		private final EntityPanel entityPanel;
		private final ControlMap controlMap;
		private final Set<PanelState> enabledEditStates;

		private Function<EntityPanel, DetailLayout> detailLayout = new DefaultDetailLayout();
		private Function<Controls, JComponent> controlComponent = new DefaultControlComponent();
		private Function<EntityEditPanel, JPanel> editBasePanel = new DefaultEditBasePanel();
		private boolean disposeEditDialogOnEscape = DISPOSE_EDIT_DIALOG_ON_ESCAPE.getOrThrow();
		private boolean toolbarControls = TOOLBAR_CONTROLS.getOrThrow();
		private boolean includeToggleEditPanelControl = INCLUDE_TOGGLE_EDIT_PANEL_CONTROL.getOrThrow();
		private String controlComponentConstraints = TOOLBAR_CONTROLS.getOrThrow() ?
						CONTROL_TOOLBAR_CONSTRAINTS.getOrThrow() : CONTROL_PANEL_CONSTRAINTS.getOrThrow();
		private boolean includeControls = INCLUDE_CONTROLS.getOrThrow();
		private boolean keyboardNavigation = KEYBOARD_NAVIGATION.getOrThrow();
		private WindowType windowType = WINDOW_TYPE.getOrThrow();
		private PanelState initialEditState = EMBEDDED;
		private String editPanelContstraints = EDIT_PANEL_CONSTRAINTS.getOrThrow();
		private Border border = createEmptyBorder(Layouts.GAP.getOrThrow(), 0, 0, 0);

		private String caption;
		private @Nullable String description;
		private @Nullable ImageIcon icon;

		private Config(EntityPanel entityPanel) {
			this.entityPanel = entityPanel;
			this.controlMap = controlMap(ControlKeys.class);
			this.enabledEditStates = new LinkedHashSet<>(asList(PanelState.values()));
			this.caption = entityPanel.model().entityDefinition().caption();
			this.description = entityPanel.model().entityDefinition().description().orElse(null);
		}

		private Config(Config config) {
			this.entityPanel = config.entityPanel;
			this.controlMap = config.controlMap.copy();
			this.enabledEditStates = new LinkedHashSet<>(config.enabledEditStates);
			this.detailLayout = config.detailLayout;
			this.controlComponent = config.controlComponent;
			this.editBasePanel = config.editBasePanel;
			this.toolbarControls = config.toolbarControls;
			this.includeToggleEditPanelControl = config.includeToggleEditPanelControl;
			this.controlComponentConstraints = config.controlComponentConstraints;
			this.includeControls = config.includeControls;
			this.keyboardNavigation = config.keyboardNavigation;
			this.initialEditState = config.initialEditState;
			this.caption = config.caption;
			this.description = config.description;
			this.icon = config.icon;
			this.disposeEditDialogOnEscape = config.disposeEditDialogOnEscape;
			this.windowType = config.windowType;
			this.editPanelContstraints = config.editPanelContstraints;
			this.border = config.border;
		}

		/**
		 * @return the entity panel
		 */
		public EntityPanel entityPanel() {
			return entityPanel;
		}

		/**
		 * @param caption the caption to use when presenting this panel
		 * @return this Config instance
		 */
		public Config caption(String caption) {
			this.caption = requireNonNull(caption);
			return this;
		}

		/**
		 * @param description the description to use when presenting this panel
		 * @return this Config instance
		 */
		public Config description(String description) {
			this.description = requireNonNull(description);
			return this;
		}

		/**
		 * @param icon the icon to use when presenting this panel
		 * @return this Config instance
		 */
		public Config icon(ImageIcon icon) {
			this.icon = requireNonNull(icon);
			return this;
		}

		/**
		 * @param detailLayout provides the detail panel layout
		 * @return this Config instance
		 */
		public Config detailLayout(Function<EntityPanel, DetailLayout> detailLayout) {
			this.detailLayout = requireNonNull(detailLayout);
			return this;
		}

		/**
		 * Creates the component to place next to the edit panel, containing the available controls,
		 * such as insert, update, delete, clear and refresh.
		 * @param controlComponent creates the controls panel
		 * @return this Config instance
		 * @see #control(ControlKey)
		 * @see EntityEditPanel#controls()
		 * @see #configureControls(Consumer)
		 * @see Config#TOOLBAR_CONTROLS
		 * @see Config#CONTROL_PANEL_CONSTRAINTS
		 * @see Config#CONTROL_TOOLBAR_CONSTRAINTS
		 * @see Config#includeControls(boolean)
		 */
		public Config controlComponent(Function<Controls, JComponent> controlComponent) {
			this.controlComponent = requireNonNull(controlComponent);
			return this;
		}

		/**
		 * Creates a base panel containing the given edit panel.
		 * The default layout is a {@link FlowLayout} with the alignment depending on {@link Config#controlComponentConstraints(String)}.
		 * @param editBasePanel creates the edit base panel
		 * @return this Config instance
		 * @see Config#controlComponentConstraints(String)
		 */
		public Config editBasePanel(Function<EntityEditPanel, JPanel> editBasePanel) {
			this.editBasePanel = requireNonNull(editBasePanel);
			return this;
		}

		/**
		 * Overridden by {@link #controlComponent(Function)}.
		 * @param toolbarControls true if the edit controls should be on a toolbar instead of a button panel
		 * @return this Config instance
		 * @see #TOOLBAR_CONTROLS
		 */
		public Config toolbarControls(boolean toolbarControls) {
			this.toolbarControls = toolbarControls;
			return this;
		}

		/**
		 * Sets the layout constraints to use for the control panel.
		 * <pre>
		 * The default layout is as follows (BorderLayout.WEST):
		 * __________________________________
		 * |   edit panel           |control|
		 * |  (EntityEditPanel)     | panel | } editControlPanel
		 * |________________________|_______|
		 *
		 * With (BorderLayout.SOUTH):
		 * __________________________
		 * |         edit           |
		 * |        panel           |
		 * |________________________| } editControlPanel
		 * |     control panel      |
		 * |________________________|
		 *
		 * etc.
		 * </pre>
		 * @param controlComponentConstraints the controls component layout constraints (BorderLayout constraints)
		 * @return this Config instance
		 * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
		 */
		public Config controlComponentConstraints(String controlComponentConstraints) {
			this.controlComponentConstraints = validateBorderLayoutConstraints(controlComponentConstraints);
			return this;
		}

		/**
		 * @param editPanelConstraints the edit panel constraints
		 * @return this Config instance
		 * @throws IllegalArgumentException in case the given constraint is not one of BorderLayout.SOUTH, NORTH, EAST or WEST
		 */
		public Config editPanelConstraints(String editPanelConstraints) {
			this.editPanelContstraints = validateBorderLayoutConstraints(editPanelConstraints);
			return this;
		}

		/**
		 * By default, the main panel uses an empty border, with a top inset of size {@link Layouts#GAP}.
		 * @param border specifies the border to use around the main panel
		 * @return this Config instance
		 */
		public EntityPanel.Config border(Border border) {
			this.border = border;
			return this;
		}

		/**
		 * @param includeToggleEditPanelControl true if a control for toggling the edit panel should be included
		 * @return this Config instance
		 */
		public Config includeToggleEditPanelControl(boolean includeToggleEditPanelControl) {
			this.includeToggleEditPanelControl = includeToggleEditPanelControl;
			return this;
		}

		/**
		 * @param includeControls true if the edit and table panel controls should be included
		 * @return this Config instance
		 */
		public Config includeControls(boolean includeControls) {
			this.includeControls = includeControls;
			return this;
		}

		/**
		 * @param keyboardNavigation true if keyboard navigation should be enabled
		 * @return this Config instance
		 */
		public Config keyboardNavigation(boolean keyboardNavigation) {
			this.keyboardNavigation = keyboardNavigation;
			return this;
		}

		/**
		 * @param windowType the window type to use when presenting panels
		 * @return this Config instance
		 * @see Config#WINDOW_TYPE
		 */
		public Config windowType(WindowType windowType) {
			this.windowType = requireNonNull(windowType);
			return this;
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
		 * Default {@link PanelState#EMBEDDED}
		 * @param initialState the initial edit panel state
		 * @return this Config instance
		 * @throws IllegalArgumentException in case the given state is {@link PanelState#WINDOW}
		 * @throws IllegalArgumentException in case the given state is not enabled
		 * @see #enabledEditStates(PanelState...)
		 */
		public Config initialEditState(PanelState initialState) {
			if (requireNonNull(initialState) == WINDOW) {
				throw new IllegalArgumentException(WINDOW + " is not a supported initial state");
			}
			if (!enabledEditStates.contains(requireNonNull(initialState))) {
				throw new IllegalArgumentException("Edit panel state: " + initialState + " is not enabled");
			}
			this.initialEditState = initialState;
			return this;
		}

		/**
		 * Sets the enabled edit panel states.
		 * @param editStates the enabled states
		 * @return this Config instance
		 * @throws IllegalArgumentException in case the given states do not include the initial state
		 * @throws IllegalArgumentException in case no {@code editStates} are specified
		 * @see #initialEditState(PanelState)
		 */
		public Config enabledEditStates(PanelState... editStates) {
			if (requireNonNull(editStates).length == 0) {
				throw new IllegalArgumentException("No edit panel states specified");
			}
			List<PanelState> states = asList(editStates);
			if (!states.contains(initialEditState)) {
				throw new IllegalArgumentException("Initial edit state has already been set to: " + initialEditState);
			}
			this.enabledEditStates.clear();
			this.enabledEditStates.addAll(asList(editStates));
			return this;
		}

		/**
		 * @param disposeEditDialogOnEscape specifies whether entity edit panel dialogs should be closed on escape
		 * @return this Config instance
		 */
		public Config disposeEditDialogOnEscape(boolean disposeEditDialogOnEscape) {
			this.disposeEditDialogOnEscape = disposeEditDialogOnEscape;
			return this;
		}

		private boolean horizontalControlLayout() {
			return controlComponentConstraints.equals(BorderLayout.SOUTH) ||
							controlComponentConstraints.equals(BorderLayout.NORTH);
		}

		private static String validateBorderLayoutConstraints(String constraints) {
			switch (requireNonNull(constraints)) {
				case BorderLayout.SOUTH:
				case BorderLayout.NORTH:
				case BorderLayout.EAST:
				case BorderLayout.WEST:
					break;
				default:
					throw new IllegalArgumentException("Constraints must be one of BorderLayout.SOUTH, NORTH, EAST or WEST");
			}

			return constraints;
		}

		private static final class DefaultDetailLayout implements Function<EntityPanel, DetailLayout> {

			@Override
			public DetailLayout apply(EntityPanel entityPanel) {
				return TabbedDetailLayout.builder()
								.panel(entityPanel)
								.build();
			}
		}

		private final class DefaultControlComponent implements Function<Controls, JComponent> {

			@Override
			public JComponent apply(Controls controls) {
				requireNonNull(controls);

				return toolbarControls ? createControlToolBar(controls) : createControlPanel(controls);
			}

			private JToolBar createControlToolBar(Controls controls) {
				return toolBar()
								.controls(controls)
								.orientation(horizontalControlLayout() ? HORIZONTAL : VERTICAL)
								.build();
			}

			private JPanel createControlPanel(Controls controls) {
				if (horizontalControlLayout()) {
					return flowLayoutPanel(FlowLayout.CENTER)
									.add(buttonPanel()
													.controls(controls))
									.build();
				}

				return borderLayoutPanel()
								.north(buttonPanel()
												.controls(controls)
												.orientation(VERTICAL)
												.button(button -> button.horizontalAlignment(SwingConstants.LEADING)))
								.build();
			}
		}

		private final class DefaultEditBasePanel implements Function<EntityEditPanel, JPanel> {

			@Override
			public JPanel apply(EntityEditPanel editPanel) {
				return panel()
								.layout(new FlowLayout(horizontalControlLayout() ? FlowLayout.CENTER : FlowLayout.LEADING, 0, 0))
								.add(editPanel)
								.build();
			}
		}
	}

	/**
	 * Handles the layout of a EntityPanel with one or more detail panels.
	 * @see #NONE
	 */
	public interface DetailLayout {

		/**
		 * A convenience instance for indicating no detail layout.
		 */
		Function<EntityPanel, DetailLayout> NONE = entityPanel -> new DetailLayout() {};

		/**
		 * Updates the UI of all associated components.
		 * Override to update the UI of components that may be hidden and
		 * therefore not updated along with the component tree.
		 */
		default void updateUI() {}

		/**
		 * Lays out a given EntityPanel along with its detail panels.
		 * In case of no special detail panel layout requirements, this method should return an empty Optional.
		 * @return the panel laid out with it detail panels or an empty Optional in case of no special layout component.
		 * @throws IllegalStateException in case the panel has no detail panels or if it has already been laid out
		 */
		default Optional<JComponent> layout() {
			return Optional.empty();
		}

		/**
		 * @return the detail controller for this layout, an empty Optional if none is available
		 */
		default Optional<DetailController> controller() {
			return Optional.empty();
		}
	}

	/**
	 * Controls the detail panels for a EntityPanel instance.
	 * Provides a way to change the {@link PanelState} and to
	 * respond to detail panel activation.
	 */
	public interface DetailController {

		/**
		 * Note that the detail panel state may be shared between detail panels,
		 * as they may be displayed in a shared window.
		 * @param detailPanel the detail panel
		 * @return the {@link Value} controlling the state of the given detail panel
		 */
		default Value<PanelState> panelState(EntityPanel detailPanel) {
			throw new UnsupportedOperationException("panelState() has not been implemented for detail controller: " + getClass());
		}

		/**
		 * Called when the given detail panel should be displayed,
		 * responsible for making sure it becomes visible.
		 * @param detailPanel the detail panel to display
		 * @see EntityPanel.Display#requested()
		 */
		default void activated(EntityPanel detailPanel) {}
	}

	/**
	 * A builder for {@link EntityPanel} instances.
	 */
	public interface Builder {

		/**
		 * Provides a {@link PanelBuilder}
		 */
		interface EntityTypeStep {

			/**
			 * @param entityType the entity type to base this panel builder on
			 * @return a {@link PanelBuilder}
			 */
			PanelBuilder entityType(EntityType entityType);
		}

		/**
		 * Provides a {@link Builder}
		 */
		interface PanelBuilder {

			/**
			 * @param entityPanel provides the {@link EntityPanel}
			 * @return a {@link Builder}
			 */
			Builder panel(Function<EntityConnectionProvider, EntityPanel> entityPanel);
		}

		/**
		 * @return the entityType
		 */
		EntityType entityType();

		/**
		 * @param caption the panel caption
		 * @return this builder instance
		 */
		Builder caption(@Nullable String caption);

		/**
		 * @return the caption, an empty Optional if none has been set
		 */
		Optional<String> caption();

		/**
		 * @param description the panel description
		 * @return this builder instance
		 */
		Builder description(@Nullable String description);

		/**
		 * @return the description, an empty Optional if none has been set
		 */
		Optional<String> description();

		/**
		 * @param icon the panel icon
		 * @return this builder instance
		 */
		Builder icon(@Nullable ControlIcon icon);

		/**
		 * @return the icon, an empty Optional if none has been set
		 */
		Optional<ControlIcon> icon();

		/**
		 * Builds an {@link EntityPanel} based on this builder configuration.
		 * @param connectionProvider the connection provider
		 * @return an {@link EntityPanel} based on this builder
		 */
		EntityPanel build(EntityConnectionProvider connectionProvider);
	}

	private final class ActivateOnMouseClickListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			editPanel.focus().afterUpdate().request();
		}
	}

	static UnaryOperator<PanelState> panelStateMapper(Set<PanelState> states) {
		return new PanelStateMapper(states);
	}

	private static final class PanelStateMapper implements UnaryOperator<PanelState> {

		private final List<PanelState> states;

		private PanelStateMapper(Set<PanelState> states) {
			this.states = new ArrayList<>(states);
		}

		@Override
		public PanelState apply(PanelState state) {
			int index = states.indexOf(state);
			if (index < 0) {
				throw new IllegalArgumentException("Invalid PanelState: " + state);
			}
			if (index == states.size() - 1) {
				return states.get(0);
			}

			return states.get(index + 1);
		}
	}
}
