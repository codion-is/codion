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
import is.codion.common.model.CancelException;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.logging.LoggerProxy;
import is.codion.common.utilities.property.PropertyStore;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionTracer;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.UIManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.logging.LogViewer;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ControlsBuilder;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.frame.Frames;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.scaler.Scaler;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityPanel.WindowType;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.net.URI;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.stringValue;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.logging.LogLevelPanel.logLevelPanel;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;
import static java.awt.Frame.MAXIMIZED_BOTH;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.UIManager.getIcon;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A central application panel class.
 * @param <M> the application model type
 * @see EntityApplication#builder(Class, Class)
 */
public class EntityApplicationPanel<M extends SwingEntityApplicationModel> extends JPanel {

	/**
	 * Specifies whether the application should call {@link System#exit(int)} when exiting.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see #exit()
	 */
	public static final PropertyValue<Boolean> CALL_SYSTEM_EXIT =
					booleanValue(EntityApplicationPanel.class.getName() + ".callSystemExit", false);

	/**
	 * Specifies whether a button for displaying system properties is included on the about panel.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 * @see #displayAbout()
	 */
	public static final PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES =
					booleanValue(EntityApplicationPanel.class.getName() + ".displaySystemProperties", true);

	/**
	 * Specifies whether to include sql tracing related controls in the log menu.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> SQL_TRACING =
					booleanValue(EntityApplicationPanel.class.getName() + ".sqlTracing", false);

	private static final String LOG_LEVEL = "log_level";
	private static final String LOG_LEVEL_DESC = "log_level_desc";
	private static final String HELP = "help";
	private static final String SHORTCUTS = "shortcuts";
	private static final String ABOUT = "about";
	private static final String ALWAYS_ON_TOP = "always_on_top";
	private static final String APPLICATION_VERSION = "application_version";
	private static final String CODION_VERSION = "codion_version";
	private static final String MEMORY_USAGE = "memory_usage";
	private static final String ADVANCED_LOG_LEVEL = "advanced_log_level";
	private static final String ADVANCED_LOG_LEVEL_MNEMONIC = "advanced_log_level_mnemonic";
	private static final NumberFormat MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

	/**
	 * Specifies the URL to the application help
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: https://codion.is/doc/{version}/help/client.html
	 * </ul>
	 */
	public static final PropertyValue<String> HELP_URL = stringValue(EntityApplicationPanel.class.getName() + ".helpUrl",
					"https://codion.is/doc/" + Version.versionString() + "/help/client.html");

	/**
	 * Indicates whether the application should ask for confirmation when exiting
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 */
	public static final PropertyValue<Boolean> CONFIRM_EXIT = booleanValue(EntityApplicationPanel.class.getName() + ".confirmExit", false);

	/**
	 * Specifies whether EntityPanels displayed via {@link EntityApplicationPanel#displayEntityPanelDialog(EntityPanel)}
	 * or {@link EntityApplicationPanel#displayEntityPanelFrame(EntityPanel)} should be cached,
	 * instead of being created each time the dialog/frame is shown.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see EntityApplicationPanel#displayEntityPanelDialog(EntityPanel)
	 * @see EntityApplicationPanel#displayEntityPanelFrame(EntityPanel)
	 */
	public static final PropertyValue<Boolean> CACHE_ENTITY_PANELS = booleanValue(EntityApplicationPanel.class.getName() + ".cacheEntityPanels", false);

	/**
	 * Specifies whether legacy preferences are used along with file based preferences
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> LEGACY_PREFERENCES = booleanValue(EntityApplicationPanel.class.getName() + ".legacyPreferences", true);

	// Non-static so that Locale.setDefault(...) can be called in the main method of a subclass
	private final MessageBundle resourceBundle =
					messageBundle(EntityApplicationPanel.class, getBundle(EntityApplicationPanel.class.getName()));

	private final M applicationModel;
	private final Collection<EntityPanel.Builder> lookupPanelBuilders;
	private final List<EntityPanel> entityPanels;
	private final ApplicationLayout applicationLayout;

	private final State alwaysOnTopState = State.builder()
					.consumer(alwaysOnTop -> Ancestor.window().of(this).optional()
									.ifPresent(parent -> parent.setAlwaysOnTop(alwaysOnTop)))
					.build();
	private final Event<?> exiting = Event.event();
	private final Event<EntityApplicationPanel<?>> initializedEvent = Event.event();
	private final boolean modifiedWarning = EntityEditPanel.Config.MODIFIED_WARNING.getOrThrow();
	private final boolean userPreferences = EntityApplicationModel.USER_PREFERENCES.getOrThrow();
	private final boolean restoreDefaultPreferences = EntityApplicationModel.RESTORE_DEFAULT_PREFERENCES.getOrThrow();

	private final Map<EntityPanel.Builder, EntityPanel> cachedEntityPanels = new HashMap<>();

	private final Map<Object, State> logLevelStates = createLogLevelStateMap();

	private @Nullable LogViewer sqlTraceViewer;

	private boolean saveDefaultUsername = true;
	private boolean initialized = false;

	/**
	 * Instantiates a new {@link EntityApplicationPanel} based on the given application model,
	 * using the default {@link TabbedApplicationLayout}.
	 * @param applicationModel the application model
	 * @param entityPanels the entity panels
	 * @param lookupPanelBuilders the support panel builders
	 */
	public EntityApplicationPanel(M applicationModel, List<EntityPanel> entityPanels,
																Collection<EntityPanel.Builder> lookupPanelBuilders) {
		this(applicationModel, entityPanels, lookupPanelBuilders, TabbedApplicationLayout::new);
	}

