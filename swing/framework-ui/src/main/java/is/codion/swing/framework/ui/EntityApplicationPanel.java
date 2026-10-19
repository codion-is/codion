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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.logging.LoggerProxy;
import is.codion.common.utilities.property.PropertyStore;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.common.utilities.version.Version;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionTracer;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.ui.UIManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.ancestor.Ancestor;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.logging.LogViewer;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.CommandControl;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlKey;
import is.codion.swing.common.ui.control.ControlMap;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.Controls.ControlsKey;
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
import is.codion.swing.framework.model.SwingEntityEditModel;
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
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import static is.codion.common.model.preferences.FilePreferences.filePreferences;
import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.Configuration.stringValue;
import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.logging.LogLevelPanel.logLevelPanel;
import static is.codion.swing.common.ui.control.ControlMap.controlMap;
import static is.codion.swing.common.ui.window.Windows.screenSizeRatio;
import static java.awt.Frame.MAXIMIZED_BOTH;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
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
	public static final PropertyValue<Boolean> SYSTEM_EXIT =
					booleanValue(EntityApplicationPanel.class.getName() + ".systemExit", false);

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
	 * The controls of the main menu, the menus included, available via {@link #control(ControlKey)} and laid out via
	 * {@link #configureMainMenu(Consumer)}.
	 * <p>The menus with a default layout, {@link #FILE_MENU}, {@link #VIEW_MENU}, {@link #TOOLS_MENU} and {@link #HELP_MENU},
	 * can be configured via {@link Controls.Layout#controls(ControlsKey, Consumer)}, the others, created from what the
	 * application has, {@link #LOOKUP_MENU} and {@link #LOG_CONTROLS}, only placed or left out.
	 */
	public static final class ControlKeys {

		/**
		 * Exits the application.
		 */
		public static final ControlKey<CommandControl> EXIT = CommandControl.key("exit");
		/**
		 * Displays a dialog for selecting the look and feel.
		 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
		 * @see LookAndFeelProvider#findLookAndFeel(String)
		 * @see LookAndFeelComboBox#ENABLE_ON_SELECTION
		 */
		public static final ControlKey<CommandControl> SELECT_LOOK_AND_FEEL = CommandControl.key("selectLookAndFeel");
		/**
		 * Displays a dialog for selecting the scaling.
		 */
		public static final ControlKey<CommandControl> SELECT_SCALING = CommandControl.key("selectScaling");
		/**
		 * Toggles whether the application window is always on top.
		 */
		public static final ControlKey<ToggleControl> ALWAYS_ON_TOP = ToggleControl.key("alwaysOnTop");
		/**
		 * Displays the help.
		 * @see #HELP_URL
		 */
		public static final ControlKey<CommandControl> HELP = CommandControl.key("help");
		/**
		 * Displays the keyboard shortcuts.
		 */
		public static final ControlKey<CommandControl> KEYBOARD_SHORTCUTS = CommandControl.key("keyboardShortcuts");
		/**
		 * The log controls: the log level, opening the log file or folder, and the SQL tracing, those available.
		 * @see #SQL_TRACING
		 */
		public static final ControlKey<Controls> LOG_CONTROLS = Controls.key("logControls");
		/**
		 * Displays information about the application.
		 */
		public static final ControlKey<CommandControl> ABOUT = CommandControl.key("about");
		/**
		 * The File menu.
		 * <p>Default layout: {@link #EXIT}
		 */
		public static final ControlsKey FILE_MENU = Controls.key("fileMenu", Controls.layout(singletonList(EXIT)));
		/**
		 * The View menu.
		 * <p>Default layout: {@link #SELECT_LOOK_AND_FEEL}, {@link #SELECT_SCALING}, separator, {@link #ALWAYS_ON_TOP}
		 */
		public static final ControlsKey VIEW_MENU = Controls.key("viewMenu",
						Controls.layout(asList(SELECT_LOOK_AND_FEEL, SELECT_SCALING, ControlKey.SEPARATOR, ALWAYS_ON_TOP)));
		/**
		 * The Tools menu, empty by default, and so left out.
		 */
		public static final ControlsKey TOOLS_MENU = Controls.key("toolsMenu", Controls.layout(emptyList()));
		/**
		 * The Lookup menu, a control for each lookup panel, not available without lookup panels.
		 */
		public static final ControlKey<Controls> LOOKUP_MENU = Controls.key("lookupMenu");
		/**
		 * The Help menu.
		 * <p>Default layout: {@link #HELP}, {@link #KEYBOARD_SHORTCUTS}, separator, {@link #LOG_CONTROLS}, separator,
		 * {@link #ABOUT}
		 */
		public static final ControlsKey HELP_MENU = Controls.key("helpMenu",
						Controls.layout(asList(HELP, KEYBOARD_SHORTCUTS, ControlKey.SEPARATOR, LOG_CONTROLS, ControlKey.SEPARATOR, ABOUT)));

		private ControlKeys() {}
	}

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
	// ENTITIES is the shared parent node navigated by both the model walk (EntityApplicationModel) and the UI walk
	private static final String ENTITIES = "entities";
	private static final String AUXILIARY = "auxiliary";
	private static final String VERSION = "version";
	private static final String VERSION_VALUE = "2";
	private static final NumberFormat MEMORY_USAGE_FORMAT = NumberFormat.getIntegerInstance();
	private static final Runtime RUNTIME = Runtime.getRuntime();

	private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

	// Non-static so that Locale.setDefault(...) can be called in the main method of a subclass
	private final MessageBundle resourceBundle =
					messageBundle(EntityApplicationPanel.class, getBundle(EntityApplicationPanel.class.getName()));

	private final M model;
	private final Collection<EntityPanel.Builder> lookupPanelBuilders;
	private final List<EntityPanel> entityPanels;
	private final ApplicationLayout applicationLayout;

	private final State alwaysOnTopState = State.builder()
					.consumer(alwaysOnTop -> Ancestor.window().of(this).optional()
									.ifPresent(parent -> parent.setAlwaysOnTop(alwaysOnTop)))
					.build();
	private final Event<EntityApplicationPanel<M>> exiting = Event.event();
	private final Event<EntityApplicationPanel<M>> initializedEvent = Event.event();
	private final boolean modifiedWarning = EntityEditPanel.Config.MODIFIED_WARNING.getOrThrow();
	private final boolean userPreferences = EntityApplicationModel.USER_PREFERENCES.getOrThrow();

	private final Map<EntityPanel.Builder, EntityPanel> cachedEntityPanels = new HashMap<>();
	private final Set<EntityPanel> displayedEntityPanels = new LinkedHashSet<>();

	private final Map<Object, State> logLevelStates = createLogLevelStateMap();
	private final ControlMap controlMap = controlMap(ControlKeys.class);
	private final Controls.Layout mainMenuLayout = Controls.layout(asList(ControlKeys.FILE_MENU, ControlKeys.VIEW_MENU,
					ControlKeys.TOOLS_MENU, ControlKeys.LOOKUP_MENU, ControlKeys.HELP_MENU));

	// The preferences root, provided by EntityApplication or resolved lazily from the domain for a stand-alone panel
	private @Nullable Preferences preferences;

	private @Nullable LogViewer sqlTraceViewer;

	private boolean saveDefaultUsername = true;
	private boolean initialized = false;
	private boolean initializing = false;

	/**
	 * Instantiates a new {@link EntityApplicationPanel} based on the given application model,
	 * using the default {@link TabbedApplicationLayout}.
	 * @param model the application model
	 * @param entityPanels the entity panels
	 * @param lookupPanelBuilders the support panel builders
	 */
	public EntityApplicationPanel(M model, List<EntityPanel> entityPanels,
																Collection<EntityPanel.Builder> lookupPanelBuilders) {
		this(model, entityPanels, lookupPanelBuilders, TabbedApplicationLayout::new);
	}

	/**
	 * Instantiates a new {@link EntityApplicationPanel} based on the given application model,
	 * using the {@link ApplicationLayout} provided by {@code applicationLayout}.
	 * @param model the application model
	 * @param entityPanels the entity panels
	 * @param lookupPanelBuilders the support panel builders
	 * @param applicationLayout provides the application layout
	 */
	public EntityApplicationPanel(M model, List<EntityPanel> entityPanels,
																Collection<EntityPanel.Builder> lookupPanelBuilders,
																Function<EntityApplicationPanel<M>, ApplicationLayout> applicationLayout) {
		this.model = requireNonNull(model);
		this.entityPanels = unmodifiableList(new ArrayList<>(requireNonNull(entityPanels)));
		this.lookupPanelBuilders = requireNonNull(lookupPanelBuilders);
		this.applicationLayout = requireNonNull(applicationLayout).apply(this);
		entityPanels.forEach(this::configureEntityPanel);
		setupTracing();
		createControls();
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
		Dialogs.exception()
						.owner(Ancestor.window().of(focusOwner).get())
						.show(exception);
	}

	/**
	 * @return the application model this application panel is based on
	 */
	public final M model() {
		return model;
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
	public final List<EntityPanel> panels() {
		return entityPanels;
	}

	/**
	 * @return a State controlling the alwaysOnTop state of this panels parent window
	 */
	public final State alwaysOnTop() {
		return alwaysOnTopState;
	}

	/**
	 * @return an observer notified when this application panel is initialized
	 * @see #initialize()
	 */
	public final Observer<EntityApplicationPanel<M>> initialized() {
		return initializedEvent.observer();
	}

	/**
	 * Exits this application. Calls {@link System#exit(int)} in case {@link #SYSTEM_EXIT} is true.
	 * <p>Disposes all windows in the JVM so that it can end naturally when {@link #SYSTEM_EXIT} is false,
	 * note that this closes the windows of any other Codion application sharing the JVM.
	 * @throws CancelException if the exit is cancelled
	 * @see #exiting()
	 * @see EntityEditPanel.Config#MODIFIED_WARNING
	 * @see EntityApplicationPanel#CONFIRM_EXIT
	 * @see EntityApplicationPanel#SYSTEM_EXIT
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
		storePreferences();
		try {
			model.connection().close();
		}
		catch (Exception e) {
			LOG.debug("Exception while disconnecting from database", e);
		}
		for (Window window : Window.getWindows()) {
			try {
				window.dispose();
			}
			catch (Exception e) {
				LOG.debug("Exception while disposing window on exit: {}", window, e);
			}
		}
		if (SYSTEM_EXIT.getOrThrow()) {
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
	 * Displays a keyboard shortcut overview panel, showing the default key strokes of the controls,
	 * see {@link is.codion.swing.common.ui.control.ControlKey#defaultKeystroke()}.
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
	 * Initializes this panel and marks it as initialized, subsequent calls have no effect, nor has a call
	 * made while the initialization is in progress.
	 * @return this application panel
	 */
	public final EntityApplicationPanel<M> initialize() {
		if (!initialized && !initializing) {
			initializing = true;
			LOG.debug("{} - initializing", this);
			try {
				setupMenus();
				restorePreferences();
				setLayout(new BorderLayout());
				add(applicationLayout.layout(), BorderLayout.CENTER);
				bindEvents();
				initializedEvent.accept(this);
			}
			finally {
				initializing = false;
				initialized = true;
			}
		}

		return this;
	}

	/**
	 * Restores preferences for this application panel and its sub-panels.
	 * Override to apply any custom preferences.
	 * <p>Remember to call {@code super.restore(preferences)} when overriding.
	 * @param preferences the preferences instance from which to restore
	 */
	public void restore(Preferences preferences) {
		requireNonNull(preferences);
		LOG.debug("Restoring user preferences");
		restoreEntityPanelPreferences(preferences);
	}

	/**
	 * Stores application preferences. Override to store custom preferences.
	 * <p>Remember to call {@code super.store(preferences)} when overriding.
	 * <p>The keys {@code application} and {@code version} are stored in the given node, along with the child nodes
	 * {@code entities} and {@code auxiliary}, so custom preferences need other names, preferably a child node of their own.
	 * @param preferences the Preferences instance to write to
	 */
	public void store(Preferences preferences) {
		requireNonNull(preferences);
		storeApplicationPreferences(preferences);
		storeEntityPanelPreferences(preferences);
		storeCachedPanelPreferences(preferences);
		storeDisplayedPanelPreferences(preferences);
	}

	/**
	 * <p>Requests the initial application focus by calling {@link EntityPanel#requestInitialFocus()}
	 * on the main entity panel.
	 * <p>By default, the main entity panel is the first one returned by {@link #panels()}.
	 */
	public void requestInitialFocus() {
		if (!entityPanels.isEmpty()) {
			entityPanels.get(0).requestInitialFocus();
		}
	}

	/**
	 * Returns a {@link Value} containing the control associated with {@code controlKey},
	 * an empty {@link Value} if no such control is available.
	 * @param <T> the control type
	 * @param controlKey the control key
	 * @return the {@link Value} containing the control associated with {@code controlKey}
	 * @see ControlKeys
	 */
	public final <T extends Control> Value<T> control(ControlKey<T> controlKey) {
		return controlMap.control(requireNonNull(controlKey));
	}

	/**
	 * Configures the main menu layout, before this panel is initialized.<br>
	 * Note that the {@link Controls.Layout} instance has pre-configured defaults,
	 * which must be cleared in order to start with an empty configuration.
	 * {@snippet class = "is.codion.demos.chinook.javadoc.EntityApplicationPanelSnippets" region = "configureMainMenu":
	 * configureMainMenu(layout -> layout // @start region=configureMainMenu
	 *         .controls(ControlKeys.TOOLS_MENU, tools -> tools
	 *                 .control(createImportControl()))
	 *         .controls(ControlKeys.VIEW_MENU, view -> view
	 *                 .after(ControlKeys.SELECT_SCALING)
	 *                 .control(createSelectLanguageControl()))
	 *         .after(ControlKeys.TOOLS_MENU)
	 *         .control(createReportsMenu())); // @end}
	 * Defaults:
	 * <ul>
	 *   <li>{@link ControlKeys#FILE_MENU ControlKeys#FILE_MENU}
	 *   <li>{@link ControlKeys#VIEW_MENU ControlKeys#VIEW_MENU}
	 *   <li>{@link ControlKeys#TOOLS_MENU ControlKeys#TOOLS_MENU}
	 *   <li>{@link ControlKeys#LOOKUP_MENU ControlKeys#LOOKUP_MENU} (If lookup panels are available)
	 *   <li>{@link ControlKeys#HELP_MENU ControlKeys#HELP_MENU}
	 * </ul>
	 * A menu left empty is left out.
	 * @param mainMenuLayout provides access to the main menu layout
	 * @throws IllegalStateException in case this panel has already been initialized
	 * @see Controls.Layout#clear()
	 */
	protected final void configureMainMenu(Consumer<Controls.Layout> mainMenuLayout) {
		throwIfInitialized();
		requireNonNull(mainMenuLayout).accept(this.mainMenuLayout);
	}

	/**
	 * @return the panel shown when Help -&#62; About is selected
	 */
	protected JPanel createAboutPanel() {
		PanelBuilder<GridLayout, ?> versionMemoryPanel = gridLayoutPanel(0, 2)
						.border(emptyBorder());
		model.connection().clientVersion().ifPresent(version -> versionMemoryPanel
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
						.west(new JLabel(FrameworkIcons.instance().logo().large()))
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
			displayedEntityPanels.add(entityPanel);
			Frames.builder()
							.component(createEmptyBorderBasePanel(entityPanel))
							.locationRelativeTo(this)
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
							.onOpened(e -> entityPanel.activation().request())
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
			displayedEntityPanels.add(entityPanel);
			Dialogs.builder()
							.component(createEmptyBorderBasePanel(entityPanel))
							.owner(Ancestor.window().of(this).get())
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.onOpened(e -> entityPanel.activation().request())
							.onClosed(e -> onEntityPanelWindowClosed(entityPanel))
							.modal(modal)
							.show();
		}
	}

	/**
	 * To cancel the exit add a listener throwing a {@link CancelException}.
	 * @return an observer notified when the application is about to exit.
	 */
	protected final Observer<EntityApplicationPanel<M>> exiting() {
		return exiting.observer();
	}

	/**
	 * Creates the JMenuBar to use on the application Frame
	 * @return by default a JMenuBar based on the main menu layout or an empty Optional in case of no menu bar
	 * @see #configureMainMenu(Consumer)
	 */
	protected Optional<JMenuBar> createMenuBar() {
		Controls mainMenuControls = mainMenuLayout.create(controlMap);

		return mainMenuControls.size() == 0 ? Optional.empty() : Optional.of(menu()
						.controls(mainMenuControls)
						.buildMenuBar());
	}

	// Provided by EntityApplication at startup (already migrated); package private
	void preferences(Preferences preferences) {
		this.preferences = requireNonNull(preferences);
	}

	// The application-provided root, or a lazily resolved (and migrated) default for a stand-alone panel
	private Preferences preferences() {
		if (preferences == null) {
			preferences = filePreferences(EntityApplicationModel.PREFERENCES_KEY.optional()
							.orElse(model.entities().domainType().name()));
			PreferencesMigrator.migrate(preferences);
		}

		return preferences;
	}

	private void restorePreferences() {
		if (userPreferences) {
			Preferences preferences = preferences();
			// The model walk restores the model-owned state (conditions, filters, sort), the UI walk the view state
			model.restore(preferences.node(ENTITIES));
			restore(preferences);
		}
		else {
			LOG.debug("User preferences are disabled");
		}
	}

	private void storePreferences() {
		try {
			if (userPreferences) {
				LOG.debug("Writing user preferences");
				Preferences preferences = preferences();
				model.store(preferences.node(ENTITIES));
				store(preferences);
				preferences.put(VERSION, VERSION_VALUE);
				preferences.flush();
			}
		}
		catch (Throwable e) {
			LOG.error("Exception while storing preferences", e);
		}
	}

	private void storeEntityPanelPreferences(Preferences preferences) {
		if (!entityPanels.isEmpty()) {
			Preferences node = preferences.node(ENTITIES);
			for (EntityPanel panel : entityPanels) {
				try {
					panel.store(node.node(panel.preferencesKey()));
				}
				catch (Exception e) {
					LOG.error("Error storing preferences for entity panel: {}", panel.preferencesKey(), e);
				}
			}
		}
	}

	private void restoreEntityPanelPreferences(Preferences preferences) {
		if (!entityPanels.isEmpty()) {
			LOG.debug("Restoring entity panel preferences");
			Preferences node = preferences.node(ENTITIES);
			for (EntityPanel panel : entityPanels) {
				try {
					panel.restore(node.node(panel.preferencesKey()));
				}
				catch (Exception e) {
					LOG.error("Error restoring preferences for entity panel: {}", panel.preferencesKey(), e);
				}
			}
		}
	}

	private void storeCachedPanelPreferences(Preferences preferences) {
		if (!cachedEntityPanels.isEmpty()) {
			for (Map.Entry<EntityPanel.Builder, EntityPanel> entry : cachedEntityPanels.entrySet()) {
				EntityPanel entityPanel = entry.getValue();
				try {
					entityPanel.store(preferences.node(AUXILIARY).node(entityPanel.preferencesKey()));
				}
				catch (Exception e) {
					LOG.error("Error storing preferences for auxiliary panel: {}", entityPanel.preferencesKey(), e);
				}
			}
		}
	}

	private void storeDisplayedPanelPreferences(Preferences preferences) {
		for (EntityPanel entityPanel : displayedEntityPanels) {
			//cached panels are already stored above, and exit() disposes windows after this,
			//so the windowClosed store() would run after the final preferences flush
			if (!cachedEntityPanels.containsValue(entityPanel)) {
				try {
					entityPanel.store(preferences.node(AUXILIARY).node(entityPanel.preferencesKey()));
				}
				catch (Exception e) {
					LOG.error("Error storing preferences for auxiliary panel: {}", entityPanel.preferencesKey(), e);
				}
			}
		}
	}

	private void storeApplicationPreferences(Preferences preferences) {
		try {
			createApplicationPreferences().save(preferences);
		}
		catch (Exception e) {
			LOG.error("Error while storing application preferences", e);
		}
	}

	private ApplicationPreferences createApplicationPreferences() {
		JFrame parentFrame = Ancestor.ofType(JFrame.class).of(this).get();
		boolean maximized = parentFrame != null && (parentFrame.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;

		return new ApplicationPreferences(
						saveDefaultUsername ? model.connection().user().username() : null,
						getLookAndFeel().getClass().getName(), Scaler.SCALING.getOrThrow(),
						//when maximized parentFrame.getSize() is the screen size, not the frame size to restore when un-maximized
						parentFrame == null || maximized ? null : parentFrame.getSize(),
						maximized);
	}

	private Control createLookupPanelControl(EntityPanel.Builder panelBuilder) {
		return Control.builder()
						.command(() -> displayEntityPanelWindow(panelBuilder))
						.caption(panelBuilder.caption().orElse(model.entities().definition(panelBuilder.entityType()).caption()))
						.description(panelBuilder.description().orElse(null))
						.icon(panelBuilder.icon().orElse(null))
						.build();
	}

	private void createControls() {
		controlMap.control(ControlKeys.EXIT).set(exitControl());
		controlMap.control(ControlKeys.SELECT_LOOK_AND_FEEL).set(selectLookAndFeelControl());
		controlMap.control(ControlKeys.SELECT_SCALING).set(selectScalingControl());
		controlMap.control(ControlKeys.ALWAYS_ON_TOP).set(alwaysOnTopControl());
		controlMap.control(ControlKeys.HELP).set(helpControl());
		controlMap.control(ControlKeys.KEYBOARD_SHORTCUTS).set(keyboardShortcutsControl());
		controlMap.control(ControlKeys.LOG_CONTROLS).set(logControls());
		controlMap.control(ControlKeys.ABOUT).set(aboutControl());
	}

	// The menus, unless already set, laid out by their keys' layouts, with the controls as configured before the
	// initialization, a menu configured via the main menu layout taking only its caption and mnemonic from these
	private void setupMenus() {
		setupMenu(ControlKeys.FILE_MENU, () -> layoutMenu(ControlKeys.FILE_MENU,
						FrameworkMessages.file(), FrameworkMessages.fileMnemonic()));
		setupMenu(ControlKeys.VIEW_MENU, () -> layoutMenu(ControlKeys.VIEW_MENU,
						FrameworkMessages.view(), FrameworkMessages.viewMnemonic()));
		setupMenu(ControlKeys.TOOLS_MENU, () -> layoutMenu(ControlKeys.TOOLS_MENU,
						resourceBundle.getString("tools"), resourceBundle.getString("tools_mnemonic").charAt(0)));
		setupMenu(ControlKeys.LOOKUP_MENU, this::lookupMenu);
		setupMenu(ControlKeys.HELP_MENU, () -> layoutMenu(ControlKeys.HELP_MENU,
						resourceBundle.getString(HELP), resourceBundle.getString("help_mnemonic").charAt(0)));
	}

	private void setupMenu(ControlKey<Controls> menuKey, Supplier<@Nullable Controls> menu) {
		Value<Controls> menuControls = controlMap.control(menuKey);
		if (menuControls.isNull()) {
			menuControls.set(menu.get());
		}
	}

	// Null without lookup panels
	private @Nullable Controls lookupMenu() {
		return lookupPanelBuilders.isEmpty() ? null : Controls.builder()
						.caption(FrameworkMessages.lookup())
						.mnemonic(FrameworkMessages.lookupMnemonic())
						.controls(lookupPanelBuilders.stream()
										.sorted(new LookupPanelBuilderComparator(model.entities()))
										.map(this::createLookupPanelControl)
										.collect(toList()))
						.build();
	}

	// The menu's items laid out by its key's default layout
	private Controls layoutMenu(ControlsKey menuKey, String caption, char mnemonic) {
		return Controls.builder()
						.caption(caption)
						.mnemonic(mnemonic)
						.actions(menuKey.defaultLayout()
										.orElseThrow(IllegalStateException::new)
										.create(controlMap)
										.actions())
						.build();
	}

	private void throwIfInitialized() {
		if (initialized) {
			throw new IllegalStateException("Method must be called before the panel is initialized");
		}
	}

	private CommandControl exitControl() {
		return Control.builder()
						.command(this::exit)
						.caption(FrameworkMessages.exit())
						.description(FrameworkMessages.exitTip())
						.mnemonic(FrameworkMessages.exitMnemonic())
						.build();
	}

	private Controls logLevelControls() {
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

	private CommandControl selectLookAndFeelControl() {
		return Dialogs.select()
						.lookAndFeel()
						.owner(this)
						.createControl();
	}

	private CommandControl selectScalingControl() {
		return Control.builder()
						.command(this::selectScaling)
						.caption(resourceBundle.getString("scaling"))
						.build();
	}

	private ToggleControl alwaysOnTopControl() {
		return Control.builder()
						.toggle(alwaysOnTopState)
						.caption(resourceBundle.getString(ALWAYS_ON_TOP))
						.build();
	}

	private CommandControl aboutControl() {
		return Control.builder()
						.command(this::displayAbout)
						.caption(resourceBundle.getString(ABOUT) + "...")
						.build();
	}

	private CommandControl helpControl() {
		return Control.builder()
						.command(this::displayHelp)
						.caption(resourceBundle.getString(HELP) + "...")
						.build();
	}

	private Controls logControls() {
		ControlsBuilder builder = Controls.builder()
						.caption(resourceBundle.getString("log"))
						.mnemonic(resourceBundle.getString("log_mnemonic").charAt(0));
		if (!logLevelStates.isEmpty()) {
			builder.control(logLevelControls());
		}
		createOpenLogControls().ifPresent(builder::control);
		createTracingControls().ifPresent(builder::control);

		return builder.build();
	}

	private CommandControl keyboardShortcutsControl() {
		return Control.builder()
						.command(this::displayKeyboardShortcuts)
						.caption(resourceBundle.getString(SHORTCUTS) + "...")
						.build();
	}

	private void configureEntityPanel(EntityPanel entityPanel) {
		entityPanel.linkSiblings(entityPanels);
		entityPanel.display().requested().addConsumer(applicationLayout::display);
	}

	private EntityPanel entityPanel(EntityPanel.Builder panelBuilder) {
		if (CACHE_ENTITY_PANELS.getOrThrow() && cachedEntityPanels.containsKey(panelBuilder)) {
			return cachedEntityPanels.get(panelBuilder);
		}

		EntityPanel entityPanel = panelBuilder.build(model.connection());
		if (userPreferences) {
			entityPanel.restore(preferences().node(AUXILIARY).node(entityPanel.preferencesKey()));
		}
		entityPanel.initialize();
		if (CACHE_ENTITY_PANELS.getOrThrow()) {
			cachedEntityPanels.put(panelBuilder, entityPanel);
		}

		return entityPanel;
	}

	private static JPanel createEmptyBorderBasePanel(EntityPanel entityPanel) {
		int gap = Layouts.GAP.getOrThrow();
		return Components.borderLayoutPanel()
						.center(entityPanel)
						.border(createEmptyBorder(gap, gap, 0, gap))
						.build();
	}

	private void handleUnsavedModifications() {
		Map<EntityPanel, Collection<Attribute<?>>> modified = modified(entityPanels);
		if (modifiedWarning && !modified.isEmpty() && showConfirmDialog(this,
						createModifiedMessage(modified, model.entities()), FrameworkMessages.modifiedWarningTitle(),
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
			EntityPanel modifiedPanel = modified.keySet().stream()
							.filter(entityPanel -> entityPanel.containsEditPanel() && entityPanel.editPanel().active().is())
							.findFirst()
							.orElse(modified.keySet().iterator().next());
			modifiedPanel.display().request();
			Collection<Attribute<?>> attributes = modified.get(modifiedPanel);
			if (modifiedPanel.containsEditPanel() && !attributes.isEmpty()) {// modified can be predicate based
				SwingUtilities.invokeLater(() -> modifiedPanel.editPanel().requestModifiedFocus(attributes.iterator().next()));
			}

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

	private static Map<EntityPanel, Collection<Attribute<?>>> modified(Collection<EntityPanel> panels) {
		Map<EntityPanel, Collection<Attribute<?>>> modifiedPanels = new LinkedHashMap<>();
		for (EntityPanel panel : panels) {
			SwingEntityEditModel editModel = panel.model().editModel();
			if (editModel.editor().entity().modified().is()) {
				modifiedPanels.put(panel, editModel.editor().entity().modified().attributes().get());
			}
			modifiedPanels.putAll(modified(panel.detail().get()));
		}

		return modifiedPanels;
	}

	private static String createModifiedMessage(Map<EntityPanel, Collection<Attribute<?>>> modified, Entities entities) {
		return modified.entrySet().stream()
						.map(entry -> entry.getKey().caption() + ": " + entry.getValue().stream()
										.map(attribute -> entities.definition(attribute.entityType()).attributes().definition(attribute))
										.map(AttributeDefinition::caption)
										.collect(Collectors.joining(", ")))
						.collect(joining("\n", "", "\n\n")) + FrameworkMessages.modifiedWarning();
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
			levelStateMap.put(logLevel, State.builder()
							.value(Objects.equals(logLevel, currentLogLevel))
							.when(true, () -> loggerProxy.setLogLevel(loggerProxy.rootLogger(), logLevel))
							.group(logLevelStateGroup)
							.build());
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
		EntityConnection connection = model.connection();
		if (connection instanceof EntityConnectionTracer && SQL_TRACING.getOrThrow()) {
			EntityConnectionTracer tracer = (EntityConnectionTracer) connection;

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
			EntityConnection connection = model.connection();
			if (connection instanceof EntityConnectionTracer) {
				EntityConnectionTracer tracer = (EntityConnectionTracer) connection;
				if (tracer.tracing().is()) {
					sqlTraceViewer = createTraceViewer(tracer);
				}
				tracer.tracing().addConsumer(this::tracingChanged);
			}
		}
	}

	private void tracingChanged(boolean tracing) {
		EntityConnection connection = model.connection();
		if (connection instanceof EntityConnectionTracer) {
			if (tracing) {
				if (sqlTraceViewer == null) {
					sqlTraceViewer = createTraceViewer((EntityConnectionTracer) connection);
				}
			}
			else if (sqlTraceViewer != null) {
				sqlTraceViewer.clear();
			}
		}
	}

	private static LogViewer createTraceViewer(EntityConnectionTracer tracer) {
		LogViewer logViewer = LogViewer.logViewer();
		logViewer.setBorder(emptyBorder());
		tracer.trace().addConsumer(trace -> {
			StringBuilder logBuilder = new StringBuilder();
			trace.appendTo(logBuilder);
			logViewer.append(logBuilder.toString());
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

	private void onEntityPanelWindowClosed(EntityPanel entityPanel) {
		displayedEntityPanels.remove(entityPanel);
		entityPanel.store(preferences().node(AUXILIARY).node(entityPanel.preferencesKey()));
		entityPanel.setPreferredSize(entityPanel.getSize());
	}

	void setSaveDefaultUsername(boolean saveDefaultUsername) {
		this.saveDefaultUsername = saveDefaultUsername;
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
		 * Called when the given entity panel should be displayed,
		 * responsible for making sure it can be displayed.
		 * @param entityPanel the entity panel to display
		 * @see EntityPanel#display()
		 */
		void display(EntityPanel entityPanel);
	}
}
