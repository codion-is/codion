/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.Memory;
import is.codion.common.Text;
import is.codion.common.credentials.CredentialsException;
import is.codion.common.credentials.CredentialsProvider;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.logging.LoggerProxy;
import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.panel.HierarchyPanel;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.laf.LookAndFeelComboBox;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
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
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
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
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.model.UserPreferences.getUserPreference;
import static is.codion.swing.common.ui.component.Components.menu;
import static is.codion.swing.common.ui.component.Components.panel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A central application panel class.
 * @param <M> the application model type
 * @see #builder(Class, Class)
 */
public abstract class EntityApplicationPanel<M extends SwingEntityApplicationModel>
        extends JPanel implements HierarchyPanel {

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

  static final String DEFAULT_USERNAME_PROPERTY = "is.codion.swing.framework.ui.defaultUsername";
  static final String LOOK_AND_FEEL_PROPERTY = "is.codion.swing.framework.ui.LookAndFeel";
  static final String FONT_SIZE_PROPERTY = "is.codion.swing.framework.ui.FontSize";

  /**
   * Specifies the URL to the application help<br>
   * Value type: String<br>
   * Default value: https://codion.is/doc/{version}/{jdk}/help/client.html
   */
  public static final PropertyValue<String> HELP_URL = Configuration.stringValue("codion.swing.helpUrl",
          "https://codion.is/doc/" + Version.versionString() + "/jdk8/help/client.html");

  /**
   * Indicates whether the application should ask for confirmation when exiting<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> CONFIRM_EXIT = Configuration.booleanValue("codion.swing.confirmExit", false);

  /**
   * Specifies whether a startup dialog should be shown<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> SHOW_STARTUP_DIALOG = Configuration.booleanValue("codion.swing.showStartupDialog", true);

  /**
   * Specifies if EntityPanels opened via the {@link EntityApplicationPanel#displayEntityPanel(EntityPanel.Builder)} method
   * should be displayed in a frame instead of the default dialog<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see EntityApplicationPanel#displayEntityPanel(EntityPanel.Builder)
   */
  public static final PropertyValue<Boolean> DISPLAY_ENTITY_PANELS_IN_FRAME = Configuration.booleanValue("codion.swing.displayEntityPanelsInFrame", false);

  /**
   * Specifies if EntityPanels opened via the {@code EntityApplicationPanel.displayEntityPanel} method
   * should be persisted, or kept in memory, when the dialog/frame is closed, instead of being created each time.<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see EntityApplicationPanel#displayEntityPanelDialog(EntityPanel.Builder)
   */
  public static final PropertyValue<Boolean> PERSIST_ENTITY_PANELS = Configuration.booleanValue("codion.swing.persistEntityPanels", false);

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (SwingConstants.TOP, SwingConstants.BOTTOM, SwingConstants.LEFT, SwingConstants.RIGHT)<br>
   * Default value: SwingConstants.TOP
   */
  public static final PropertyValue<Integer> TAB_PLACEMENT = Configuration.integerValue("codion.swing.tabPlacement", SwingConstants.TOP);

  private static final int DEFAULT_LOGO_SIZE = 68;

  /** Non-static so that Locale.setDefault(...) can be called in the main method of a subclass */
  private final ResourceBundle resourceBundle = ResourceBundle.getBundle(EntityApplicationPanel.class.getName());

  private final String applicationDefaultUsernameProperty;
  private final String applicationLookAndFeelProperty;
  private final String applicationFontSizeProperty;

  private final M applicationModel;
  private final Collection<EntityPanel.Builder> supportPanelBuilders = new ArrayList<>();
  private final List<EntityPanel> entityPanels = new ArrayList<>();

  private JTabbedPane applicationTabPane;

  private final State alwaysOnTopState = State.state();
  private final Event<?> onExitEvent = Event.event();

  private final Map<EntityPanel.Builder, EntityPanel> persistentEntityPanels = new HashMap<>();

  private final Map<Object, State> logLevelStates = createLogLevelStateMap();

  private boolean panelInitialized = false;

  public EntityApplicationPanel(M applicationModel) {
    this.applicationModel = requireNonNull(applicationModel);
    this.applicationDefaultUsernameProperty = DEFAULT_USERNAME_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationLookAndFeelProperty = LOOK_AND_FEEL_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationFontSizeProperty = FONT_SIZE_PROPERTY + "#" + getClass().getSimpleName();
    //initialize button captions, not in a static initializer since applications may set the locale in main()
    UiManagerDefaults.initialize();
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to display
   */
  public final void displayException(Throwable exception) {
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
   * @param entityType the entityType
   * @return the first entity panel found based on the given entity type, null if none is found
   */
  public final EntityPanel entityPanel(EntityType entityType) {
    return entityPanels.stream()
            .filter(entityPanel -> entityPanel.model().entityType().equals(entityType))
            .findFirst()
            .orElse(null);
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
  public final State alwaysOnTopState() {
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

  @Override
  public final Optional<HierarchyPanel> parentPanel() {
    return Optional.empty();
  }

  @Override
  public final Optional<HierarchyPanel> selectedChildPanel() {
    if (applicationTabPane != null && applicationTabPane.getTabCount() > 0) {//initializeUI() may have been overridden
      return Optional.of((HierarchyPanel) applicationTabPane.getSelectedComponent());
    }

    return entityPanels.isEmpty() ? Optional.empty() : Optional.of(entityPanels.get(0));
  }

  @Override
  public final void selectChildPanel(HierarchyPanel childPanel) {
    if (applicationTabPane != null && applicationTabPane.indexOfComponent((JComponent) childPanel) != -1) {//initializeUI() may have been overridden
      applicationTabPane.setSelectedComponent((JComponent) childPanel);
    }
  }

  @Override
  public final Optional<HierarchyPanel> previousSiblingPanel() {
    return Optional.empty();
  }

  @Override
  public final Optional<HierarchyPanel> nextSiblingPanel() {
    return Optional.empty();
  }

  @Override
  public final List<HierarchyPanel> childPanels() {
    return Collections.unmodifiableList(entityPanels);
  }

  @Override
  public final void activatePanel() {}

  /**
   * Exits this application
   * @throws CancelException if the exit is cancelled
   * @see #addOnExitListener(EventListener)
   * @see EntityApplicationPanel#CONFIRM_EXIT
   * @see EntityApplicationModel#isWarnAboutUnsavedData()
   */
  public final void exit() {
    if (cancelExit()) {
      throw new CancelException();
    }

    try {
      onExitEvent.onEvent();
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
  public final void initializePanel() {
    if (!panelInitialized) {
      try {
        this.entityPanels.addAll(createEntityPanels());
        this.supportPanelBuilders.addAll(createSupportEntityPanelBuilders());
        initializeUI();
        bindEventsInternal();
        bindEvents();
      }
      finally {
        panelInitialized = true;
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
      if (definition.foreignKeys().isEmpty() || referencesOnlySelf(entities, definition.type())) {
        root.add(new EntityDependencyTreeNode(definition.type(), entities));
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
   * Returns the JTabbedPane used by the default UI, an empty Optional in case the default UI
   * initialization has been overridden. Returns an empty Optional until the panel has been intialized via {@link #initializePanel()}.
   * @return the default application tab pane or an empty Optional if none is available
   */
  protected final Optional<JTabbedPane> applicationTabPane() {
    return Optional.ofNullable(applicationTabPane);
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
    if (fileMenuControls != null && !fileMenuControls.isEmpty()) {
      menuControls.add(fileMenuControls);
    }
    Controls viewMenuControls = createViewMenuControls();
    if (viewMenuControls != null && !viewMenuControls.isEmpty()) {
      menuControls.add(viewMenuControls);
    }
    Controls toolsMenuControls = createToolsMenuControls();
    if (toolsMenuControls != null && !toolsMenuControls.isEmpty()) {
      menuControls.add(toolsMenuControls);
    }
    Controls supportTableMenuControls = createSupportTableMenuControls();
    if (supportTableMenuControls != null && !supportTableMenuControls.isEmpty()) {
      menuControls.add(supportTableMenuControls);
    }
    Controls helpMenuControls = createHelpMenuControls();
    if (helpMenuControls != null && !helpMenuControls.isEmpty()) {
      menuControls.add(helpMenuControls);
    }

    return menuControls;
  }

  /**
   * @return the Controls specifying the items in the 'File' menu
   */
  protected Controls createFileMenuControls() {
    return Controls.builder()
            .caption(FrameworkMessages.file())
            .mnemonic(FrameworkMessages.fileMnemonic())
            .control(createExitControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Tools' menu
   */
  protected Controls createToolsMenuControls() {
    Controls.Builder toolsControlsBuilder = Controls.builder()
            .caption(resourceBundle.getString("tools"))
            .mnemonic(resourceBundle.getString("tools_mnemonic").charAt(0));
    if (!logLevelStates.isEmpty()) {
      toolsControlsBuilder.control(createLogLevelControl());
    }

    return toolsControlsBuilder.build();
  }

  /**
   * @return the Controls specifying the items in the 'View' menu
   */
  protected Controls createViewMenuControls() {
    return Controls.builder()
            .caption(FrameworkMessages.view())
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
    return Controls.builder()
            .caption(resourceBundle.getString(HELP))
            .mnemonic(resourceBundle.getString("help_mnemonic").charAt(0))
            .control(createHelpControl())
            .control(createViewKeyboardShortcutsControl())
            .separator()
            .control(createAboutControl())
            .build();
  }

  /**
   * @return the Controls on which to base the Support Tables menu
   */
  protected Controls createSupportTableMenuControls() {
    return Controls.builder()
            .caption(FrameworkMessages.supportTables())
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
            .caption(FrameworkMessages.exit())
            .description(FrameworkMessages.exitTip())
            .mnemonic(FrameworkMessages.exitMnemonic())
            .build();
  }

  /**
   * @return a Control for setting the log level
   */
  protected final Control createLogLevelControl() {
    Controls logLevelControls = Controls.builder()
            .caption(resourceBundle.getString(LOG_LEVEL))
            .description(resourceBundle.getString(LOG_LEVEL_DESC))
            .build();
    logLevelStates.forEach((logLevel, state) -> logLevelControls.add(ToggleControl.builder(state)
            .caption(logLevel.toString())
            .build()));

    return logLevelControls;
  }

  /**
   * @return a Control for refreshing the application model
   */
  protected final Control createRefreshAllControl() {
    return Control.builder(applicationModel::refresh)
            .caption(FrameworkMessages.refreshAll())
            .build();
  }

  /**
   * @return a Control for viewing the application structure tree
   */
  protected final Control createViewApplicationTreeControl() {
    return Control.builder(this::viewApplicationTree)
            .caption(resourceBundle.getString("view_application_tree"))
            .build();
  }

  /**
   * @return a Control for viewing the application dependency tree
   */
  protected final Control createViewDependencyTree() {
    return Control.builder(this::viewDependencyTree)
            .caption(FrameworkMessages.dependencies())
            .build();
  }

  /**
   * Allows the user the select between the available Look and Feels, saves the selection as a user preference.
   * @return a Control for selecting the application look and feel
   * @see LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)
   * @see LookAndFeelProvider#findLookAndFeelProvider(String)
   * @see Dialogs#lookAndFeelSelectionDialog()
   * @see LookAndFeelComboBox#CHANGE_ON_SELECTION
   */
  protected final Control createSelectLookAndFeelControl() {
    return Dialogs.lookAndFeelSelectionDialog()
            .owner(this)
            .userPreferencePropertyName(applicationLookAndFeelProperty)
            .createControl();
  }

  /**
   * @return a Control for selecting the font size
   */
  protected final Control createSelectFontSizeControl() {
    return Dialogs.fontSizeSelectionDialog(applicationFontSizeProperty)
            .owner(this)
            .createControl();
  }

  /**
   * @return a Control controlling the always on top status
   */
  protected final ToggleControl createAlwaysOnTopControl() {
    return ToggleControl.builder(alwaysOnTopState)
            .caption(resourceBundle.getString(ALWAYS_ON_TOP))
            .build();
  }

  /**
   * @return a Control for viewing information about the application
   */
  protected final Control createAboutControl() {
    return Control.builder(this::displayAbout)
            .caption(resourceBundle.getString(ABOUT) + "...")
            .build();
  }

  /**
   * @return a Control for displaying the help
   */
  protected final Control createHelpControl() {
    return Control.builder(this::displayHelp)
            .caption(resourceBundle.getString(HELP) + "...")
            .build();
  }

  /**
   * @return a Control for displaying the keyboard shortcuts overview
   */
  protected final Control createViewKeyboardShortcutsControl() {
    return Control.builder(this::displayKeyboardShortcuts)
            .caption(resourceBundle.getString(KEYBOARD_SHORTCUTS) + "...")
            .build();
  }

  /**
   * @return the panel shown when Help -&#62; About is selected
   */
  protected JPanel createAboutPanel() {
    PanelBuilder versionMemoryPanel = panel(gridLayout(0, 2))
            .border(createEmptyBorder(5, 5, 5, 5));
    applicationModel().version().ifPresent(version -> versionMemoryPanel
            .add(new JLabel(resourceBundle.getString(APPLICATION_VERSION) + ":"))
            .add(new JLabel(version.toString())));
    versionMemoryPanel
            .add(new JLabel(resourceBundle.getString(CODION_VERSION) + ":"))
            .add(new JLabel(Version.versionAndMetadataString()))
            .add(new JLabel(resourceBundle.getString(MEMORY_USAGE) + ":"))
            .add(new JLabel(Memory.memoryUsage()));

    return panel(borderLayout())
            .border(createEmptyBorder(5, 5, 5, 5))
            .add(new JLabel(FrameworkIcons.instance().logo(DEFAULT_LOGO_SIZE)), BorderLayout.WEST)
            .add(versionMemoryPanel.build(), BorderLayout.CENTER)
            .build();
  }

  /**
   * Override to add event bindings after initialization
   */
  protected void bindEvents() {}

  /**
   * Displays the panel provided by the given builder in a frame or dialog,
   * depending on {@link EntityApplicationPanel#DISPLAY_ENTITY_PANELS_IN_FRAME}.
   * @param panelBuilder the entity panel builder
   */
  protected final void displayEntityPanel(EntityPanel.Builder panelBuilder) {
    if (DISPLAY_ENTITY_PANELS_IN_FRAME.get()) {
      displayEntityPanelFrame(panelBuilder);
    }
    else {
      displayEntityPanelDialog(panelBuilder);
    }
  }

  /**
   * Shows a frame containing the entity panel provided by the given panel builder
   * @param panelBuilder the entity panel builder
   */
  protected final void displayEntityPanelFrame(EntityPanel.Builder panelBuilder) {
    requireNonNull(panelBuilder, "panelBuilder");
    WaitCursor.show(this);
    try {
      EntityPanel entityPanel = entityPanel(panelBuilder);
      if (entityPanel.isShowing()) {
        Window parentWindow = Utilities.parentWindow(entityPanel);
        if (parentWindow != null) {
          parentWindow.toFront();
        }
      }
      else {
        Windows.frame(entityPanel)
                .locationRelativeTo(this)
                .title(panelBuilder.caption().orElse(applicationModel.entities().definition(panelBuilder.entityType()).caption()))
                .defaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                .onClosed(windowEvent -> {
                  entityPanel.model().savePreferences();
                  entityPanel.savePreferences();
                })
                .show();
      }
    }
    finally {
      WaitCursor.hide(this);
    }
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel builder
   * @param panelBuilder the entity panel builder
   */
  protected final void displayEntityPanelDialog(EntityPanel.Builder panelBuilder) {
    displayEntityPanelDialog(panelBuilder, false);
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel builder
   * @param panelBuilder the entity panel builder
   * @param modalDialog if true the dialog is made modal
   */
  protected final void displayEntityPanelDialog(EntityPanel.Builder panelBuilder, boolean modalDialog) {
    requireNonNull(panelBuilder, "panelBuilder");
    WaitCursor.show(this);
    try {
      EntityPanel entityPanel = entityPanel(panelBuilder);
      if (entityPanel.isShowing()) {
        Window parentWindow = Utilities.parentWindow(entityPanel);
        if (parentWindow != null) {
          parentWindow.toFront();
        }
      }
      else {
        String dialogTitle = panelBuilder.caption().orElse(applicationModel.entities().definition(panelBuilder.entityType()).caption());
        Dialogs.componentDialog(entityPanel)
                .owner(parentWindow().orElse(null))
                .title(dialogTitle)
                .onClosed(e -> {
                  entityPanel.model().savePreferences();
                  entityPanel.savePreferences();
                })
                .modal(modalDialog)
                .resizable(true)
                .show();
      }
    }
    finally {
      WaitCursor.hide(this);
    }
  }

  /**
   * Initializes this EntityApplicationPanel UI
   */
  protected void initializeUI() {
    applicationTabPane = createApplicationTabPane();
    setLayout(new BorderLayout());
    //tab pane added to a base panel for correct Look&Feel rendering
    add(panel(borderLayout())
            .add(applicationTabPane, BorderLayout.CENTER)
            .build(), BorderLayout.CENTER);
    JPanel northPanel = createNorthPanel();
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }

    JPanel southPanel = createSouthPanel();
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * Creates the {@link EntityPanel}s to include in this application panel, in the order they should appear in the tab pane.
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
   * Creates a panel to display in the NORTH position of this application panel.
   * override to provide a north panel.
   * @return a panel for the NORTH position
   */
  protected JPanel createNorthPanel() {
    return null;
  }

  /**
   * Creates a panel to display in the SOUTH position of this application frame,
   * override to provide a south panel.
   * @return a panel for the SOUTH position
   */
  protected JPanel createSouthPanel() {
    return null;
  }

  /**
   * Adds a listener notified when the application is about to exit.
   * To cancel the exit throw a {@link CancelException}.
   * @param listener a listener notified when the application is about to exit
   */
  protected final void addOnExitListener(EventListener listener) {
    onExitEvent.addListener(listener);
  }

  /**
   * Creates the JMenuBar to use on the application Frame
   * @return by default a JMenuBar based on the main menu controls
   * @see #createMainMenuControls()
   */
  protected JMenuBar createMenuBar() {
    Controls mainMenuControls = createMainMenuControls();
    if (mainMenuControls == null || mainMenuControls.isEmpty()) {
      return null;
    }

    return menu(mainMenuControls).createMenuBar();
  }

  /**
   * @return the default username, that is, the username of the last successful login from user preferences,
   * or the operating system username, if no username is found in user preferences.
   */
  protected final String defaultUsername() {
    return getUserPreference(applicationDefaultUsernameProperty,
            EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name"));
  }

  /**
   * Called during the exit() method, override to save user preferences on program exit,
   * remember to call super.savePreferences() when overriding
   * @see EntityApplicationModel#savePreferences()
   */
  protected void savePreferences() {
    entityPanels().forEach(EntityPanel::savePreferences);
    applicationModel().savePreferences();
  }

  private JTabbedPane createApplicationTabPane() {
    JTabbedPane tabbedPane = new JTabbedPane(TAB_PLACEMENT.get());
    tabbedPane.setFocusable(false);
    tabbedPane.addChangeListener(e -> {
      if (tabbedPane.getTabCount() > 0) {
        ((EntityPanel) tabbedPane.getSelectedComponent()).initializePanel();
      }
    });
    for (EntityPanel entityPanel : entityPanels) {
      tabbedPane.addTab(entityPanel.getCaption(), null, entityPanel, entityPanel.getDescription());
      if (entityPanel.editPanel() != null) {
        entityPanel.editPanel().addActiveListener(panelActivated -> {
          if (panelActivated) {
            selectChildPanel(entityPanel);
          }
        });
      }
    }
    //initialize first panel
    if (tabbedPane.getTabCount() > 0) {
      ((EntityPanel) tabbedPane.getSelectedComponent()).initializePanel();
    }

    return tabbedPane;
  }

  private void bindEventsInternal() {
    alwaysOnTopState.addDataListener(alwaysOnTop ->
            parentWindow().ifPresent(parent -> parent.setAlwaysOnTop(alwaysOnTop)));
  }

  private Control createSupportPanelControl(EntityPanel.Builder panelBuilder) {
    return Control.builder(() -> displayEntityPanel(panelBuilder))
            .caption(panelBuilder.caption().orElse(applicationModel.entities().definition(panelBuilder.entityType()).caption()))
            .build();
  }

  private EntityPanel entityPanel(EntityPanel.Builder panelBuilder) {
    if (PERSIST_ENTITY_PANELS.get() && persistentEntityPanels.containsKey(panelBuilder)) {
      return persistentEntityPanels.get(panelBuilder);
    }

    EntityPanel entityPanel = panelBuilder.buildPanel(applicationModel.connectionProvider());
    entityPanel.initializePanel();
    if (PERSIST_ENTITY_PANELS.get()) {
      persistentEntityPanels.put(panelBuilder, entityPanel);
    }

    return entityPanel;
  }

  private JScrollPane createApplicationTree() {
    return createTree(createApplicationTree(entityPanels));
  }

  private JScrollPane createDependencyTree() {
    return createTree(createDependencyTreeModel(applicationModel.entities()));
  }

  private boolean cancelExit() {
    boolean cancelForUnsavedData = applicationModel().isWarnAboutUnsavedData() && applicationModel().containsUnsavedData() &&
            JOptionPane.showConfirmDialog(this, FrameworkMessages.unsavedDataWarning(),
                    FrameworkMessages.unsavedDataWarningTitle(),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION;
    boolean exitNotConfirmed = CONFIRM_EXIT.get() && JOptionPane.showConfirmDialog(this,
            FrameworkMessages.confirmExit(), FrameworkMessages.confirmExitTitle(),
            JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION;

    return cancelForUnsavedData || exitNotConfirmed;
  }

  /**
   * Looks up user credentials via a {@link CredentialsProvider} service using an authentication token
   * found in the program arguments list. Useful for single sign on application launch.
   * <pre>javaws -open authenticationToken:123-123-123 http://codion.is/demo/demo.jnlp</pre>
   * <pre>java -jar application/getdown-1.7.1.jar app_dir app_id authenticationToken:123-123-123</pre>
   * @param args the program arguments
   * @return the User credentials associated with the authentication token, null if no authentication token is found,
   * the user credentials have expired or if no authentication server is running
   */
  protected static User getUser(String[] args) {
    Optional<UUID> token = CredentialsProvider.authenticationToken(args);
    try {
      if (token.isPresent()) {
        Optional<CredentialsProvider> optionalProvider = CredentialsProvider.instance();
        if (optionalProvider.isPresent()) {
          return optionalProvider.get().credentials(token.get()).orElse(null);
        }
      }

      LOG.debug("No CredentialsProvider available");
      return null;
    }
    catch (CredentialsException e) {
      LOG.debug("CredentialsService not reachable", e);
      return null;
    }
    catch (IllegalArgumentException e) {
      LOG.debug("Invalid UUID authentication token");
      return null;
    }
  }

  private static Map<Object, State> createLogLevelStateMap() {
    LoggerProxy loggerProxy = LoggerProxy.instance();
    if (loggerProxy == LoggerProxy.NULL_PROXY) {
      return Collections.emptyMap();
    }
    Object currentLogLevel = loggerProxy.getLogLevel();
    Map<Object, State> levelStateMap = new LinkedHashMap<>();
    State.Group logLevelStateGroup = State.group();
    for (Object logLevel : loggerProxy.logLevels()) {
      State logLevelState = State.state(Objects.equals(logLevel, currentLogLevel));
      logLevelStateGroup.add(logLevelState);
      logLevelState.addDataListener(enabled -> {
        if (enabled) {
          loggerProxy.setLogLevel(logLevel);
        }
      });
      levelStateMap.put(logLevel, logLevelState);
    }

    return Collections.unmodifiableMap(levelStateMap);
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
      DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel.getCaption());
      root.add(node);
      if (!entityPanel.childPanels().isEmpty()) {
        addModelsToTree(node, (Collection<? extends EntityPanel>) entityPanel.childPanels());
      }
    }
  }

  private static boolean referencesOnlySelf(Entities entities, EntityType entityType) {
    return entities.definition(entityType).foreignKeys().stream()
            .allMatch(foreignKey -> foreignKey.referencedType().equals(entityType));
  }

  private static final class SupportPanelBuilderComparator implements Comparator<EntityPanel.Builder> {

    private final Entities entities;
    private final Comparator<String> comparator = Text.spaceAwareCollator();

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
        for (ForeignKeyProperty fkProperty : definition.foreignKeyProperties()) {
          if (fkProperty.referencedType().equals(entityType()) && !fkProperty.isSoftReference()
                  && !foreignKeyCycle(fkProperty.referencedType())) {
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
   * Builds a {@link EntityApplicationPanel} and starts the application.
   * @param <M> the application model type
   * @param <P> the application panel type
   * @see EntityApplicationPanel#builder(Class, Class)
   * @see #start()
   */
  public interface Builder<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> {

    /**
     * @param domainClassName the full name of the domain implementation class
     * @return this Builder instance
     */
    Builder<M, P> domainClassName(String domainClassName);

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
     * {@link LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)}
     * before starting the application.
     * @param defaultLookAndFeelClassName the default look and feel classname
     * @return this Builder instance
     */
    Builder<M, P> defaultLookAndFeelClassName(String defaultLookAndFeelClassName);

    /**
     * Sets the look and feel classname, overrides any look and feel settings found in user preferences.
     * Note that for an external Look and Feels to be enabled, it must be registered via
     * {@link LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)}
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
    Builder<M, P> loginPanelSouthComponentSupplier(Supplier<JComponent> loginPanelSouthComponentSupplier);

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
    Builder<M, P> onApplicationStarted(EventDataListener<P> onApplicationStarted);

    /**
     * @param frameSupplier the frame supplier
     * @return this Builder instance
     */
    Builder<M, P> frameSupplier(Supplier<JFrame> frameSupplier);

    /**
     * @param frameTitleFactory the frame title factory
     * @return this Builder instance
     */
    Builder<M, P> frameTitleFactory(Function<M, String> frameTitleFactory);

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
     * @param displayProgressDialog if true then a progress dialog is displayed while the application is being initialized
     * @return this Builder instance
     */
    Builder<M, P> displayStartupDialog(boolean displayProgressDialog);

    /**
     * @param frameSize the frame size when not maximized
     * @return this Builder instance
     */
    Builder<M, P> frameSize(Dimension frameSize);

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
       * @param domainClassName the full name of the domain model class
       * @param clientTypeId the client type id
       * @param clientVersion the client version
       * @return a new {@link EntityConnectionProvider} instance.
       */
      default EntityConnectionProvider createConnectionProvider(User user, String domainClassName, String clientTypeId, Version clientVersion) {
        return EntityConnectionProvider.builder()
                .user(user)
                .domainClassName(domainClassName)
                .clientTypeId(clientTypeId)
                .clientVersion(clientVersion)
                .build();
      }
    }
  }
}
