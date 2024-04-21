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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel.TableControl;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

import static is.codion.swing.common.ui.Utilities.parentWindow;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.button.ToggleButtonType.CHECKBOX;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.framework.ui.EntityPanel.Direction.*;
import static is.codion.swing.framework.ui.EntityPanel.KeyboardShortcut.*;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.*;
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.SwingConstants.HORIZONTAL;
import static javax.swing.SwingConstants.VERTICAL;

/**
 * A panel representing an Entity via a EntityModel, which facilitates browsing and editing of records.
 * <pre>
 *   EntityType entityType = ...;
 *   EntityConnectionProvider connectionProvider = ...;
 *   SwingEntityModel entityModel = new SwingEntityModel(entityType, connectionProvider);
 *   EntityPanel entityPanel = new EntityPanel(entityModel);
 *   entityPanel.initialize();
 *   JFrame frame = new JFrame();
 *   frame.add(entityPanel);
 *   frame.pack();
 *   frame.setVisible(true);
 * </pre>
 */
public class EntityPanel extends JPanel {

	private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityPanel.class.getName());
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	/**
	 * The possible states of a detail or edit panel.
	 */
	public enum PanelState {
		WINDOW, EMBEDDED, HIDDEN
	}

	/**
	 * The navigation directions.
	 */
	public enum Direction {
		UP, DOWN, RIGHT, LEFT
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
	 * The keyboard shortcuts available for {@link EntityPanel}s.
	 * Note that changing the shortcut keystroke after the panel
	 * has been initialized has no effect.
	 */
	public enum KeyboardShortcut implements KeyboardShortcuts.Shortcut {
		/**
		 * Requests focus for the table.<br>
		 * Default: CTRL-T
		 */
		REQUEST_TABLE_FOCUS(keyStroke(VK_T, CTRL_DOWN_MASK)),
		/**
		 * Toggles the condition panel between hidden, visible and advanced.<br>
		 * Default: CTRL-ALT-S
		 */
		TOGGLE_CONDITION_PANEL(keyStroke(VK_S, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Displays a dialog for selecting a column condition panel.<br>
		 * Default: CTRL-S
		 */
		SELECT_CONDITION_PANEL(keyStroke(VK_S, CTRL_DOWN_MASK)),
		/**
		 * Toggles the filter panel between hidden, visible and advanced.<br>
		 * Default: CTRL-ALT-F
		 */
		TOGGLE_FILTER_PANEL(keyStroke(VK_F, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Displays a dialog for selecting a column filter panel.<br>
		 * Default: CTRL-SHIFT-F
		 */
		SELECT_FILTER_PANEL(keyStroke(VK_F, CTRL_DOWN_MASK | SHIFT_DOWN_MASK)),
		/**
		 * Requests focus for the table search field.<br>
		 * Default: CTRL-F
		 */
		REQUEST_SEARCH_FIELD_FOCUS(keyStroke(VK_F, CTRL_DOWN_MASK)),
		/**
		 * Requests focus for the edit panel (intial focus component).<br>
		 * Default: CTRL-E
		 */
		REQUEST_EDIT_PANEL_FOCUS(keyStroke(VK_E, CTRL_DOWN_MASK)),
		/**
		 * Displays a dialog for selecting an input field.<br>
		 * Default: CTRL-I
		 */
		SELECT_INPUT_FIELD(keyStroke(VK_I, CTRL_DOWN_MASK)),
		/**
		 * Toggles the edit panel between hidden, embedded and dialog.<br>
		 * Default: CTRL-ALT-E
		 */
		TOGGLE_EDIT_PANEL(keyStroke(VK_E, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Navigates to the parent panel, if one is available.<br>
		 * Default: CTRL-ALT-UP ARROW
		 */
		NAVIGATE_UP(keyStroke(VK_UP, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Navigates to the selected child panel, if one is available.<br>
		 * Default: CTRL-ALT-DOWN ARROW
		 */
		NAVIGATE_DOWN(keyStroke(VK_DOWN, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Navigates to the sibling panel on the right, if one is available.<br>
		 * Default: CTRL-ALT-RIGHT ARROW
		 */
		NAVIGATE_RIGHT(keyStroke(VK_RIGHT, CTRL_DOWN_MASK | ALT_DOWN_MASK)),
		/**
		 * Navigates to the sibling panel on the left, if one is available.<br>
		 * Default: CTRL-ALT-LEFT ARROW
		 */
		NAVIGATE_LEFT(keyStroke(VK_LEFT, CTRL_DOWN_MASK | ALT_DOWN_MASK));

		private final KeyStroke defaultKeystroke;

		KeyboardShortcut(KeyStroke defaultKeystroke) {
			this.defaultKeystroke = defaultKeystroke;
		}

		@Override
		public KeyStroke defaultKeystroke() {
			return defaultKeystroke;
		}
	}

	private static final Consumer<Config> NO_CONFIGURATION = c -> {};

	/**
	 * Specifies the mapping between {@link PanelState} instances: From HIDDEN to EMBEDDED to WINDOW back to HIDDEN
	 */
	static final Function<PanelState, PanelState> PANEL_STATE_MAPPER = new PanelStateMapper();

	private final SwingEntityModel entityModel;
	private final List<EntityPanel> detailPanels = new ArrayList<>();
	private final EntityEditPanel editPanel;
	private final EntityTablePanel tablePanel;
	private final JPanel editControlPanel;
	private final JPanel mainPanel;
	private final DetailLayout detailLayout;
	private final DetailController detailController;
	private final Event<EntityPanel> activateEvent = Event.event();
	private final Value<PanelState> editPanelState;

	private final Config configuration;

	private EntityPanel parentPanel;
	private EntityPanel previousSiblingPanel;
	private EntityPanel nextSiblingPanel;

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
	 * @param configuration provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, Consumer<Config> configuration) {
		this(requireNonNull(entityModel), null, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, configuration);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel) {
		this(requireNonNull(entityModel), editPanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param configuration provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, Consumer<Config> configuration) {
		this(requireNonNull(entityModel), editPanel, entityModel.containsTableModel() ? new EntityTablePanel(entityModel.tableModel()) : null, configuration);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param tablePanel the table panel
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel) {
		this(entityModel, tablePanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param tablePanel the table panel
	 * @param configuration provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityTablePanel tablePanel, Consumer<Config> configuration) {
		this(entityModel, null, tablePanel, configuration);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param tablePanel the table panel
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel) {
		this(entityModel, editPanel, tablePanel, NO_CONFIGURATION);
	}

	/**
	 * Instantiates a new EntityPanel instance. The panel is not laid out and initialized until {@link #initialize()} is called.
	 * @param entityModel the EntityModel
	 * @param editPanel the edit panel
	 * @param tablePanel the table panel
	 * @param configuration provides access to the panel configuration
	 */
	public EntityPanel(SwingEntityModel entityModel, EntityEditPanel editPanel, EntityTablePanel tablePanel,
										 Consumer<Config> configuration) {
		requireNonNull(entityModel, "entityModel");
		this.entityModel = entityModel;
		this.editPanel = editPanel;
		this.tablePanel = tablePanel;
		this.editControlPanel = createEditControlPanel();
		this.mainPanel = borderLayoutPanel().build();
		this.configuration = configure(configuration);
		this.detailLayout = this.configuration.detailLayout.apply(this);
		this.detailController = detailLayout.controller().orElse(new DetailController() {});
		this.editPanelState = Value.nonNull(EMBEDDED)
						.initialValue(this.configuration.editPanelState)
						.listener(this::updateEditPanelState)
						.build();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateUI(editControlPanel, mainPanel, tablePanel, editPanel);
		if (detailPanels != null) {
			Utilities.updateUI(detailPanels);
		}
		if (detailLayout != null) {
			detailLayout.updateUI();
		}
	}

	/**
	 * @param <T> the model type
	 * @return the EntityModel
	 */
	public final <T extends SwingEntityModel> T model() {
		return (T) entityModel;
	}

	/**
	 * @param <T> the edit model type
	 * @return the EntityEditModel
	 */
	public final <T extends SwingEntityEditModel> T editModel() {
		return entityModel.editModel();
	}

	/**
	 * @param <T> the table model type
	 * @return the EntityTableModel
	 * @throws IllegalStateException in case no table model is available
	 */
	public final <T extends SwingEntityTableModel> T tableModel() {
		return entityModel.tableModel();
	}

	/**
	 * @return the parent panel or an empty Optional in case of a root panel
	 */
	public final Optional<EntityPanel> parentPanel() {
		return Optional.ofNullable(parentPanel);
	}

	/**
	 * @param detailPanels the detail panels
	 * @throws IllegalStateException if the panel has already been initialized
	 * @throws IllegalArgumentException if this panel already contains a given detail panel
	 */
	public final void addDetailPanels(EntityPanel... detailPanels) {
		requireNonNull(detailPanels, "detailPanels");
		for (EntityPanel detailPanel : detailPanels) {
			addDetailPanel(detailPanel);
		}
	}

	/**
	 * Adds the given detail panel and sets this panel as the parent panel of the given detail panel.
	 * @param detailPanel the detail panel to add
	 * @throws IllegalStateException if the panel has already been initialized
	 * @throws IllegalArgumentException if this panel already contains the given detail panel
	 */
	public final void addDetailPanel(EntityPanel detailPanel) {
		throwIfInitialized();
		if (detailPanels.contains(requireNonNull(detailPanel))) {
			throw new IllegalArgumentException("Panel already contains detail panel: " + detailPanel);
		}
		addEntityPanelAndLinkSiblings(detailPanel, detailPanels);
		detailPanel.setParentPanel(this);
	}

	/**
	 * Initializes this EntityPanel, in case of some specific initialization code you can override the
	 * {@link #initializeUI()} method and add your code there. Calling this method a second time has no effect.
	 * @param <T> the entity panel type
	 * @return this EntityPanel instance
	 */
	public final <T extends EntityPanel> T initialize() {
		if (!initialized) {
			try {
				setFocusCycleRoot(true);
				setupToggleEditPanelControl();
				initializeUI();
				initializeEditPanel();
				initializeTablePanel();
				setupKeyboardActions();
			}
			finally {
				initialized = true;
			}
		}

		return (T) this;
	}

	/**
	 * @param <T> the edit panel type
	 * @return the edit panel
	 * @throws IllegalStateException in case no edit panel is available
	 * @see #containsEditPanel()
	 */
	public final <T extends EntityEditPanel> T editPanel() {
		if (editPanel == null) {
			throw new IllegalStateException("No edit panel available");
		}

		return (T) editPanel;
	}

	/**
	 * @return true if this panel contains a edit panel.
	 */
	public final boolean containsEditPanel() {
		return editPanel != null;
	}

	/**
	 * @param <T> the table panel type
	 * @return the table panel
	 * @throws IllegalStateException in case no table panel is available
	 * @see #containsTablePanel()
	 */
	public final <T extends EntityTablePanel> T tablePanel() {
		if (tablePanel == null) {
			throw new IllegalStateException("No table panel available");
		}

		return (T) tablePanel;
	}

	/**
	 * @return true if this panel contains a table panel.
	 */
	public final boolean containsTablePanel() {
		return tablePanel != null;
	}

	/**
	 * Enables the given key event on this panel
	 * @param keyEventBuilder the key event builder
	 */
	public final void addKeyEvent(KeyEvents.Builder keyEventBuilder) {
		requireNonNull(keyEventBuilder);
		keyEventBuilder.enable(this);
		if (containsEditPanel()) {
			keyEventBuilder.enable(editControlPanel);
		}
	}

	/**
	 * Disables the given key event on this panel
	 * @param keyEventBuilder the key event builder
	 */
	public final void removeKeyEvent(KeyEvents.Builder keyEventBuilder) {
		requireNonNull(keyEventBuilder);
		keyEventBuilder.disable(this);
		if (containsEditPanel()) {
			keyEventBuilder.disable(editControlPanel);
		}
	}

	/**
	 * Returns the detail panels which models are active.
	 * @return the currently active detail EntityPanels, if any
	 */
	public final Collection<EntityPanel> activeDetailPanels() {
		return detailPanels.stream()
						.filter(detailPanel -> entityModel.activeDetailModels().contains(detailPanel.entityModel))
						.collect(toList());
	}

	/**
	 * Returns the first detail panel found based on the given {@code entityType}
	 * @param <T> the entity panel type
	 * @param entityType the entityType of the detail panel to retrieve
	 * @return the detail panel of the given type
	 * @throws IllegalArgumentException in case a panel based on the given entityType was not found
	 */
	public final <T extends EntityPanel> T detailPanel(EntityType entityType) {
		requireNonNull(entityType);
		return (T) detailPanels.stream()
						.filter(detailPanel -> detailPanel.model().entityType().equals(entityType))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Detail panel for entity: " + entityType + " not found in panel: " + this));
	}

	/**
	 * Returns all detail panels.
	 * @return the detail panels
	 */
	public final Collection<EntityPanel> detailPanels() {
		return unmodifiableCollection(detailPanels);
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
	 * @return an observer notified before this panel is activated
	 * @see #activate()
	 */
	public final EventObserver<EntityPanel> activateEvent() {
		return activateEvent.observer();
	}

	/**
	 * Activates this panel, by initializing it, bringing its parent window to front and requesting initial focus.
	 * It is up the panel or application layout to make sure this panel is made visible when activated.
	 * @see #activateEvent()
	 */
	public final void activate() {
		activateEvent.accept(this);
		initialize();
		Window parentWindow = parentWindow(this);
		if (parentWindow != null) {
			parentWindow.toFront();
		}
		Window editPanelWindow = parentWindow(editControlPanel);
		if (editPanelWindow != null) {
			editPanelWindow.toFront();
		}
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
		Dialogs.displayExceptionDialog(exception, parentWindow(focusOwner));
	}

	/**
	 * @return the value controlling the edit panel state, either HIDDEN, EMBEDDED or WINDOW
	 */
	public final Value<PanelState> editPanelState() {
		return editPanelState;
	}

	/**
	 * Requests focus for this panel. If an edit panel is available and not hidden, the component
	 * defined as the initialFocusComponent gets the input focus.
	 * If no edit panel is available the table panel gets the focus, otherwise the first child
	 * component of this EntityPanel is used.
	 * @see EntityEditPanel#initialFocusComponent()
	 */
	public final void requestInitialFocus() {
		if (editPanel != null && editPanel.isShowing()) {
			editPanel.requestInitialFocus();
		}
		else if (tablePanel != null) {
			tablePanel.table().requestFocus();
		}
		else if (getComponentCount() > 0) {
			getComponents()[0].requestFocus();
		}
		else {
			requestFocus();
		}
	}

	/**
	 * Saves any user preferences for all entity panels and associated elements
	 */
	public void savePreferences() {
		detailPanels.forEach(EntityPanel::savePreferences);
	}

	/**
	 * Instantiates a new {@link EntityPanel.Builder}
	 * @param entityType the entity type to base this panel builder on
	 * @return a panel builder
	 */
	public static EntityPanel.Builder builder(EntityType entityType) {
		return new EntityPanelBuilder(SwingEntityModel.builder(entityType));
	}

	/**
	 * Instantiates a new {@link EntityPanel.Builder}
	 * @param modelBuilder the {@link SwingEntityModel.Builder} to base this panel builder on
	 * @return a panel builder
	 */
	public static EntityPanel.Builder builder(SwingEntityModel.Builder modelBuilder) {
		return new EntityPanelBuilder(modelBuilder);
	}

	/**
	 * Instantiates a new {@link EntityPanel.Builder}
	 * @param model the {@link SwingEntityModel} to base this panel builder on
	 * @return a panel builder
	 */
	public static EntityPanel.Builder builder(SwingEntityModel model) {
		return new EntityPanelBuilder(model);
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
	 * Creates a base panel containing the given edit panel.
	 * The default layout is a {@link FlowLayout} with the alignment depending on {@link Config#controlComponentConstraints(String)}.
	 * @param editPanel the initialized edit panel
	 * @return a base panel for the edit panel
	 * @see Config#controlComponentConstraints(String)
	 */
	protected JPanel createEditBasePanel(EntityEditPanel editPanel) {
		return panel(new FlowLayout(configuration.horizontalControlLayout() ? FlowLayout.CENTER : FlowLayout.LEADING, 0, 0))
						.add(editPanel)
						.build();
	}

	/**
	 * Creates the component to place next to the edit panel, containing the available controls,
	 * such as insert, update, delete, clear and refresh.
	 * @param controls the controls to display on the component
	 * @return the component containing the edit and table panel controls, null if no controls are available
	 * @see EntityEditPanel#controls()
	 * @see #createControls()
	 * @see Config#TOOLBAR_CONTROLS
	 * @see Config#CONTROL_PANEL_CONSTRAINTS
	 * @see Config#CONTROL_TOOLBAR_CONSTRAINTS
	 * @see Config#includeControls(boolean)
	 */
	protected JComponent createControlComponent(Controls controls) {
		if (requireNonNull(controls).empty()) {
			return null;
		}

		return configuration.toolbarControls ? createControlToolBar(controls) : createControlPanel(controls);
	}

	/**
	 * Creates the {@link Controls} instance on which to base the controls component.
	 * By default all controls from {@link EntityEditPanel#controls()} are included and if a
	 * table panel is available a table refresh control is included as well.
	 * Override to customize the controls presented on this panel.
	 * @return the control component controls, an empty {@link Controls} instance in case of no controls.
	 * @see #createControlComponent(Controls)
	 */
	protected Controls createControls() {
		Controls controls = Controls.controls();
		if (containsEditPanel()) {
			controls.addAll(editPanel().controls());
		}
		if (containsTablePanel()) {
			controls.add(createRefreshTableControl());
		}

		return controls;
	}

	/**
	 * Creates the main component, which is {@link #mainPanel()} in case of no detail panels
	 * or the result of {@link DetailLayout#layout()} in case of one or more detail panels.
	 * @return the main component to base this entity panel on
	 */
	protected final JComponent createMainComponent() {
		return detailPanels.isEmpty() ? mainPanel() : detailLayout().layout().orElse(mainPanel());
	}

	/**
	 * @return the main panel containing the table, edit and control panels
	 */
	protected final JPanel mainPanel() {
		if (editPanel != null && editControlPanel.getComponents().length == 0) {
			editControlPanel.add(createEditBasePanel(editPanel), BorderLayout.CENTER);
		}
		if (tablePanel != null && mainPanel.getComponents().length == 0) {
			mainPanel.add(tablePanel, BorderLayout.CENTER);
		}
		mainPanel.setMinimumSize(new Dimension(0, 0));

		return mainPanel;
	}

	/**
	 * Sets up the keyboard actions.
	 * @see KeyboardShortcut
	 */
	protected final void setupKeyboardActions() {
		if (containsTablePanel()) {
			tablePanel.control(TableControl.REQUEST_TABLE_FOCUS).optional().ifPresent(control ->
							KeyEvents.builder(configuration.shortcuts.keyStroke(REQUEST_TABLE_FOCUS).get())
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(control)
											.enable(this));
			tablePanel.control(TableControl.TOGGLE_CONDITION_PANEL).optional().ifPresent(control ->
							KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_CONDITION_PANEL).get())
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(control)
											.enable(this));
			tablePanel.control(TableControl.SELECT_CONDITION_PANEL).optional().ifPresent(control ->
							KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_CONDITION_PANEL).get())
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(control)
											.enable(this));
			tablePanel.control(TableControl.TOGGLE_FILTER_PANEL).optional().ifPresent(control ->
							KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_FILTER_PANEL).get())
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(control)
											.enable(this));
			tablePanel.control(TableControl.SELECT_FILTER_PANEL).optional().ifPresent(control ->
							KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_FILTER_PANEL).get())
											.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
											.action(control)
											.enable(this));
			KeyEvents.builder(configuration.shortcuts.keyStroke(REQUEST_SEARCH_FIELD_FOCUS).get())
							.action(createRequestTableSearchFieldControl())
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.enable(this);
			if (containsEditPanel()) {
				tablePanel.control(TableControl.REQUEST_TABLE_FOCUS).optional().ifPresent(control ->
								KeyEvents.builder(configuration.shortcuts.keyStroke(REQUEST_TABLE_FOCUS).get())
												.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.action(control)
												.enable(editControlPanel));
				tablePanel.control(TableControl.TOGGLE_CONDITION_PANEL).optional().ifPresent(control ->
								KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_CONDITION_PANEL).get())
												.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.action(control)
												.enable(editControlPanel));
				tablePanel.control(TableControl.SELECT_CONDITION_PANEL).optional().ifPresent(control ->
								KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_CONDITION_PANEL).get())
												.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.action(control)
												.enable(editControlPanel));
				tablePanel.control(TableControl.TOGGLE_FILTER_PANEL).optional().ifPresent(control ->
								KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_FILTER_PANEL).get())
												.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.action(control)
												.enable(editControlPanel));
				tablePanel.control(TableControl.SELECT_FILTER_PANEL).optional().ifPresent(control ->
								KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_FILTER_PANEL).get())
												.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
												.action(control)
												.enable(editControlPanel));
			}
		}
		if (containsEditPanel()) {
			KeyEvents.builder(configuration.shortcuts.keyStroke(REQUEST_EDIT_PANEL_FOCUS).get())
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(createRequestEditPanelFocusControl())
							.enable(this);
			KeyEvents.builder(configuration.shortcuts.keyStroke(SELECT_INPUT_FIELD).get())
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(createSelectInputComponentControl())
							.enable(this, editControlPanel);
			KeyEvents.builder(configuration.shortcuts.keyStroke(TOGGLE_EDIT_PANEL).get())
							.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
							.action(createToggleEditPanelControl())
							.enable(this, editControlPanel);
		}
		if (configuration.useKeyboardNavigation) {
			setupNavigation();
		}
	}

	protected final void setupNavigation() {
		KeyEvents.Builder navigateUp = KeyEvents.builder(configuration.shortcuts.keyStroke(NAVIGATE_UP).get())
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(new Navigate(UP));
		KeyEvents.Builder navigateDown = KeyEvents.builder(configuration.shortcuts.keyStroke(NAVIGATE_DOWN).get())
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(new Navigate(DOWN));
		KeyEvents.Builder navigateRight = KeyEvents.builder(configuration.shortcuts.keyStroke(NAVIGATE_RIGHT).get())
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(new Navigate(RIGHT));
		KeyEvents.Builder navigateLeft = KeyEvents.builder(configuration.shortcuts.keyStroke(NAVIGATE_LEFT).get())
						.condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
						.action(new Navigate(LEFT));
		navigateUp.enable(this);
		navigateDown.enable(this);
		navigateRight.enable(this);
		navigateLeft.enable(this);
		if (containsEditPanel()) {
			navigateUp.enable(editControlPanel);
			navigateDown.enable(editControlPanel);
			navigateRight.enable(editControlPanel);
			navigateLeft.enable(editControlPanel);
		}
	}

	/**
	 * @return a Control instance for selecting a input component
	 */
	protected final Control createSelectInputComponentControl() {
		return Control.control(this::selectInputComponent);
	}

	/**
	 * @return a Control instance for requesting edit panel focus
	 */
	protected final Control createRequestEditPanelFocusControl() {
		return Control.control(this::requestEditPanelFocus);
	}

	/**
	 * @return a Control instance for requesting table search field focus
	 */
	protected final Control createRequestTableSearchFieldControl() {
		return Control.control(tablePanel.table().searchField()::requestFocusInWindow);
	}

	/**
	 * @return a Control instance for toggling the edit panel state
	 */
	protected final Control createToggleEditPanelControl() {
		return Control.builder(this::toggleEditPanelState)
						.smallIcon(ICONS.editPanel())
						.description(MESSAGES.getString("toggle_edit"))
						.build();
	}

	/**
	 * @return a Control instance for refreshing the table model
	 */
	protected final Control createRefreshTableControl() {
		return Control.builder(tableModel()::refresh)
						.name(Messages.refresh())
						.enabled(editPanel == null ? null : editPanel.active())
						.description(Messages.refreshTip() + " (ALT-" + Messages.refreshMnemonic() + ")")
						.mnemonic(Messages.refreshMnemonic())
						.smallIcon(ICONS.refresh())
						.build();
	}

	/**
	 * Initializes the edit panel, if one is available.
	 */
	protected final void initializeEditPanel() {
		if (editPanel != null) {
			editPanel.initialize();
			if (configuration.includeControls) {
				JComponent controlComponent = createControlComponent(createControls());
				if (controlComponent != null) {
					editControlPanel.add(controlComponent, configuration.controlComponentConstraints);
				}
			}
			updateEditPanelState();
		}
	}

	/**
	 * Initializes the table panel, if one is available.
	 */
	protected final void initializeTablePanel() {
		if (tablePanel != null) {
			tablePanel.table().doubleClickAction().mapNull(() -> Control.control(new ShowHiddenEditPanel()));
			tablePanel.initialize();
		}
	}

	/**
	 * @param <T> the detail layout type
	 * @return the detail layout used by this panel
	 */
	protected final <T extends DetailLayout> T detailLayout() {
		return (T) detailLayout;
	}

	/**
	 * @param <T> the detail controller type
	 * @return the detail controller used by this panel
	 */
	protected final <T extends DetailController> T detailController() {
		return (T) detailController;
	}

	private JPanel createEditControlPanel() {
		if (editPanel == null) {
			return null;
		}

		return borderLayoutPanel()
						.minimumSize(new Dimension(0, 0))
						.border(createEmptyBorder(Layouts.GAP.get(), 0, Layouts.GAP.get(), 0))
						.mouseListener(new ActivateOnMouseClickListener())
						.build();
	}

	final void setParentPanel(EntityPanel parentPanel) {
		if (this.parentPanel != null) {
			throw new IllegalStateException("Parent panel has already been set for " + this);
		}
		this.parentPanel = requireNonNull(parentPanel);
	}

	final void setPreviousSiblingPanel(EntityPanel previousSiblingPanel) {
		this.previousSiblingPanel = requireNonNull(previousSiblingPanel);
	}

	final void setNextSiblingPanel(EntityPanel nextSiblingPanel) {
		this.nextSiblingPanel = requireNonNull(nextSiblingPanel);
	}

	static void addEntityPanelAndLinkSiblings(EntityPanel detailPanel, List<EntityPanel> entityPanels) {
		if (!entityPanels.isEmpty()) {
			EntityPanel leftSibling = entityPanels.get(entityPanels.size() - 1);
			detailPanel.setPreviousSiblingPanel(leftSibling);
			leftSibling.setNextSiblingPanel(detailPanel);
			EntityPanel firstPanel = entityPanels.get(0);
			detailPanel.setNextSiblingPanel(firstPanel);
			firstPanel.setPreviousSiblingPanel(detailPanel);
		}
		entityPanels.add(detailPanel);
	}

	final WindowType windowType() {
		return configuration.windowType;
	}

	private JToolBar createControlToolBar(Controls controls) {
		return toolBar(controls)
						.orientation(configuration.horizontalControlLayout() ? HORIZONTAL : VERTICAL)
						.build();
	}

	private JPanel createControlPanel(Controls controls) {
		if (configuration.horizontalControlLayout()) {
			return flowLayoutPanel(FlowLayout.CENTER)
							.add(buttonPanel(controls)
											.toggleButtonType(CHECKBOX)
											.build())
							.build();
		}

		return borderLayoutPanel()
						.northComponent(buttonPanel(controls)
										.orientation(VERTICAL)
										.buttonBuilder(buttonBuilder ->
														buttonBuilder.horizontalAlignment(SwingConstants.LEADING))
										.toggleButtonType(CHECKBOX)
										.build())
						.build();
	}

	private void setupToggleEditPanelControl() {
		if (containsTablePanel() && containsEditPanel() && configuration.includeToggleEditPanelControl) {
			tablePanel.addToolBarControls(Controls.builder()
							.control(createToggleEditPanelControl())
							.build());
		}
	}

	//#############################################################################################
	// End - initialization methods
	//#############################################################################################

	private void requestEditPanelFocus() {
		if (editPanelState.isEqualTo(HIDDEN)) {
			editPanelState.set(EMBEDDED);
		}
		editPanel().requestInitialFocus();
	}

	private void selectInputComponent() {
		if (editPanelState.isEqualTo(HIDDEN)) {
			editPanelState.set(EMBEDDED);
		}
		editPanel().selectInputComponent();
	}

	private void updateEditPanelState() {
		switch (editPanelState.get()) {
			case WINDOW:
				showEditWindow();
				break;
			case EMBEDDED:
				hideEditWindow();
				mainPanel.add(editControlPanel, BorderLayout.NORTH);
				break;
			case HIDDEN:
				hideEditWindow();
				mainPanel.remove(editControlPanel);
				break;
			default:
				throw new IllegalStateException("Unkown panel state: " + editPanelState.get());
		}
		revalidate();
		requestInitialFocus();
	}

	private void toggleEditPanelState() {
		editPanelState.map(PANEL_STATE_MAPPER);
	}

	private void showEditWindow() {
		JPanel basePanel = borderLayoutPanel()
						.border(createEmptyBorder(Layouts.GAP.get(), Layouts.GAP.get(), 0, Layouts.GAP.get()))
						.centerComponent(editControlPanel)
						.build();
		if (configuration.windowType == WindowType.FRAME) {
			showEditFrame(basePanel);
		}
		else {
			showEditDialog(basePanel);
		}
	}

	private void showEditFrame(JPanel basePanel) {
		Windows.frame(basePanel)
						.locationRelativeTo(tablePanel == null ? this : tablePanel)
						.title(configuration.caption)
						.icon(configuration.icon)
						.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
						.onClosed(windowEvent -> editPanelState.set(HIDDEN))
						.build()
						.setVisible(true);
	}

	private void showEditDialog(JPanel basePanel) {
		Dialogs.componentDialog(basePanel)
						.owner(this)
						.locationRelativeTo(tablePanel == null ? this : tablePanel)
						.title(configuration.caption)
						.icon(configuration.icon)
						.modal(false)
						.disposeOnEscape(configuration.disposeEditDialogOnEscape)
						.onClosed(windowEvent -> editPanelState.set(HIDDEN))
						.build()
						.setVisible(true);
	}

	private void hideEditWindow() {
		Window editPanelWindow = parentWindow(editControlPanel);
		if (editPanelWindow != null) {
			editPanelWindow.dispose();
		}
	}

	private void throwIfInitialized() {
		if (initialized) {
			throw new IllegalStateException("Method must be called before the panel is initialized");
		}
	}

	private Config configure(Consumer<Config> configuration) {
		Config config = new Config(this);
		requireNonNull(configuration).accept(config);

		return new Config(config);
	}

	private final class ShowHiddenEditPanel implements Control.Command {

		@Override
		public void execute() {
			if (containsEditPanel() && editPanelState.isEqualTo(HIDDEN)) {
				editPanelState.set(WINDOW);
			}
			Window editPanelWindow = parentWindow(editControlPanel);
			if (editPanelWindow != null) {
				editPanelWindow.toFront();
			}
		}
	}

	private final class Navigate extends AbstractAction {

		private final Direction direction;

		private Navigate(Direction direction) {
			super("Navigate " + direction);
			this.direction = direction;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			switch (direction) {
				case LEFT:
					if (previousSiblingPanel != null) {
						previousSiblingPanel.activate();
					}
					break;
				case RIGHT:
					if (nextSiblingPanel != null) {
						nextSiblingPanel.activate();
					}
					break;
				case UP:
					if (parentPanel != null) {
						parentPanel.activate();
					}
					break;
				case DOWN:
					activeDetailPanel()
									.ifPresent(EntityPanel::activate);
					break;
				default:
					throw new IllegalArgumentException("Unknown direction: " + direction);
			}
		}

		private Optional<EntityPanel> activeDetailPanel() {
			return activeDetailPanels().stream().findFirst();
		}
	}

	/**
	 * Contains configuration settings for a {@link EntityPanel} which must be set before the panel is initialized.
	 */
	public static final class Config {

		/**
		 * Indicates whether keyboard navigation will be enabled<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> USE_KEYBOARD_NAVIGATION =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.useKeyboardNavigation", true);

		/**
		 * Indicates whether entity edit panel dialogs should be closed on escape<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> DISPOSE_EDIT_DIALOG_ON_ESCAPE =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.disposeEditDialogOnEscape", true);

		/**
		 * Specifies whether a control for toggling the edit panel is available to the user<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_TOGGLE_EDIT_PANEL_CONTROL =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.includeToggleEditPanelControl", true);

		/**
		 * Specifies whether the edit controls (Save, update, delete, clear, refresh) should be on a toolbar instead of a button panel<br>
		 * Value type: Boolean<br>
		 * Default value: false
		 */
		public static final PropertyValue<Boolean> TOOLBAR_CONTROLS =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.toolbarControls", false);

		/**
		 * Specifies how detail and edit panels should be displayed.<br>
		 * Value type: {@link WindowType}<br>
		 * Default value: {@link WindowType#DIALOG}
		 */
		public static final PropertyValue<WindowType> WINDOW_TYPE =
						Configuration.enumValue("is.codion.swing.framework.ui.EntityPanel.windowType", WindowType.class, WindowType.DIALOG);

		/**
		 * Specifies where the control panel should be placed in a BorderLayout<br>
		 * Value type: Boolean<br>
		 * Default value: {@link BorderLayout#EAST}
		 * @see #TOOLBAR_CONTROLS
		 */
		public static final PropertyValue<String> CONTROL_PANEL_CONSTRAINTS =
						Configuration.stringValue("is.codion.swing.framework.ui.EntityPanel.controlPanelConstraints", BorderLayout.EAST);

		/**
		 * Specifies where the control toolbar should be placed in a BorderLayout<br>
		 * Value type: Boolean<br>
		 * Default value: BorderLayout.WEST
		 * @see #TOOLBAR_CONTROLS
		 */
		public static final PropertyValue<String> CONTROL_TOOLBAR_CONSTRAINTS =
						Configuration.stringValue("is.codion.swing.framework.ui.EntityPanel.controlToolbarConstraints", BorderLayout.WEST);

		/**
		 * Specifies whether entity panels should include controls by default<br>
		 * Value type: Boolean<br>
		 * Default value: true
		 */
		public static final PropertyValue<Boolean> INCLUDE_CONTROLS =
						Configuration.booleanValue("is.codion.swing.framework.ui.EntityPanel.includeControls", true);

		/**
		 * The default keyboard shortcut keyStrokes.
		 */
		public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS = keyboardShortcuts(KeyboardShortcut.class);

		private final EntityPanel entityPanel;
		private final KeyboardShortcuts<KeyboardShortcut> shortcuts;

		private Function<EntityPanel, DetailLayout> detailLayout = new DefaultDetailLayout();
		private boolean disposeEditDialogOnEscape = DISPOSE_EDIT_DIALOG_ON_ESCAPE.get();
		private boolean toolbarControls = TOOLBAR_CONTROLS.get();
		private boolean includeToggleEditPanelControl = INCLUDE_TOGGLE_EDIT_PANEL_CONTROL.get();
		private String controlComponentConstraints = TOOLBAR_CONTROLS.get() ?
						CONTROL_TOOLBAR_CONSTRAINTS.get() : CONTROL_PANEL_CONSTRAINTS.get();
		private boolean includeControls = INCLUDE_CONTROLS.get();
		private boolean useKeyboardNavigation = USE_KEYBOARD_NAVIGATION.get();
		private WindowType windowType = WINDOW_TYPE.get();
		private PanelState editPanelState = EMBEDDED;
		private String caption;
		private String description;
		private ImageIcon icon;

		private Config(EntityPanel entityPanel) {
			this.entityPanel = entityPanel;
			this.shortcuts = KEYBOARD_SHORTCUTS.copy();
			this.caption = entityPanel.model().entityDefinition().caption();
		}

		private Config(Config config) {
			this.entityPanel = config.entityPanel;
			this.shortcuts = config.shortcuts.copy();
			this.detailLayout = config.detailLayout;
			this.toolbarControls = config.toolbarControls;
			this.includeToggleEditPanelControl = config.includeToggleEditPanelControl;
			this.controlComponentConstraints = config.controlComponentConstraints;
			this.includeControls = config.includeControls;
			this.useKeyboardNavigation = config.useKeyboardNavigation;
			this.editPanelState = config.editPanelState;
			this.caption = config.caption;
			this.description = config.description;
			this.icon = config.icon;
			this.disposeEditDialogOnEscape = config.disposeEditDialogOnEscape;
			this.windowType = config.windowType;
		}

		/**
		 * @return the entity panel
		 */
		public EntityPanel entityPanel () {
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
		 * @param toolbarControls true if the edit controls should be on a toolbar instead of a button panel
		 * @return this Config instance
		 * @see #TOOLBAR_CONTROLS
		 */
		public Config toolbarControls(boolean toolbarControls) {
			this.toolbarControls = toolbarControls;
			return this;
		}

		/**
		 * Sets the layout constraints to use for the control panel
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
			switch (requireNonNull(controlComponentConstraints)) {
				case BorderLayout.SOUTH:
				case BorderLayout.NORTH:
				case BorderLayout.EAST:
				case BorderLayout.WEST:
					break;
				default:
					throw new IllegalArgumentException("Control component constraints must be one of BorderLayout.SOUTH, NORTH, EAST or WEST");
			}
			this.controlComponentConstraints = controlComponentConstraints;
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
		 * @param useKeyboardNavigation true if keyboard navigation should be enabled
		 * @return this Config instance
		 */
		public Config useKeyboardNavigation(boolean useKeyboardNavigation) {
			this.useKeyboardNavigation = useKeyboardNavigation;
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
		 * @param shortcuts provides this tables {@link KeyboardShortcuts} instance.
		 * @return this Config instance
		 */
		public Config keyStrokes(Consumer<KeyboardShortcuts<KeyboardShortcut>> shortcuts) {
			requireNonNull(shortcuts).accept(this.shortcuts);
			return this;
		}

		/**
		 * @param editPanelState the initial edit panel state
		 * @return this Config instance
		 */
		public Config editPanelState(PanelState editPanelState) {
			this.editPanelState = requireNonNull(editPanelState);
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

		private static final class DefaultDetailLayout implements Function<EntityPanel, DetailLayout> {

			@Override
			public DetailLayout apply(EntityPanel entityPanel) {
				return TabbedDetailLayout.builder(entityPanel).build();
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
	 * select one detail panel.
	 */
	public interface DetailController {

		/**
		 * Note that the detail panel state may be shared between detail panels,
		 * as they may be displayed in a shared window.
		 * @param detailPanel the detail panel
		 * @return the value controlling the state of the given detail panel
		 */
		default Value<PanelState> panelState(EntityPanel detailPanel) {
			throw new UnsupportedOperationException("panelState() has not been implemented for detail controller: " + getClass());
		}

		/**
		 * Called when the given detail panel is activated.
		 * @param detailPanel the detail panel to select
		 * @see EntityPanel#activate()
		 * @see EntityPanel#activateEvent()
		 */
		default void activated(EntityPanel detailPanel) {}
	}

	/**
	 * A builder for {@link EntityPanel} instances.
	 */
	public interface Builder {

		/**
		 * @return the entityType
		 */
		EntityType entityType();

		/**
		 * @param caption the panel caption
		 * @return this builder instance
		 */
		Builder caption(String caption);

		/**
		 * @return the caption, an empty Optional if none has been set
		 */
		Optional<String> caption();

		/**
		 * @param description the panel description
		 * @return this builder instance
		 */
		Builder description(String description);

		/**
		 * @return the description, an empty Optional if none has been set
		 */
		Optional<String> description();

		/**
		 * @param icon the panel icon
		 * @return this builder instance
		 */
		Builder icon(ImageIcon icon);

		/**
		 * @return the icon, an empty Optional if none has been set
		 */
		Optional<ImageIcon> icon();

		/**
		 * Adds the given detail panel builder to this panel builder, if it hasn't been previously added
		 * @param panelBuilder the detail panel provider
		 * @return this builder instance
		 */
		Builder detailPanel(EntityPanel.Builder panelBuilder);

		/**
		 * Default true.
		 * @param refreshWhenInitialized if true then the table model this panel is based on
		 * will be refreshed when the panel is initialized
		 * @return this builder instance
		 */
		Builder refreshWhenInitialized(boolean refreshWhenInitialized);

		/**
		 * @param conditionPanelVisible if true then the table condition panel is made visible when the panel is initialized
		 * @return this builder instance
		 */
		Builder conditionPanelVisible(boolean conditionPanelVisible);

		/**
		 * @param filterPanelVisible if true then the table filter panel is made visible when the panel is initialized
		 * @return this builder instance
		 */
		Builder filterPanelVisible(boolean filterPanelVisible);

		/**
		 * @param detailLayout provides the detail panel layout to use
		 * @return this builder instane
		 */
		Builder detailLayout(Function<EntityPanel, DetailLayout> detailLayout);

		/**
		 * @param preferredSize the preferred panel size
		 * @return this builder instance
		 */
		Builder preferredSize(Dimension preferredSize);

		/**
		 * Note that setting the EntityPanel class overrides any table panel or edit panel classes that have been set.
		 * @param panelClass the EntityPanel class to use when providing this panel
		 * @return this builder instance
		 */
		Builder panel(Class<? extends EntityPanel> panelClass);

		/**
		 * @param editPanelClass the EntityEditPanel class to use when providing this panel
		 * @return this builder instance
		 */
		Builder editPanel(Class<? extends EntityEditPanel> editPanelClass);

		/**
		 * @param tablePanelClass the EntityTablePanel class to use when providing this panel
		 * @return this builder instance
		 */
		Builder tablePanel(Class<? extends EntityTablePanel> tablePanelClass);

		/**
		 * @param onBuildPanel called after the entity panel has been built
		 * @return this builder instance
		 */
		Builder onBuildPanel(Consumer<EntityPanel> onBuildPanel);

		/**
		 * @param onBuildEditPanel called after the edit panel has been built
		 * @return this builder instance
		 */
		Builder onBuildEditPanel(Consumer<EntityEditPanel> onBuildEditPanel);

		/**
		 * @param onBuildTablePanel called after the table panel has been built
		 * @return this builder instance
		 */
		Builder onBuildTablePanel(Consumer<EntityTablePanel> onBuildTablePanel);

		/**
		 * Creates an EntityPanel based on this builder
		 * @param connectionProvider the connection provider
		 * @return an EntityPanel based on this builder
		 * @throws IllegalStateException in case no {@link SwingEntityModel.Builder} has been set
		 */
		EntityPanel build(EntityConnectionProvider connectionProvider);

		/**
		 * Creates an EntityPanel based on this builder
		 * @param model the EntityModel to base this panel on
		 * @return an EntityPanel based on this builder
		 */
		EntityPanel build(SwingEntityModel model);
	}

	private final class ActivateOnMouseClickListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent e) {
			editPanel.requestAfterUpdateFocus();
		}
	}

	private static final class PanelStateMapper implements Function<PanelState, PanelState> {

		@Override
		public PanelState apply(PanelState state) {
			switch (state) {
				case HIDDEN:
					return EMBEDDED;
				case EMBEDDED:
					return WINDOW;
				case WINDOW:
					return HIDDEN;
				default:
					throw new IllegalArgumentException("Unknown panel state: " + state);
			}
		}
	}
}