	/**
	 * Instantiates a new {@link EntityApplicationPanel} based on the given application model,
	 * using the {@link ApplicationLayout} provided by {@code applicationLayout}.
	 * @param applicationModel the application model
	 * @param entityPanels the entity panels
	 * @param lookupPanelBuilders the support panel builders
	 * @param applicationLayout provides the application layout
	 */
	public EntityApplicationPanel(M applicationModel, List<EntityPanel> entityPanels,
																Collection<EntityPanel.Builder> lookupPanelBuilders,
																Function<EntityApplicationPanel<M>, ApplicationLayout> applicationLayout) {
		this.applicationModel = requireNonNull(applicationModel);
		this.entityPanels = unmodifiableList(new ArrayList<>(requireNonNull(entityPanels)));
		this.lookupPanelBuilders = requireNonNull(lookupPanelBuilders);
		this.applicationLayout = requireNonNull(applicationLayout).apply(this);
		entityPanels.forEach(this::configureEntityPanel);
		setupTracing();
		//initialize button captions, not in a static initializer since applications may set the locale in main()
		UIManagerDefaults.initialize();
	}

	/**
	 * Displays the exception in a dialog
	 * @param exception the exception to display
	 */
	public final void displayException(Exception exception) {
		Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		if (focusOwner == null) {
			focusOwner = EntityApplicationPanel.this;
		}
		Dialogs.displayException(exception, Ancestor.window().of(focusOwner).get());
	}

	/**
	 * @return the application model this application panel is based on
	 */
	public final M applicationModel() {
		return applicationModel;
	}

	/**
	 * @return the application layout
	 */
	public final ApplicationLayout applicationLayout() {
		return applicationLayout;
	}

