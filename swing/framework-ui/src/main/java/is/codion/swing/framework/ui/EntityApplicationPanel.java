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
import is.codion.common.Memory;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventObserver;
import is.codion.common.logging.LoggerProxy;
import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.property.PropertyValue;
import is.codion.common.resource.MessageBundle;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.ValueObserver;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityPanel.WindowType;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
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
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.common.ui.component.Components.*;
import static java.awt.Frame.MAXIMIZED_BOTH;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.BorderFactory.createEmptyBorder;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A central application panel class.
 * @param <M> the application model type
 * @see #builder(Class, Class)
 */
public abstract class EntityApplicationPanel<M extends SwingEntityApplicationModel> extends JPanel {

	/**
	 * Specifies whether the client should save and apply user preferences<br>
	 * Value type: Boolean<br>
	 * Default value: true<br>
	 * @see #savePreferences()
	 */
	public static final PropertyValue<Boolean> USE_CLIENT_PREFERENCES =
					Configuration.booleanValue(EntityApplicationPanel.class.getName() + ".useClientPreferences", true);

	private static final String LOG_LEVEL = "log_level";
	private static final String LOG_LEVEL_DESC = "log_level_desc";
	private static final String HELP = "help";
	private static final String KEYBOARD_SHORTCUTS = "keyboard_shortcuts";
	private static final String ABOUT = "about";
	private static final String ALWAYS_ON_TOP = "always_on_top";
	private static final String APPLICATION_VERSION = "application_version";
	private static final String CODION_VERSION = "codion_version";
	private static final String MEMORY_USAGE = "memory_usage";

	private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

	/**
	 * Specifies the URL to the application help<br>
	 * Value type: String<br>
	 * Default value: https://codion.is/doc/{version}/help/client.html
	 */
	public static final PropertyValue<String> HELP_URL = Configuration.stringValue(EntityApplicationPanel.class.getName() + ".helpUrl",
					"https://codion.is/doc/" + Version.versionString() + "/help/client.html");

	/**
	 * Indicates whether the application should ask for confirmation when exiting<br>
	 * Value type: Boolean<br>
	 * Default value: false
	 */
	public static final PropertyValue<Boolean> CONFIRM_EXIT = Configuration.booleanValue(EntityApplicationPanel.class.getName() + ".confirmExit", false);

	/**
	 * Specifies whether a startup dialog should be shown<br>
	 * Value type: Boolean<br>
	 * Default value: true
	 */
	public static final PropertyValue<Boolean> SHOW_STARTUP_DIALOG = Configuration.booleanValue(EntityApplicationPanel.class.getName() + ".showStartupDialog", true);

	/**
	 * Specifies whether EntityPanels displayed via {@link EntityApplicationPanel#displayEntityPanelDialog(EntityPanel)}
	 * or {@link EntityApplicationPanel#displayEntityPanelFrame(EntityPanel)} should be cached,
	 * instead of being created each time the dialog/frame is shown.<br>
	 * Value type: Boolean<br>
	 * Default value: false
	 * @see EntityApplicationPanel#displayEntityPanelDialog(EntityPanel)
	 * @see EntityApplicationPanel#displayEntityPanelFrame(EntityPanel)
	 */
	public static final PropertyValue<Boolean> CACHE_ENTITY_PANELS = Configuration.booleanValue(EntityApplicationPanel.class.getName() + ".cacheEntityPanels", false);

	private static final int DEFAULT_LOGO_SIZE = 68;

	// Non-static so that Locale.setDefault(...) can be called in the main method of a subclass
	private final MessageBundle resourceBundle =
					messageBundle(EntityApplicationPanel.class, getBundle(EntityApplicationPanel.class.getName()));

	private final M applicationModel;
	private final Collection<EntityPanel.Builder> supportPanelBuilders = new ArrayList<>();
	private final List<EntityPanel> entityPanels = new ArrayList<>();
	private final ApplicationLayout applicationLayout;

	private final State alwaysOnTopState = State.builder()
					.consumer(alwaysOnTop -> parentWindow().ifPresent(parent -> parent.setAlwaysOnTop(alwaysOnTop)))
					.build();
	private final Event<?> onExitEvent = Event.event();
	private final Event<EntityApplicationPanel<?>> onInitialized = Event.event();
	private final boolean modifiedWarning = EntityEditPanel.Config.MODIFIED_WARNING.get();

	private final Map<EntityPanel.Builder, EntityPanel> cachedEntityPanels = new HashMap<>();

	private final Map<Object, State> logLevelStates = createLogLevelStateMap();

	private boolean saveDefaultUsername = true;
	private int fontSize = 100;
	private boolean initialized = false;

	public EntityApplicationPanel(M applicationModel) {
		this(applicationModel, TabbedApplicationLayout::new);
	}

	public EntityApplicationPanel(M applicationModel, Function<EntityApplicationPanel<?>, ApplicationLayout> applicationLayout) {
		this.applicationModel = requireNonNull(applicationModel);
		this.applicationLayout = requireNonNull(applicationLayout).apply(this);
		//initialize button captions, not in a static initializer since applications may set the locale in main()
		UiManagerDefaults.initialize();
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
		Dialogs.displayExceptionDialog(exception, Utilities.parentWindow(focusOwner));
	}

	/**
	 * @return the application model this application panel is based on
	 */
	public final M applicationModel() {
		return applicationModel;
	}

	/**
	 * @param <T> the layout type
	 * @return the application layout
	 */
	public final <T extends ApplicationLayout> T applicationLayout() {
		return (T) applicationLayout;
	}

	/**
	 * @param <T> the entity panel type
	 * @param entityType the entityType
	 * @return the first entity panel found based on the given entity type
	 * @throws IllegalArgumentException in case this application panel does not contain a panel for the given entity type
	 */
	public final <T extends EntityPanel> T entityPanel(EntityType entityType) {
		requireNonNull(entityType);
		return (T) entityPanels.stream()
						.filter(entityPanel -> entityPanel.model().entityType().equals(entityType))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("EntityPanel for entity: " + entityType + " not found in application panel: " + getClass()));
	}

	/**
	 * @return an unmodifiable view of the main application panels
	 */
	public final List<EntityPanel> entityPanels() {
		return Collections.unmodifiableList(entityPanels);
	}

	/**
	 * @return the parent window of this panel, if one exists, an empty Optional otherwise
	 */
	public final Optional<Window> parentWindow() {
		return Optional.ofNullable(Utilities.parentWindow(this));
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
		Dialogs.componentDialog(createApplicationTree())
						.owner(this)
						.title(resourceBundle.getString("view_application_tree"))
						.modal(false)
						.show();
	}

	/**
	 * Shows a dialog containing a dependency tree view of all defined entities
	 */
	public final void viewDependencyTree() {
		Dialogs.componentDialog(createDependencyTree())
						.owner(this)
						.title(FrameworkMessages.dependencies())
						.modal(false)
						.show();
	}

	/**
	 * @return an observer notified when this application panel is initialized
	 * @see #initialize()
	 */
	public final EventObserver<EntityApplicationPanel<?>> initializedEvent() {
		return onInitialized.observer();
	}

	/**
	 * Exits this application
	 * @throws CancelException if the exit is cancelled
	 * @see #exitEvent()
	 * @see EntityEditPanel.Config#MODIFIED_WARNING
	 * @see EntityApplicationPanel#CONFIRM_EXIT
	 */
	public final void exit() {
		if (cancelExit()) {
			throw new CancelException();
		}

		try {
			onExitEvent.run();
		}
		catch (CancelException e) {
			throw e;
		}
		catch (Exception e) {
			LOG.debug("Exception while exiting", e);
		}
		try {
			savePreferences();
			UserPreferences.flushUserPreferences();
		}
		catch (Exception e) {
			LOG.debug("Exception while saving preferences", e);
		}
		try {
			applicationModel.connectionProvider().close();
		}
		catch (Exception e) {
			LOG.debug("Exception while disconnecting from database", e);
		}
		parentWindow().ifPresent(Window::dispose);
		System.exit(0);
	}

	/**
	 * Displays the help.
	 * @throws Exception in case of an exception, for example a malformed URL
	 * @see #HELP_URL
	 */
	public void displayHelp() throws Exception {
		Desktop.getDesktop().browse(new URL(HELP_URL.get()).toURI());
	}

	/**
	 * Displays a keyboard shortcut overview panel.
	 */
	public final void displayKeyboardShortcuts() {
		KeyboardShortcutsPanel shortcutsPanel = new KeyboardShortcutsPanel();
		shortcutsPanel.setPreferredSize(new Dimension(shortcutsPanel.getPreferredSize().width, Windows.screenSizeRatio(0.5).height));
		Dialogs.componentDialog(shortcutsPanel)
						.owner(this)
						.title(resourceBundle.getString(KEYBOARD_SHORTCUTS))
						.modal(false)
						.show();
	}

	/**
	 * Shows an about dialog
	 * @see #createAboutPanel()
	 */
	public final void displayAbout() {
		Dialogs.componentDialog(createAboutPanel())
						.owner(this)
						.title(resourceBundle.getString(ABOUT))
						.resizable(false)
						.show();
	}

	/**
	 * Initializes this panel and marks is as initialized, subsequent calls have no effect.
	 */
	public final void initialize() {
		if (!initialized) {
			try {
				createEntityPanels().forEach(this::addEntityPanel);
				supportPanelBuilders.addAll(createSupportEntityPanelBuilders());
				setLayout(new BorderLayout());
				add(applicationLayout.layout(), BorderLayout.CENTER);
				bindEvents();
				onInitialized.accept(this);
			}
			finally {
				initialized = true;
			}
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
			if (definition.foreignKeys().get().isEmpty() || referencesOnlySelf(entities, definition.entityType())) {
				root.add(new EntityDependencyTreeNode(definition.entityType(), entities));
			}
		}

		return new DefaultTreeModel(root);
	}

	/**
	 * @param <M> the application model type
	 * @param <P> the application panel type
	 * @param applicationModelClass the application model class
	 * @param applicationPanelClass the application panel class
	 * @return a {@link Builder}
	 */
	public static <M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> Builder<M, P> builder(
					Class<M> applicationModelClass, Class<P> applicationPanelClass) {
		return new DefaultEntityApplicationPanelBuilder<>(applicationModelClass, applicationPanelClass);
	}

	/**
	 * @return the controls on which to base the main menu
	 * @see #createFileMenuControls()
	 * @see #createViewMenuControls()
	 * @see #createToolsMenuControls()
	 * @see #createHelpMenuControls()
	 */
	protected Controls createMainMenuControls() {
		Controls menuControls = Controls.controls();
		Controls fileMenuControls = createFileMenuControls();
		if (fileMenuControls != null && fileMenuControls.notEmpty()) {
			menuControls.add(fileMenuControls);
		}
		Controls viewMenuControls = createViewMenuControls();
		if (viewMenuControls != null && viewMenuControls.notEmpty()) {
			menuControls.add(viewMenuControls);
		}
		Controls toolsMenuControls = createToolsMenuControls();
		if (toolsMenuControls != null && toolsMenuControls.notEmpty()) {
			menuControls.add(toolsMenuControls);
		}
		Controls supportTableMenuControls = createSupportTableMenuControls();
		if (supportTableMenuControls != null && supportTableMenuControls.notEmpty()) {
			menuControls.add(supportTableMenuControls);
		}
		Controls helpMenuControls = createHelpMenuControls();
		if (helpMenuControls != null && helpMenuControls.notEmpty()) {
			menuControls.add(helpMenuControls);
		}

		return menuControls;
	}

	/**
	 * @return the Controls specifying the items in the 'File' menu
	 */
	protected Controls createFileMenuControls() {
		return Controls.builder()
						.name(FrameworkMessages.file())
						.mnemonic(FrameworkMessages.fileMnemonic())
						.control(createExitControl())
						.build();
	}

	/**
	 * @return the Controls specifying the items in the 'Tools' menu
	 */
	protected Controls createToolsMenuControls() {
		return Controls.builder()
						.name(resourceBundle.getString("tools"))
						.mnemonic(resourceBundle.getString("tools_mnemonic").charAt(0))
						.build();
	}

	/**
	 * @return the Controls specifying the items in the 'View' menu
	 */
	protected Controls createViewMenuControls() {
		return Controls.builder()
						.name(FrameworkMessages.view())
						.mnemonic(FrameworkMessages.viewMnemonic())
						.control(createSelectLookAndFeelControl())
						.control(createSelectFontSizeControl())
						.separator()
						.control(createAlwaysOnTopControl())
						.build();
	}

	/**
	 * @return the Controls specifying the items in the 'Help' menu
	 */
	protected Controls createHelpMenuControls() {
		Controls controls = Controls.builder()
						.name(resourceBundle.getString(HELP))
						.mnemonic(resourceBundle.getString("help_mnemonic").charAt(0))
						.control(createHelpControl())
						.control(createViewKeyboardShortcutsControl())
						.separator()
						.control(createAboutControl())
						.build();

		Controls logControls = createLogControls();
		if (logControls != null && !logControls.empty()) {
			controls.addSeparatorAt(2);
			controls.addAt(3, logControls);
		}

		return controls;
	}

	/**
	 * @return the Controls on which to base the Support Tables menu
	 */
	protected Controls createSupportTableMenuControls() {
		return Controls.builder()
						.name(FrameworkMessages.supportTables())
						.mnemonic(FrameworkMessages.supportTablesMnemonic())
						.controls(supportPanelBuilders.stream()
										.sorted(new SupportPanelBuilderComparator(applicationModel.entities()))
										.map(this::createSupportPanelControl)
										.toArray(Control[]::new))
						.build();
	}

	/**
	 * @return a Control for exiting the application
	 */
	protected final Control createExitControl() {
		return Control.builder(this::exit)
						.name(FrameworkMessages.exit())
						.description(FrameworkMessages.exitTip())
						.mnemonic(FrameworkMessages.exitMnemonic())
						.build();
	}

	/**
	 * @return a Control for setting the log level
	 */
	protected final Control createLogLevelControl() {
		Controls logLevelControls = Controls.builder()
						.name(resourceBundle.getString(LOG_LEVEL))
						.description(resourceBundle.getString(LOG_LEVEL_DESC))
						.build();
		logLevelStates.forEach((logLevel, state) -> logLevelControls.add(ToggleControl.builder(state)
						.name(logLevel.toString())
						.build()));

		return logLevelControls;
	}

	/**
	 * @return a Control for viewing the application structure tree
	 */
	protected final Control createViewApplicationTreeControl() {
		return Control.builder(this::viewApplicationTree)
						.name(resourceBundle.getString("view_application_tree"))
						.build();
	}

	/**
	 * @return a Control for viewing the application dependency tree
	 */
	protected final Control createViewDependencyTree() {
		return Control.builder(this::viewDependencyTree)
						.name(FrameworkMessages.dependencies())
						.build();
	}

	/**
	 * Allows the user the select between the available Look and Feels, saves the selection as a user preference.
	 * @return a Control for selecting the application look and feel
	 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelProvider)
	 * @see LookAndFeelProvider#findLookAndFeelProvider(String)
	 * @see Dialogs#lookAndFeelSelectionDialog()
	 * @see LookAndFeelComboBox#ENABLE_ON_SELECTION
	 */
	protected final Control createSelectLookAndFeelControl() {
		return Dialogs.lookAndFeelSelectionDialog()
						.owner(this)
						.createControl(lookAndFeelProvider -> {});
	}

	/**
	 * @return a Control for selecting the font size
	 */
	protected final Control createSelectFontSizeControl() {
		return Dialogs.fontSizeSelectionDialog()
						.owner(this)
						.initialSelection(fontSize)
						.createControl(selectedFontSize -> {
							fontSize = selectedFontSize;
							showMessageDialog(this, resourceBundle.getString("font_size_selected_message"));
						});
	}

	/**
	 * @return a Control controlling the always on top status
	 */
	protected final ToggleControl createAlwaysOnTopControl() {
		return ToggleControl.builder(alwaysOnTopState)
						.name(resourceBundle.getString(ALWAYS_ON_TOP))
						.build();
	}

	/**
	 * @return a Control for viewing information about the application
	 */
	protected final Control createAboutControl() {
		return Control.builder(this::displayAbout)
						.name(resourceBundle.getString(ABOUT) + "...")
						.build();
	}

	/**
	 * @return a Control for displaying the help
	 */
	protected final Control createHelpControl() {
		return Control.builder(this::displayHelp)
						.name(resourceBundle.getString(HELP) + "...")
						.build();
	}

	/**
	 * @return a Control for displaying the keyboard shortcuts overview
	 */
	protected final Control createViewKeyboardShortcutsControl() {
		return Control.builder(this::displayKeyboardShortcuts)
						.name(resourceBundle.getString(KEYBOARD_SHORTCUTS) + "...")
						.build();
	}

	/**
	 * @return the panel shown when Help -&#62; About is selected
	 */
	protected JPanel createAboutPanel() {
		PanelBuilder versionMemoryPanel = gridLayoutPanel(0, 2)
						.border(emptyBorder());
		applicationModel().version().ifPresent(version -> versionMemoryPanel
						.add(new JLabel(resourceBundle.getString(APPLICATION_VERSION) + ":"))
						.add(new JLabel(version.toString())));
		versionMemoryPanel
						.add(new JLabel(resourceBundle.getString(CODION_VERSION) + ":"))
						.add(new JLabel(Version.versionAndMetadataString()))
						.add(new JLabel(resourceBundle.getString(MEMORY_USAGE) + ":"))
						.add(new JLabel(Memory.memoryUsage()));

		return borderLayoutPanel()
						.border(emptyBorder())
						.westComponent(new JLabel(FrameworkIcons.instance().logo(DEFAULT_LOGO_SIZE)))
						.centerComponent(versionMemoryPanel.build())
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
	 */
	protected final void displayEntityPanel(EntityPanel.Builder panelBuilder) {
		displayEntityPanel(entityPanel(panelBuilder));
	}

	/**
	 * Displays the given panel in a frame or dialog,
	 * depending on {@link EntityPanel.Config#WINDOW_TYPE}.
	 * @param entityPanel the entity panel
	 */
	protected final void displayEntityPanel(EntityPanel entityPanel) {
		if (EntityPanel.Config.WINDOW_TYPE.isEqualTo(WindowType.FRAME)) {
			displayEntityPanelFrame(entityPanel);
		}
		else {
			displayEntityPanelDialog(entityPanel);
		}
	}

	/**
	 * Shows a frame containing the given entity panel
	 * @param entityPanel the entity panel
	 */
	protected final void displayEntityPanelFrame(EntityPanel entityPanel) {
		requireNonNull(entityPanel, "entityPanel");
		if (entityPanel.isShowing()) {
			Window parentWindow = Utilities.parentWindow(entityPanel);
			if (parentWindow != null) {
				parentWindow.toFront();
			}
		}
		else {
			Windows.frame(createEmptyBorderBasePanel(entityPanel))
							.locationRelativeTo(this)
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
							.onClosed(windowEvent -> entityPanel.savePreferences())
							.show();
		}
	}

	/**
	 * Shows a non-modal dialog containing the given entity panel
	 * @param entityPanel the entity panel
	 */
	protected final void displayEntityPanelDialog(EntityPanel entityPanel) {
		displayEntityPanelDialog(entityPanel, false);
	}

	/**
	 * Shows a dialog containing the given entity panel
	 * @param entityPanel the entity panel
	 * @param modalDialog if true the dialog is made modal
	 */
	protected final void displayEntityPanelDialog(EntityPanel entityPanel, boolean modalDialog) {
		requireNonNull(entityPanel, "entityPanel");
		if (entityPanel.isShowing()) {
			Window parentWindow = Utilities.parentWindow(entityPanel);
			if (parentWindow != null) {
				parentWindow.toFront();
			}
		}
		else {
			Dialogs.componentDialog(createEmptyBorderBasePanel(entityPanel))
							.owner(parentWindow().orElse(null))
							.title(entityPanel.caption())
							.icon(entityPanel.icon().orElse(null))
							.onClosed(e -> entityPanel.savePreferences())
							.modal(modalDialog)
							.resizable(true)
							.show();
		}
	}

	/**
	 * Creates the {@link EntityPanel}s to include in this application panel.
	 * Returns an empty list in case this panel contains no entity panels or has a custom UI.
	 * @return a List containing the {@link EntityPanel}s to include in this application panel or an empty list in case of no entity panels.
	 */
	protected abstract List<EntityPanel> createEntityPanels();

	/**
	 * Returns a Collection of {@link EntityPanel.Builder} instances to use to populate the Support Table menu.
	 * Returns an empty Collection in case of no support table panels.
	 * @return a Collection of {@link EntityPanel.Builder} instances to use to populate the Support Table menu.
	 */
	protected Collection<EntityPanel.Builder> createSupportEntityPanelBuilders() {
		return Collections.emptyList();
	}

	/**
	 * To cancel the exit add a listener throwing a {@link CancelException}.
	 * @return an observer notified when the application is about to exit.
	 */
	protected final EventObserver<?> exitEvent() {
		return onExitEvent.observer();
	}

	/**
	 * Creates the JMenuBar to use on the application Frame
	 * @return by default a JMenuBar based on the main menu controls
	 * @see #createMainMenuControls()
	 */
	protected JMenuBar createMenuBar() {
		Controls mainMenuControls = createMainMenuControls();
		if (mainMenuControls == null || mainMenuControls.empty()) {
			return null;
		}

		return menu(mainMenuControls).createMenuBar();
	}

	/**
	 * Called during the exit() method, override to save user preferences on program exit,
	 * remember to call super.savePreferences() when overriding
	 * @see #USE_CLIENT_PREFERENCES
	 */
	protected void savePreferences() {
		if (USE_CLIENT_PREFERENCES.get()) {
			entityPanels().forEach(EntityPanel::savePreferences);
			try {
				createPreferences().save(getClass());
			}
			catch (Exception e) {
				LOG.error("Error while saving application preferences", e);
			}
		}
	}

	private ApplicationPreferences createPreferences() {
		JFrame parentFrame = Utilities.parentFrame(this);

		return new ApplicationPreferences(
						saveDefaultUsername ? applicationModel.connectionProvider().user().username() : null,
						getLookAndFeel().getClass().getName(), fontSize,
						parentFrame == null ? null : parentFrame.getSize(),
						parentFrame != null && (parentFrame.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH);
	}

	private Control createSupportPanelControl(EntityPanel.Builder panelBuilder) {
		return Control.builder(() -> displayEntityPanel(panelBuilder))
						.name(panelBuilder.caption().orElse(applicationModel.entities().definition(panelBuilder.entityType()).caption()))
						.smallIcon(panelBuilder.icon().orElse(null))
						.build();
	}

	private void addEntityPanel(EntityPanel entityPanel) {
		EntityPanel.addEntityPanelAndLinkSiblings(entityPanel, entityPanels);
		entityPanel.activateEvent().addConsumer(applicationLayout::activated);
		if (entityPanel.containsEditPanel()) {
			entityPanel.editPanel().active().addConsumer(new SelectActivatedPanel(entityPanel));
		}
	}

	private EntityPanel entityPanel(EntityPanel.Builder panelBuilder) {
		if (CACHE_ENTITY_PANELS.get() && cachedEntityPanels.containsKey(panelBuilder)) {
			return cachedEntityPanels.get(panelBuilder);
		}

		EntityPanel entityPanel = panelBuilder.build(applicationModel.connectionProvider());
		entityPanel.initialize();
		if (CACHE_ENTITY_PANELS.get()) {
			cachedEntityPanels.put(panelBuilder, entityPanel);
		}

		return entityPanel;
	}

	private static JPanel createEmptyBorderBasePanel(EntityPanel entityPanel) {
		int gap = Layouts.GAP.get();
		return Components.borderLayoutPanel()
						.centerComponent(entityPanel)
						.border(createEmptyBorder(gap, gap, 0, gap))
						.build();
	}

	private JScrollPane createApplicationTree() {
		return createTree(createApplicationTree(entityPanels));
	}

	private JScrollPane createDependencyTree() {
		return createTree(createDependencyTreeModel(applicationModel.entities()));
	}

	private boolean cancelExit() {
		Collection<EntityPanel> modified = modified(entityPanels);
		if (modifiedWarning && !modified.isEmpty()) {
			return showConfirmDialog(this,
							createModifiedMessage(modified), FrameworkMessages.modifiedWarningTitle(),
							JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION;
		}

		return CONFIRM_EXIT.get() && showConfirmDialog(this,
						FrameworkMessages.confirmExit(), FrameworkMessages.confirmExitTitle(),
						JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION;
	}

	private static Collection<EntityPanel> modified(Collection<EntityPanel> panels) {
		Collection<EntityPanel> modifiedPanels = new ArrayList<>();
		for (EntityPanel panel : panels) {
			EntityEditModel editModel = panel.editModel();
			if (editModel.editing().get()) {
				modifiedPanels.add(panel);
			}
			modifiedPanels.addAll(modified(panel.detailPanels()));
		}

		return modifiedPanels;
	}

	private static String createModifiedMessage(Collection<EntityPanel> modified) {
		return modified.stream()
						.map(EntityPanel::caption)
						.collect(Collectors.joining(", ")) + "\n" + FrameworkMessages.modifiedWarning();
	}

	private static Map<Object, State> createLogLevelStateMap() {
		LoggerProxy loggerProxy = LoggerProxy.instance();
		if (loggerProxy == LoggerProxy.NULL_PROXY) {
			return Collections.emptyMap();
		}
		Object currentLogLevel = loggerProxy.getLogLevel();
		Map<Object, State> levelStateMap = new LinkedHashMap<>();
		State.Group logLevelStateGroup = State.group();
		for (Object logLevel : loggerProxy.levels()) {
			State logLevelState = State.state(Objects.equals(logLevel, currentLogLevel));
			logLevelStateGroup.add(logLevelState);
			logLevelState.addConsumer(enabled -> {
				if (enabled) {
					loggerProxy.setLogLevel(logLevel);
				}
			});
			levelStateMap.put(logLevel, logLevelState);
		}

		return Collections.unmodifiableMap(levelStateMap);
	}

	private Controls createLogControls() {
		Controls.Builder builder = Controls.builder()
						.name(resourceBundle.getString("log"))
						.mnemonic(resourceBundle.getString("log_mnemonic").charAt(0));
		if (!logLevelStates.isEmpty()) {
			builder.control(createLogLevelControl());
		}
		Control logFileControl = createLogFileControl();
		if (logFileControl != null) {
			builder.control(logFileControl);
		}

		return builder.build();
	}

	private Control createLogFileControl() {
		Collection<String> files = LoggerProxy.instance().files();
		if (files.isEmpty()) {
			return null;
		}
		String firstLogFile = files.iterator().next();

		return Control.builder(() -> Desktop.getDesktop().open(new File(firstLogFile)))
						.name(resourceBundle.getString("open_log_file"))
						.description(resourceBundle.getString("open_log_file") + " (" + firstLogFile + ")")
						.build();
	}

	private static JScrollPane createTree(TreeModel treeModel) {
		JTree tree = new JTree(treeModel);
		tree.setShowsRootHandles(true);
		tree.setToggleClickCount(1);
		tree.setRootVisible(false);
		Utilities.expandAll(tree, new TreePath(tree.getModel().getRoot()));

		return new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	private static DefaultTreeModel createApplicationTree(Collection<? extends EntityPanel> entityPanels) {
		DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
		addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

		return applicationTreeModel;
	}

	private static void addModelsToTree(DefaultMutableTreeNode root, Collection<? extends EntityPanel> panels) {
		for (EntityPanel entityPanel : panels) {
			DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel.caption());
			root.add(node);
			if (!entityPanel.detailPanels().isEmpty()) {
				addModelsToTree(node, entityPanel.detailPanels());
			}
		}
	}

	private static boolean referencesOnlySelf(Entities entities, EntityType entityType) {
		return entities.definition(entityType).foreignKeys().get().stream()
						.allMatch(foreignKey -> foreignKey.referencedType().equals(entityType));
	}

	void setSaveDefaultUsername(boolean saveDefaultUsername) {
		this.saveDefaultUsername = saveDefaultUsername;
	}

	private final class SelectActivatedPanel implements Consumer<Boolean> {

		private final EntityPanel entityPanel;

		private SelectActivatedPanel(EntityPanel entityPanel) {
			this.entityPanel = entityPanel;
		}

		@Override
		public void accept(Boolean active) {
			if (active) {
				applicationLayout.activated(entityPanel);
			}
		}
	}

	private static final class SupportPanelBuilderComparator implements Comparator<EntityPanel.Builder> {

		private final Entities entities;
		private final Comparator<String> comparator = Text.collator();

		private SupportPanelBuilderComparator(Entities entities) {
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
			super(requireNonNull(entityType, "entityType"));
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
						childrenList.add(new EntityDependencyTreeNode(definition.entityType(), entities));
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
		 * Lays out the main component for a given application panel
		 * @return the main application panel
		 * @throws IllegalStateException in case the panel has already been laid out
		 */
		JComponent layout();

		/**
		 * Called when the given entity panel is activated.
		 * @param entityPanel the entity panel to select
		 * @see EntityPanel#activate()
		 * @see EntityPanel#activateEvent()
		 */
		default void activated(EntityPanel entityPanel) {}
	}

	/**
	 * Builds a {@link EntityApplicationPanel} and starts the application.
	 * @param <M> the application model type
	 * @param <P> the application panel type
	 * @see EntityApplicationPanel#builder(Class, Class)
	 * @see #start()
	 */
	public interface Builder<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> {

		/**
		 * @param domainType the domain type
		 * @return this Builder instance
		 */
		Builder<M, P> domainType(DomainType domainType);

		/**
		 * @param applicationName the application name
		 * @return this Builder instance
		 */
		Builder<M, P> applicationName(String applicationName);

		/**
		 * @param applicationIcon the application icon
		 * @return this Builder instance
		 */
		Builder<M, P> applicationIcon(ImageIcon applicationIcon);

		/**
		 * @param applicationVersion the application version
		 * @return this Builder instance
		 */
		Builder<M, P> applicationVersion(Version applicationVersion);

		/**
		 * Sets the default look and feel classname, used in case no look and feel settings are found in user preferences.
		 * Note that for an external Look and Feels to be enabled, it must be registered via
		 * {@link LookAndFeelProvider#addLookAndFeel(LookAndFeelProvider)}
		 * before starting the application.
		 * @param defaultLookAndFeelClassName the default look and feel classname
		 * @return this Builder instance
		 */
		Builder<M, P> defaultLookAndFeelClassName(String defaultLookAndFeelClassName);

		/**
		 * Sets the look and feel classname, overrides any look and feel settings found in user preferences.
		 * Note that for an external Look and Feels to be enabled, it must be registered via
		 * {@link LookAndFeelProvider#addLookAndFeel(LookAndFeelProvider)}
		 * before starting the application.
		 * @param lookAndFeelClassName the look and feel classname
		 * @return this Builder instance
		 */
		Builder<M, P> lookAndFeelClassName(String lookAndFeelClassName);

		/**
		 * @param connectionProviderFactory the connection provider factory
		 * @return this Builder instance
		 */
		Builder<M, P> connectionProviderFactory(ConnectionProviderFactory connectionProviderFactory);

		/**
		 * @param applicationModelFactory the application model factory
		 * @return this Builder instance
		 */
		Builder<M, P> applicationModelFactory(Function<EntityConnectionProvider, M> applicationModelFactory);

		/**
		 * @param applicationPanelFactory the application panel factory
		 * @return this Builder instance
		 */
		Builder<M, P> applicationPanelFactory(Function<M, P> applicationPanelFactory);

		/**
		 * @param loginProvider provides a way for a user to login
		 * @return this Builder instance
		 */
		Builder<M, P> loginProvider(LoginProvider loginProvider);

		/**
		 * @param defaultLoginUser the default user credentials to display in the login dialog
		 * @return this Builder instance
		 */
		Builder<M, P> defaultLoginUser(User defaultLoginUser);

		/**
		 * @param automaticLoginUser if specified the application is started automatically with the given user,
		 * instead of displaying a login dialog
		 * @return this Builder instance
		 */
		Builder<M, P> automaticLoginUser(User automaticLoginUser);

		/**
		 * @param saveDefaultUsername true if the username should be saved in user preferences after a successful login
		 * @return this Builder instance
		 */
		Builder<M, P> saveDefaultUsername(boolean saveDefaultUsername);

		/**
		 * Note that this does not apply when a custom {@link LoginProvider} has been specified.
		 * @param loginPanelSouthComponentSupplier supplies the component to add to the
		 * {@link BorderLayout#SOUTH} position of the default login panel
		 * @return this Builder instance
		 */
		Builder<M, P> loginPanelSouthComponent(Supplier<JComponent> loginPanelSouthComponentSupplier);

		/**
		 * Runs before the application is started, but after Look and Feel initialization.
		 * Throw {@link CancelException} in order to cancel the application startup.
		 * @param beforeApplicationStarted run before the application is started
		 * @return this Builder instance
		 */
		Builder<M, P> beforeApplicationStarted(Runnable beforeApplicationStarted);

		/**
		 * @param onApplicationStarted called after a successful application start
		 * @return this Builder instance
		 */
		Builder<M, P> onApplicationStarted(Consumer<P> onApplicationStarted);

		/**
		 * @param frameSupplier the frame supplier
		 * @return this Builder instance
		 */
		Builder<M, P> frameSupplier(Supplier<JFrame> frameSupplier);

		/**
		 * @param frameTitle the frame title
		 * @return this Builder instance
		 */
		Builder<M, P> frameTitle(String frameTitle);

		/**
		 * For a dynamic frame title.
		 * @param frameTitle the value observer controlling the frame title
		 * @return this Builder instance
		 */
		Builder<M, P> frameTitle(ValueObserver<String> frameTitle);

		/**
		 * @param includeMainMenu if true then a main menu is included
		 * @return this Builder instance
		 */
		Builder<M, P> includeMainMenu(boolean includeMainMenu);

		/**
		 * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
		 * @return this Builder instance
		 */
		Builder<M, P> maximizeFrame(boolean maximizeFrame);

		/**
		 * @param displayFrame specifies whether the frame should be displayed or left invisible
		 * @return this Builder instance
		 */
		Builder<M, P> displayFrame(boolean displayFrame);

		/**
		 * Specifies whether or not to set the default uncaught exception handler when starting the application, true by default.
		 * @param setUncaughtExceptionHandler if true the default uncaught exception handler is set on application start
		 * @return this Builder instance
		 * @see Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
		 */
		Builder<M, P> setUncaughtExceptionHandler(boolean setUncaughtExceptionHandler);

		/**
		 * @param displayStartupDialog if true then a progress dialog is displayed while the application is being initialized
		 * @return this Builder instance
		 */
		Builder<M, P> displayStartupDialog(boolean displayStartupDialog);

		/**
		 * @param frameSize the frame size when not maximized
		 * @return this Builder instance
		 */
		Builder<M, P> frameSize(Dimension frameSize);

		/**
		 * @param defaultFrameSize the default frame size when no previous size is available in user preferences
		 * @return this Builder instance
		 */
		Builder<M, P> defaultFrameSize(Dimension defaultFrameSize);

		/**
		 * If this is set to false, the {@link #connectionProviderFactory(ConnectionProviderFactory)}
		 * {@link User} argument will be null.
		 * @param loginRequired true if a user login is required for this application, false if the user is supplied differently
		 * @return this Builder instance
		 */
		Builder<M, P> loginRequired(boolean loginRequired);

		/**
		 * Starts the application on the Event Dispatch Thread.
		 */
		void start();

		/**
		 * Starts the application.
		 * @param onEventDispatchThread if true then startup is performed on the EDT
		 */
		void start(boolean onEventDispatchThread);

		/**
		 * Provides a way for a user to login.
		 */
		interface LoginProvider {

			/**
			 * Performs the login and returns the User, may not return null.
			 * @return the user, not null
			 * @throws RuntimeException in case the login failed
			 * @throws CancelException in case the login is cancelled
			 */
			User login();
		}

		/**
		 * A factory for a {@link EntityConnectionProvider} instance.
		 */
		interface ConnectionProviderFactory {

			/**
			 * Creates a new {@link EntityConnectionProvider} instance.
			 * @param user the user, may be null in case login is not required {@link Builder#loginRequired(boolean)}.
			 * @param domainType the domain type
			 * @param clientTypeId the client type id
			 * @param clientVersion the client version
			 * @return a new {@link EntityConnectionProvider} instance.
			 */
			default EntityConnectionProvider createConnectionProvider(User user, DomainType domainType, String clientTypeId, Version clientVersion) {
				return EntityConnectionProvider.builder()
								.user(user)
								.domainType(domainType)
								.clientTypeId(clientTypeId)
								.clientVersion(clientVersion)
								.build();
			}
		}
	}
}