	/**
	 * @param entityType the entityType
	 * @return the first entity panel found based on the given entity type
	 * @throws IllegalArgumentException in case this application panel does not contain a panel for the given entity type
	 */
	public final EntityPanel entityPanel(EntityType entityType) {
		requireNonNull(entityType);
		return entityPanels.stream()
						.filter(entityPanel -> entityPanel.model().entityType().equals(entityType))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("EntityPanel for entity: " + entityType + " not found in application panel: " + getClass()));
	}

	/**
	 * @return an unmodifiable view of the main application panels
	 */
	public final List<EntityPanel> entityPanels() {
		return entityPanels;
	}

	/**
	 * @return a State controlling the alwaysOnTop state of this panels parent window
	 */
	public final State alwaysOnTop() {
		return alwaysOnTopState;
	}

	/**
	 * Displays in a dialog a tree describing the application layout
	 */
	public final void viewApplicationTree() {
		Dialogs.builder()
						.component(createApplicationTree())
						.owner(this)
						.title(resourceBundle.getString("view_application_tree"))
						.modal(false)
						.show();
	}

	/**
	 * Shows a dialog containing a dependency tree view of all defined entities
	 */
	public final void viewDependencyTree() {
		Dialogs.builder()
						.component(createDependencyTree())
						.owner(this)
						.title(FrameworkMessages.dependencies())
						.modal(false)
						.show();
	}

	/**
	 * @return an observer notified when this application panel is initialized
	 * @see #initialize()
	 */
	public final Observer<EntityApplicationPanel<?>> initialized() {
		return initializedEvent.observer();
	}

	/**
	 * Exits this application. Calls {@link System#exit(int)} in case {@link #CALL_SYSTEM_EXIT} is true.
	 * @throws CancelException if the exit is cancelled
	 * @see #exiting()
	 * @see EntityEditPanel.Config#MODIFIED_WARNING
	 * @see EntityApplicationPanel#CONFIRM_EXIT
	 * @see EntityApplicationPanel#CALL_SYSTEM_EXIT
	 */
	public final void exit() {
		handleUnsavedModifications();
		if (!confirmExit()) {
			throw new CancelException();
		}

		try {
			exiting.run();
		}
		catch (CancelException e) {
			throw e;
		}
		catch (Exception e) {
			LOG.debug("Exception while exiting", e);
		}
		try {
			if (userPreferences) {
				LOG.debug("Writing user preferences");
				writePreferences(applicationModel.preferences());
				UserPreferences.flush();
			}
		}
		catch (Throwable e) {
			LOG.error("Exception while saving preferences", e);
		}
		try {
			applicationModel.connectionProvider().close();
		}
		catch (Exception e) {
			LOG.debug("Exception while disconnecting from database", e);
		}
		Stream.of(Window.getWindows()).forEach(Window::dispose);
		if (CALL_SYSTEM_EXIT.getOrThrow()) {
			System.exit(0);
		}
	}

	/**
	 * Displays the help.
	 * @throws Exception in case of an exception, for example a malformed URL
	 * @see #HELP_URL
	 */
	public void displayHelp() throws Exception {
		Desktop.getDesktop().browse(URI.create(HELP_URL.getOrThrow()));
	}

	/**
	 * Displays a keyboard shortcut overview panel.
	 */
	public final void displayKeyboardShortcuts() {
		KeyboardShortcutsPanel shortcutsPanel = new KeyboardShortcutsPanel();
		shortcutsPanel.setPreferredSize(new Dimension(shortcutsPanel.getPreferredSize().width, screenSizeRatio(0.5).height));
		Dialogs.builder()
						.component(shortcutsPanel)
						.owner(this)
						.title(resourceBundle.getString(SHORTCUTS))
						.modal(false)
						.show();
	}

	/**
	 * Shows an about-dialog
	 * @see #createAboutPanel()
	 */
	public final void displayAbout() {
		Dialogs.builder()
						.component(createAboutPanel())
						.owner(this)
						.title(resourceBundle.getString(ABOUT))
						.resizable(false)
						.show();
	}

	/**
	 * Initializes this panel and marks is as initialized, subsequent calls have no effect.
	 * @return this application panel
	 */
	public final EntityApplicationPanel<M> initialize() {
		if (!initialized) {
			try {
				applyPreferences();
				setLayout(new BorderLayout());
				add(applicationLayout.layout(), BorderLayout.CENTER);
				bindEvents();
				initializedEvent.accept(this);
			}
			finally {
				initialized = true;
			}
		}

		return this;
	}

	/**
	 * <p>Requests the initial application focus by calling {@link EntityPanel#requestInitialFocus()}
	 * on the main entity panel.
	 * <p>By default, the main entity panel is the first one returned by {@link #entityPanels()}.
	 */
	public void requestInitialFocus() {
		if (!entityPanels.isEmpty()) {
			entityPanels.get(0).requestInitialFocus();
		}
	}

	/**
	 * @param entities the entities
	 * @return a tree model showing the dependencies between entities via foreign keys
	 */
	public static TreeModel createDependencyTreeModel(Entities entities) {
		requireNonNull(entities);
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
		for (EntityDefinition definition : entities.definitions()) {
			if (definition.foreignKeys().get().isEmpty() || referencesOnlySelf(entities, definition.type())) {
				root.add(new EntityDependencyTreeNode(definition.type(), entities));
			}
		}

		return new DefaultTreeModel(root);
	}

	/**
	 * @return the controls on which to base the main menu or an empty Optional if the menu should be excluded
	 * @see #createFileMenuControls()
	 * @see #createViewMenuControls()
	 * @see #createToolsMenuControls()
	 * @see #createLookupMenuControls()
	 * @see #createHelpMenuControls()
	 */
	protected Optional<Controls> createMainMenuControls() {
		ControlsBuilder menuControls = Controls.builder();
		createFileMenuControls().ifPresent(menuControls::control);
		createViewMenuControls().ifPresent(menuControls::control);
		createToolsMenuControls().ifPresent(menuControls::control);
		createLookupMenuControls().ifPresent(menuControls::control);
		createHelpMenuControls().ifPresent(menuControls::control);

		Controls controls = menuControls.build();

		return controls.size() == 0 ? Optional.empty() : Optional.of(controls);
	}

	/**
	 * @return the Controls specifying the items in the 'File' menu or an empty Optional to skip the menu
	 */
	protected Optional<Controls> createFileMenuControls() {
		return Optional.of(Controls.builder()
						.caption(FrameworkMessages.file())
						.mnemonic(FrameworkMessages.fileMnemonic())
						.control(createExitControl())
						.build());
	}

	/**
	 * @return the Controls specifying the items in the 'Tools' menu or an empty Optional to skip the menu
	 */
	protected Optional<Controls> createToolsMenuControls() {
		return Optional.of(Controls.builder()
						.caption(resourceBundle.getString("tools"))
						.mnemonic(resourceBundle.getString("tools_mnemonic").charAt(0))
						.build());
	}

	/**
	 * @return the Controls specifying the items in the 'View' menu or an empty Optional to skip the menu
	 */
	protected Optional<Controls> createViewMenuControls() {
		return Optional.of(Controls.builder()
						.caption(FrameworkMessages.view())
						.mnemonic(FrameworkMessages.viewMnemonic())
						.control(createSelectLookAndFeelControl())
						.control(createSelectScalingControl())
						.separator()
						.control(createAlwaysOnTopControl())
						.build());
	}

	/**
	 * @return the Controls specifying the items in the 'Help' menu or an empty Optional to skip the menu
	 */
	protected Optional<Controls> createHelpMenuControls() {
		return Optional.of(Controls.builder()
						.caption(resourceBundle.getString(HELP))
						.mnemonic(resourceBundle.getString("help_mnemonic").charAt(0))
						.control(createHelpControl())
						.control(createViewKeyboardShortcutsControl())
						.separator()
						.control(createLogControls())
						.separator()
						.control(createAboutControl())
						.build());
	}

	/**
	 * @return the Controls on which to base the Lookup menu or an empty Optional to skip the menu
	 */
	protected Optional<Controls> createLookupMenuControls() {
		return lookupPanelBuilders.isEmpty() ? Optional.empty() : Optional.of(Controls.builder()
						.caption(FrameworkMessages.lookup())
						.mnemonic(FrameworkMessages.lookupMnemonic())
						.controls(lookupPanelBuilders.stream()
										.sorted(new LookupPanelBuilderComparator(applicationModel.entities()))
										.map(this::createLookupPanelControl)
										.collect(toList()))
						.build());
	}

	/**
	 * @return a Control for exiting the application
	 */
	protected final Control createExitControl() {
		return Control.builder()
						.command(this::exit)
						.caption(FrameworkMessages.exit())
						.description(FrameworkMessages.exitTip())
						.mnemonic(FrameworkMessages.exitMnemonic())
						.build();
	}

	/**
	 * @return Controls for setting the log level
	 */
	protected final Controls createLogLevelControl() {
		return Controls.builder()
						.caption(resourceBundle.getString(LOG_LEVEL))
						.description(resourceBundle.getString(LOG_LEVEL_DESC))
						.controls(logLevelStates.entrySet().stream()
										.map(entry -> Control.builder()
														.toggle(entry.getValue())
														.caption(entry.getKey().toString())
														.build())
										.collect(toList()))
						.separator()
						.control(Control.builder()
										.command(this::advancedLogLevelConfiguration)
										.caption(resourceBundle.getString(ADVANCED_LOG_LEVEL) + "...")
										.mnemonic(resourceBundle.getString(ADVANCED_LOG_LEVEL_MNEMONIC).charAt(0)))
						.build();
	}

	/**
	 * @return a Control for viewing the application structure tree
	 */
	protected final Control createViewApplicationTreeControl() {
		return Control.builder()
						.command(this::viewApplicationTree)
						.caption(resourceBundle.getString("view_application_tree"))
						.build();
	}

	/**
	 * @return a Control for viewing the application dependency tree
	 */
	protected final Control createViewDependencyTree() {
		return Control.builder()
						.command(this::viewDependencyTree)
						.caption(FrameworkMessages.dependencies())
						.build();
	}

	/**
	 * Allows the user the select between the available Look and Feels.
	 * @return a Control for selecting the application look and feel
	 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
	 * @see LookAndFeelProvider#findLookAndFeel(String)
	 * @see LookAndFeelComboBox#ENABLE_ON_SELECTION
	 */
	protected final Control createSelectLookAndFeelControl() {
		return Dialogs.select()
						.lookAndFeel()
						.owner(this)
						.createControl();
	}

	/**
	 * @return a Control for selecting the scaling
	 */
	protected final Control createSelectScalingControl() {
		return Control.builder()
						.command(this::selectScaling)
						.caption(resourceBundle.getString("scaling"))
						.build();
	}

	/**
	 * @return a Control controlling the always on top status
	 */
	protected final ToggleControl createAlwaysOnTopControl() {
		return Control.builder()
						.toggle(alwaysOnTopState)
						.caption(resourceBundle.getString(ALWAYS_ON_TOP))
						.build();
	}

	/**
	 * @return a Control for viewing information about the application
	 */
	protected final Control createAboutControl() {
		return Control.builder()
						.command(this::displayAbout)
						.caption(resourceBundle.getString(ABOUT) + "...")
						.build();
	}

	/**
	 * @return a Control for displaying the help
	 */
	protected final Control createHelpControl() {
		return Control.builder()
						.command(this::displayHelp)
						.caption(resourceBundle.getString(HELP) + "...")
						.build();
	}

	/**
	 * @return Controls for log handling
	 */
	protected final Controls createLogControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(resourceBundle.getString("log"))
						.mnemonic(resourceBundle.getString("log_mnemonic").charAt(0));
		if (!logLevelStates.isEmpty()) {
			builder.control(createLogLevelControl());
		}
		createOpenLogControls().ifPresent(builder::control);
		createTracingControls().ifPresent(builder::control);

		return builder.build();
	}

	/**
	 * @return a Control for displaying the keyboard shortcuts overview
	 */
	protected final Control createViewKeyboardShortcutsControl() {
		return Control.builder()
						.command(this::displayKeyboardShortcuts)
						.caption(resourceBundle.getString(SHORTCUTS) + "...")
						.build();
	}

	/**
	 * @return the panel shown when Help -&#62; About is selected
	 */
	protected JPanel createAboutPanel() {
		PanelBuilder<GridLayout, ?> versionMemoryPanel = gridLayoutPanel(0, 2)
						.border(emptyBorder());
		applicationModel().version().ifPresent(version -> versionMemoryPanel
						.add(new JLabel(resourceBundle.getString(APPLICATION_VERSION) + ":"))
						.add(new JLabel(version.toString())));
		versionMemoryPanel
						.add(new JLabel(resourceBundle.getString(CODION_VERSION) + ":"))
						.add(new JLabel(Version.versionAndMetadataString()))
						.add(new JLabel(resourceBundle.getString(MEMORY_USAGE) + ":"));

		JLabel memoryLabel = new JLabel(MEMORY_USAGE_FORMAT.format((RUNTIME.totalMemory() - RUNTIME.freeMemory()) / 1024) + " KB");
		versionMemoryPanel.add(DISPLAY_SYSTEM_PROPERTIES.getOrThrow() ? borderLayoutPanel()
						.center(memoryLabel)
						.east(toolBar()
										.action(createDisplaySystemPropertiesControl())
										.floatable(false)
										.includeButtonText(true)
										.preferredHeight(memoryLabel.getHeight()))
						.build() : memoryLabel);

		return borderLayoutPanel()
						.border(emptyBorder())
						.west(new JLabel(FrameworkIcons.instance().logo()))
						.center(versionMemoryPanel)
						.build();
	}

	/**
	 * Override to add event bindings after initialization
	 */
	protected void bindEvents() {}

	/**
	 * Displays the panel provided by the given builder in a frame or dialog,
	 * depending on {@link EntityPanel.Config#WINDOW_TYPE}.
	 * @param panelBuilder the entity panel builder
	 * @see #displayEntityPanelFrame(EntityPanel)
	 * @see #displayEntityPanelDialog(EntityPanel)
	 */
	protected final void displayEntityPanelWindow(EntityPanel.Builder panelBuilder) {
		displayEntityPanelWindow(entityPanel(panelBuilder));
	}

	/**
	 * Displays the given panel in a frame or dialog,
	 * depending on {@link EntityPanel.Config#WINDOW_TYPE}.
	 * @param entityPanel the entity panel
	 * @see #displayEntityPanelFrame(EntityPanel)
	 * @see #displayEntityPanelDialog(EntityPanel)
	 */
	protected final void displayEntityPanelWindow(EntityPanel entityPanel) {
		if (EntityPanel.Config.WINDOW_TYPE.is(WindowType.FRAME)) {
			displayEntityPanelFrame(entityPanel);
		}
		else {
			displayEntityPanelDialog(entityPanel);
		}
	}

	/**
	 * Displays a frame containing the given entity panel
	 * @param entityPanel the entity panel
	 */
	protected final void displayEntityPanelFrame(EntityPanel entityPanel) {
		if (requireNonNull(entityPanel).isShowing()) {
			Ancestor.window().of(entityPanel).toFront();
		}
		else {
			Frames.builder()
							.component(createEmptyBorderBasePanel(entityPanel))
							.locationRelativeTo(this)
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
							.onOpened(e -> entityPanel.activate())
							.onClosed(e -> onEntityPanelWindowClosed(entityPanel))
							.show();
		}
	}

	/**
	 * Displays a non-modal dialog containing the given entity panel
	 * @param entityPanel the entity panel
	 */
	protected final void displayEntityPanelDialog(EntityPanel entityPanel) {
		displayEntityPanelDialog(entityPanel, false);
	}

	/**
	 * Shows a dialog containing the given entity panel
	 * @param entityPanel the entity panel
	 * @param modal if true the dialog should be modal
	 */
	protected final void displayEntityPanelDialog(EntityPanel entityPanel, boolean modal) {
		if (requireNonNull(entityPanel).isShowing()) {
			Ancestor.window().of(entityPanel).toFront();
		}
		else {
			Dialogs.builder()
							.component(createEmptyBorderBasePanel(entityPanel))
							.owner(Ancestor.window().of(this).get())
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.onOpened(e -> entityPanel.activate())
							.onClosed(e -> onEntityPanelWindowClosed(entityPanel))
							.modal(modal)
							.show();
		}
	}

	/**
	 * To cancel the exit add a listener throwing a {@link CancelException}.
	 * @return an observer notified when the application is about to exit.
	 */
	protected final Observer<?> exiting() {
		return exiting.observer();
	}

	/**
	 * Creates the JMenuBar to use on the application Frame
	 * @return by default a JMenuBar based on the main menu controls or an empty Optional in case of no menu bar
	 * @see #createMainMenuControls()
	 */
	protected Optional<JMenuBar> createMenuBar() {
		return createMainMenuControls()
						.filter(controls -> controls.size() > 0)
						.map(mainMenuControls -> menu()
										.controls(mainMenuControls)
										.buildMenuBar());
	}

	/**
	 * <p>Called during the exit() method, override to write custom user preferences on program exit.
	 * <p>Remember to call super.writePreferences(preferences) when overriding.
	 * @param preferences the preferences instance to write to
	 */
	protected void writePreferences(Preferences preferences) {
		entityPanels().forEach(entityPanel -> entityPanel.writePreferences(preferences));
		try {
			ApplicationPreferences applicationPrefs = createPreferences();
			applicationPrefs.save(preferences);
			if (LEGACY_PREFERENCES.getOrThrow()) {
				entityPanels().forEach(EntityPanel::writeLegacyPreferences);
				applicationPrefs.saveLegacyPreferences(getClass());
			}
		}
		catch (Exception e) {
			LOG.error("Error while saving application preferences", e);
		}
	}

	/**
	 * <p>Called on application start, override to apply any custom preferences.
	 * <p>Remember to call super.savePreferences(preferences) when overriding.
	 * @param preferences the preferences instance containing the preferences to apply
	 */
	protected void applyPreferences(Preferences preferences) {
		requireNonNull(preferences);
		entityPanels.forEach(panel -> panel.applyPreferences(preferences));
	}

	private ApplicationPreferences createPreferences() {
		JFrame parentFrame = Ancestor.ofType(JFrame.class).of(this).get();

		return new ApplicationPreferences(
						saveDefaultUsername ? applicationModel.connectionProvider().user().username() : null,
						getLookAndFeel().getClass().getName(), Scaler.SCALING.getOrThrow(),
						parentFrame == null ? null : parentFrame.getSize(),
						parentFrame != null && (parentFrame.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH);
	}

	private Control createLookupPanelControl(EntityPanel.Builder panelBuilder) {
		return Control.builder()
						.command(() -> displayEntityPanelWindow(panelBuilder))
						.caption(panelBuilder.caption().orElse(applicationModel.entities().definition(panelBuilder.entityType()).caption()))
						.description(panelBuilder.description().orElse(null))
						.icon(panelBuilder.icon().orElse(null))
						.build();
	}

	private void configureEntityPanel(EntityPanel entityPanel) {
		entityPanel.linkSiblings(entityPanels);
		entityPanel.activated().addConsumer(applicationLayout::activated);
		if (entityPanel.containsEditPanel()) {
			entityPanel.editPanel().active().addConsumer(new DisplayActivatedPanel(entityPanel));
		}
	}

	private EntityPanel entityPanel(EntityPanel.Builder panelBuilder) {
		if (CACHE_ENTITY_PANELS.getOrThrow() && cachedEntityPanels.containsKey(panelBuilder)) {
			return cachedEntityPanels.get(panelBuilder);
		}

		EntityPanel entityPanel = panelBuilder.build(applicationModel.connectionProvider());
		if (userPreferences) {
			if (LEGACY_PREFERENCES.getOrThrow()) {
				entityPanel.applyLegacyPreferences();
			}
			entityPanel.applyPreferences(applicationModel.preferences());
		}
		entityPanel.initialize();
		if (CACHE_ENTITY_PANELS.getOrThrow()) {
			cachedEntityPanels.put(panelBuilder, entityPanel);
		}

		return entityPanel;
	}

	private void applyPreferences() {
		if (!userPreferences) {
			LOG.debug("User preferences are disabled");
			return;
		}
		if (restoreDefaultPreferences) {
			LOG.debug("Restoring default user preferences for EntityPanels: {}", entityPanels);
			return;
		}
		LOG.debug("Applying user preferences");
		if (LEGACY_PREFERENCES.getOrThrow()) {
			applyLegacyPreferences();
		}
		applyPreferences(applicationModel.preferences());
	}

	private void applyLegacyPreferences() {
		entityPanels.forEach(EntityPanel::applyLegacyPreferences);
	}

	private static JPanel createEmptyBorderBasePanel(EntityPanel entityPanel) {
		int gap = Layouts.GAP.getOrThrow();
		return Components.borderLayoutPanel()
						.center(entityPanel)
						.border(createEmptyBorder(gap, gap, 0, gap))
						.build();
	}

	private JScrollPane createApplicationTree() {
		return createTree(createApplicationTree(entityPanels));
	}

	private JScrollPane createDependencyTree() {
		return createTree(createDependencyTreeModel(applicationModel.entities()));
	}

	private void handleUnsavedModifications() {
		Collection<EntityPanel> modified = modified(entityPanels);
		if (modifiedWarning && !modified.isEmpty() && showConfirmDialog(this,
						createModifiedMessage(modified), FrameworkMessages.modifiedWarningTitle(),
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
			modified.iterator().next().activate();

			throw new CancelException();
		}
	}

	private void selectScaling() {
		Scaler.SCALING.set(Dialogs.select()
						.scaling()
						.owner(this)
						.show(resourceBundle.getString("scaling")));

		showMessageDialog(this, resourceBundle.getString("scaling_selected_message"));
	}

	private boolean confirmExit() {
		return !CONFIRM_EXIT.getOrThrow() || showConfirmDialog(this,
						FrameworkMessages.confirmExit(), FrameworkMessages.confirmExitTitle(),
						JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	private static Collection<EntityPanel> modified(Collection<EntityPanel> panels) {
		Collection<EntityPanel> modifiedPanels = new ArrayList<>();
		for (EntityPanel panel : panels) {
			EntityEditModel editModel = panel.editModel();
			if (editModel.editor().modified().is()) {
				modifiedPanels.add(panel);
			}
			modifiedPanels.addAll(modified(panel.detailPanels().get()));
		}

		return modifiedPanels;
	}

	private static String createModifiedMessage(Collection<EntityPanel> modified) {
		return modified.stream()
						.map(EntityPanel::caption)
						.collect(joining(", ")) + "\n" + FrameworkMessages.modifiedWarning();
	}

	private static Map<Object, State> createLogLevelStateMap() {
		LoggerProxy loggerProxy = LoggerProxy.instance();
		if (loggerProxy == LoggerProxy.NONE) {
			return Collections.emptyMap();
		}
		Object currentLogLevel = loggerProxy.getLogLevel(loggerProxy.rootLogger());
		Map<Object, State> levelStateMap = new LinkedHashMap<>();
		State.Group logLevelStateGroup = State.group();
		for (Object logLevel : loggerProxy.levels()) {
			State logLevelState = State.state(Objects.equals(logLevel, currentLogLevel));
			logLevelStateGroup.add(logLevelState);
			logLevelState.when(true)
							.addListener(() -> loggerProxy.setLogLevel(loggerProxy.rootLogger(), logLevel));
			levelStateMap.put(logLevel, logLevelState);
		}

		return Collections.unmodifiableMap(levelStateMap);
	}

	private Optional<Controls> createOpenLogControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(resourceBundle.getString("open_log"));
		createLogFileControl().ifPresent(builder::control);
		createLogFolderControl().ifPresent(builder::control);

		return Optional.of(builder.build());
	}

	private Optional<Controls> createTracingControls() {
		EntityConnectionProvider connectionProvider = applicationModel.connectionProvider();
		if (connectionProvider instanceof EntityConnectionTracer && SQL_TRACING.getOrThrow()) {
			EntityConnectionTracer tracer = (EntityConnectionTracer) connectionProvider;

			return Optional.of(Controls.builder()
							.caption(resourceBundle.getString("sql_tracing"))
							.control(Control.builder()
											.toggle(tracer.tracing())
											.caption(resourceBundle.getString("sql_tracing_enabled")))
							.control(Control.builder()
											.command(() -> viewTrace(tracer))
											.caption(resourceBundle.getString("view_sql_trace") + "...")
											.enabled(tracer.tracing()))
							.build());
		}

		return Optional.empty();
	}

	private Optional<Control> createLogFileControl() {
		return firstLogFile()
						.map(firstLogFile -> Control.builder()
										.command(() -> Desktop.getDesktop().open(firstLogFile))
										.caption(resourceBundle.getString("open_log_file"))
										.smallIcon(getIcon("FileView.fileIcon"))
										.description(firstLogFile.getAbsolutePath())
										.build());
	}

	private Optional<Control> createLogFolderControl() {
		return firstLogFile()
						.map(File::getParentFile)
						.map(logFileFolder -> Control.builder()
										.command(() -> Desktop.getDesktop().open(logFileFolder))
										.caption(resourceBundle.getString("open_log_folder"))
										.smallIcon(getIcon("FileView.directoryIcon"))
										.description(logFileFolder.getAbsolutePath())
										.build());
	}

	private Control createDisplaySystemPropertiesControl() {
		return Control.builder()
						.command(() -> Dialogs.builder()
										.component(Components.textArea()
														.value(PropertyStore.systemProperties())
														.editable(false)
														.font(font -> new Font(Font.MONOSPACED, font.getStyle(), font.getSize()))
														.popupControl(textArea -> Control.builder()
																		.command(() -> {
																			String text = textArea.getSelectedText();
																			if (Text.nullOrEmpty(text)) {
																				text = textArea.getText();
																			}
																			Utilities.setClipboard(text);
																		})
																		.caption(Messages.copy())
																		.icon(FrameworkIcons.instance().copy())
																		.build())
														.scrollPane())
										.owner(this)
										.size(screenSizeRatio(0.33))
										.title(resourceBundle.getString("system_properties"))
										.show())
						.caption("ⓘ")
						.font(monospaceFont())
						.build();
	}

	private static Font monospaceFont() {
		Font font = UIManager.getFont("TextArea.font");

		return new Font(Font.MONOSPACED, font.getStyle(), font.getSize());
	}

	private void viewTrace(EntityConnectionTracer tracer) {
		if (sqlTraceViewer == null) {
			sqlTraceViewer = createTraceViewer(tracer);
		}
		if (sqlTraceViewer.isShowing()) {
			Ancestor.window().of(sqlTraceViewer).toFront();
		}
		else {
			Dialogs.builder()
							.component(sqlTraceViewer)
							.owner(this)
							.title(resourceBundle.getString("sql_tracing"))
							.size(screenSizeRatio(0.5))
							.modal(false)
							.show();
		}
	}

	private void setupTracing() {
		if (SQL_TRACING.getOrThrow()) {
			EntityConnectionProvider connectionProvider = applicationModel.connectionProvider();
			if (connectionProvider instanceof EntityConnectionTracer) {
				EntityConnectionTracer tracer = (EntityConnectionTracer) connectionProvider;
				if (tracer.tracing().is()) {
					sqlTraceViewer = createTraceViewer(tracer);
				}
				tracer.tracing().addConsumer(this::tracingChanged);
			}
		}
	}

	private void tracingChanged(boolean tracing) {
		EntityConnectionProvider connectionProvider = applicationModel.connectionProvider();
		if (connectionProvider instanceof EntityConnectionTracer) {
			if (tracing) {
				if (sqlTraceViewer == null) {
					sqlTraceViewer = createTraceViewer((EntityConnectionTracer) connectionProvider);
				}
			}
			else if (sqlTraceViewer != null) {
				sqlTraceViewer.clear();
			}
		}
	}

	private LogViewer createTraceViewer(EntityConnectionTracer tracer) {
		LogViewer logViewer = LogViewer.logViewer();
		logViewer.setBorder(emptyBorder());
		tracer.trace().addConsumer(trace -> {
			StringBuilder logBuilder = new StringBuilder();
			trace.appendTo(logBuilder);
			sqlTraceViewer.append(logBuilder.toString());
		});

		return logViewer;
	}

	private static Optional<File> firstLogFile() {
		Collection<String> files = LoggerProxy.instance().files();
		if (files.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(new File(files.iterator().next()));
	}

	private void advancedLogLevelConfiguration() {
		Dialogs.builder()
						.component(borderLayoutPanel()
										.center(logLevelPanel())
										.border(emptyBorder()))
						.owner(this)
						.title(resourceBundle.getString(ADVANCED_LOG_LEVEL))
						.show();
	}

	private static JScrollPane createTree(TreeModel treeModel) {
		JTree tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		tree.setToggleClickCount(1);
		tree.setRootVisible(false);
		expandAll(tree, new TreePath(tree.getModel().getRoot()));

		return new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	private static void expandAll(JTree tree, TreePath parent) {
		TreeNode node = (TreeNode) parent.getLastPathComponent();
		if (node.getChildCount() >= 0) {
			Enumeration<? extends TreeNode> e = node.children();
			while (e.hasMoreElements()) {
				expandAll(tree, parent.pathByAddingChild(e.nextElement()));
			}
		}
		tree.expandPath(parent);
	}

	private static DefaultTreeModel createApplicationTree(Collection<? extends EntityPanel> entityPanels) {
		DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
		addPanelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

		return applicationTreeModel;
	}

	private static void addPanelsToTree(DefaultMutableTreeNode root, Collection<? extends EntityPanel> panels) {
		for (EntityPanel entityPanel : panels) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel.caption());
			node.setUserObject(entityPanel);
			root.add(node);
			if (!entityPanel.detailPanels().get().isEmpty()) {
				addPanelsToTree(node, entityPanel.detailPanels().get());
			}
		}
	}

	private static boolean referencesOnlySelf(Entities entities, EntityType entityType) {
		return entities.definition(entityType).foreignKeys().get().stream()
						.allMatch(foreignKey -> foreignKey.referencedType().equals(entityType));
	}

	private void onEntityPanelWindowClosed(EntityPanel entityPanel) {
		entityPanel.writePreferences(applicationModel.preferences());
		entityPanel.setPreferredSize(entityPanel.getSize());
	}

	void setSaveDefaultUsername(boolean saveDefaultUsername) {
		this.saveDefaultUsername = saveDefaultUsername;
	}

	private final class DisplayActivatedPanel implements Consumer<Boolean> {

		private final EntityPanel entityPanel;

		private DisplayActivatedPanel(EntityPanel entityPanel) {
			this.entityPanel = entityPanel;
		}

		@Override
		public void accept(Boolean active) {
			if (active) {
				applicationLayout.activated(entityPanel);
			}
		}
	}

	private static final class LookupPanelBuilderComparator implements Comparator<EntityPanel.Builder> {

		private final Entities entities;
		private final Comparator<String> comparator = Text.collator();

		private LookupPanelBuilderComparator(Entities entities) {
			this.entities = entities;
		}

		@Override
		public int compare(EntityPanel.Builder ep1, EntityPanel.Builder ep2) {
			String caption1 = ep1.caption().orElse(entities.definition(ep1.entityType()).caption());
			String caption2 = ep2.caption().orElse(entities.definition(ep2.entityType()).caption());

			return comparator.compare(caption1, caption2);
		}
	}

	private static final class EntityDependencyTreeNode extends DefaultMutableTreeNode {

		private final Entities entities;

		private EntityDependencyTreeNode(EntityType entityType, Entities entities) {
			super(requireNonNull(entityType));
			this.entities = entities;
		}

		/**
		 * @return the type of the entity this node represents
		 */
		public EntityType entityType() {
			return (EntityType) getUserObject();
		}

		@Override
		public void setParent(MutableTreeNode newParent) {
			super.setParent(newParent);
			removeAllChildren();
			for (EntityDependencyTreeNode child : getChildren()) {
				add(child);
			}
		}

		private List<EntityDependencyTreeNode> getChildren() {
			List<EntityDependencyTreeNode> childrenList = new ArrayList<>();
			for (EntityDefinition definition : entities.definitions()) {
				for (ForeignKeyDefinition foreignKeyDefinition : definition.foreignKeys().definitions()) {
					if (foreignKeyDefinition.attribute().referencedType().equals(entityType()) && !foreignKeyDefinition.soft()
									&& !foreignKeyCycle(foreignKeyDefinition.attribute().referencedType())) {
						childrenList.add(new EntityDependencyTreeNode(definition.type(), entities));
					}
				}
			}

			return childrenList;
		}

		private boolean foreignKeyCycle(EntityType referencedEntityType) {
			TreeNode tmp = getParent();
			while (tmp instanceof EntityDependencyTreeNode) {
				if (((EntityDependencyTreeNode) tmp).entityType().equals(referencedEntityType)) {
					return true;
				}
				tmp = tmp.getParent();
			}

			return false;
		}
	}

	/**
	 * Handles laying out an EntityApplicationPanel.
	 */
	public interface ApplicationLayout {

		/**
		 * Lays out the main component for a given application panel.
		 * Note that this method is responsible for initializing any visible entity panels using {@link EntityPanel#initialize()}.
		 * @return the main application component
		 * @throws IllegalStateException in case the panel has already been laid out
		 */
		JComponent layout();

		/**
		 * Called when the given entity panel is activated,
		 * responsible for making sure it becomes visible.
		 * @param entityPanel the entity panel being activated
		 * @see EntityPanel#activate()
		 * @see EntityPanel#activated()
		 */
		default void activated(EntityPanel entityPanel) {}
	}
}
