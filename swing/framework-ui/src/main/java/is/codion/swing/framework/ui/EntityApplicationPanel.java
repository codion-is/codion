/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
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
import is.codion.swing.common.model.component.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.panel.HierarchyPanel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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
import java.awt.Frame;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import java.util.function.Supplier;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A central application panel class.
 * @param <M> the application model type
 */
public abstract class EntityApplicationPanel<M extends SwingEntityApplicationModel>
        extends JPanel implements HierarchyPanel {

  private static final String CODION_CLIENT_VERSION = "codion.client.version";
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
   * Default value: https://codion.is/doc/{version}/{jdk}/help/client.html
   */
  public static final PropertyValue<String> HELP_URL = Configuration.stringValue("codion.swing.helpUrl",
          "https://codion.is/doc/" + Version.versionString() + "/jdk11/help/client.html");

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

  private static final String DEFAULT_USERNAME_PROPERTY = "is.codion.swing.framework.ui.defaultUsername";
  private static final String LOOK_AND_FEEL_PROPERTY = "is.codion.swing.framework.ui.LookAndFeel";
  private static final String FONT_SIZE_PROPERTY = "is.codion.swing.framework.ui.FontSize";

  private static final int DEFAULT_LOGO_SIZE = 68;

  /** Non-static so that Locale.setDefault(...) can be called in the main method of a subclass */
  private final ResourceBundle resourceBundle = ResourceBundle.getBundle(EntityApplicationPanel.class.getName());

  private final String applicationDefaultUsernameProperty;
  private final String applicationLookAndFeelProperty;
  private final String applicationFontSizeProperty;

  private final Supplier<JFrame> frameProvider;
  private final List<EntityPanel.Builder> supportPanelBuilders = new ArrayList<>();
  private final List<EntityPanel> entityPanels = new ArrayList<>();

  private M applicationModel;
  private JTabbedPane applicationTabPane;

  private final Event<JFrame> applicationStartedEvent = Event.event();
  private final State alwaysOnTopState = State.state();
  private final Event<?> onExitEvent = Event.event();

  private final Map<EntityPanel.Builder, EntityPanel> persistentEntityPanels = new HashMap<>();

  private final Map<Object, State> logLevelStates;

  private final String applicationName;

  private ImageIcon applicationIcon;

  /**
   * @param applicationName the application name
   */
  public EntityApplicationPanel(String applicationName) {
    this(applicationName, null);
  }

  /**
   * @param applicationName the application name
   * @param applicationIcon the application icon
   */
  public EntityApplicationPanel(String applicationName, ImageIcon applicationIcon) {
    this(applicationName, applicationIcon, JFrame::new);
  }

  /**
   * @param applicationName the application name
   * @param applicationIcon the application icon
   * @param frameProvider the JFrame provider
   */
  public EntityApplicationPanel(String applicationName, ImageIcon applicationIcon, Supplier<JFrame> frameProvider) {
    this.frameProvider = frameProvider;
    this.applicationName = applicationName == null ? "" : applicationName;
    this.applicationIcon = applicationIcon;
    this.applicationDefaultUsernameProperty = DEFAULT_USERNAME_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationLookAndFeelProperty = LOOK_AND_FEEL_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationFontSizeProperty = FONT_SIZE_PROPERTY + "#" + getClass().getSimpleName();
    this.logLevelStates = createLogLevelStateMap();
    //initialize button captions, not in a static initializer since applications may set the locale in main()
    UiManagerDefaults.initialize();
    setUncaughtExceptionHandler();
  }

  /**
   * Displays the exception in a dialog
   * @param exception the exception to display
   */
  public final void displayException(Throwable exception) {
    LOG.error(exception.getMessage(), exception);
    Dialogs.showExceptionDialog(exception, Utilities.getParentWindow(this).orElse(null));
  }

  /**
   * @return the application model this application panel is based on
   */
  public final M model() {
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
    return Utilities.getParentWindow(this);
  }

  /**
   * @return the application name
   */
  public final String applicationName() {
    return applicationName;
  }

  /**
   * @return the application icon, the default logo icon if none has been specified
   * @see FrameworkIcons#logo(int)
   */
  public final ImageIcon applicationIcon() {
    if (applicationIcon == null) {
      applicationIcon = FrameworkIcons.instance().logo(DEFAULT_LOGO_SIZE);
    }

    return applicationIcon;
  }

  /**
   * @return true if the frame this application panel is shown in should be 'alwaysOnTop'
   */
  public final boolean isAlwaysOnTop() {
    return alwaysOnTopState.get();
  }

  /**
   * @param alwaysOnTop the new value
   * @see #addAlwaysOnTopListener(EventDataListener)
   */
  public final void setAlwaysOnTop(boolean alwaysOnTop) {
    alwaysOnTopState.set(alwaysOnTop);
  }

  /**
   * @return a State controlling the alwaysOnTop state of this panels parent window
   */
  public final State alwaysOnTopState() {
    return alwaysOnTopState;
  }

  /**
   * Shows a dialog for setting the log level
   */
  public final void setLogLevel() {
    LoggerProxy loggerProxy = LoggerProxy.instance();
    if (loggerProxy == LoggerProxy.NULL_PROXY) {
      throw new RuntimeException("No LoggerProxy implementation available");
    }
    ComboBoxModel<Object> model = new DefaultComboBoxModel<>(loggerProxy.logLevels().toArray());
    model.setSelectedItem(loggerProxy.getLogLevel());
    Dialogs.okCancelDialog(new JComboBox<>(model))
            .owner(this)
            .title(resourceBundle.getString(LOG_LEVEL))
            .onOk(() -> logLevelStates.get(model.getSelectedItem()).set(true))
            .show();
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
            .title(FrameworkMessages.viewDependencies())
            .modal(false)
            .show();
  }

  /**
   * Display a dialog for selecting the application font size multiplier
   */
  public final void selectFontSize() {
    List<Item<Integer>> values = new ArrayList<>();
    for (int i = 50; i <= 200; i += 5) {
      values.add(Item.item(i, i + "%"));
    }
    ItemComboBoxModel<Integer> comboBoxModel = ItemComboBoxModel.createModel(values);
    Integer fontSizeMultiplier = fontSizeMultiplier();

    Dialogs.okCancelDialog(Components.panel(Layouts.borderLayout())
                    .add(Components.itemComboBox(comboBoxModel)
                            .initialValue(fontSizeMultiplier)
                            .renderer(new FontSizeCellRenderer(values, fontSizeMultiplier))
                            .build(), BorderLayout.CENTER)
                    .border(BorderFactory.createEmptyBorder(10, 10, 0, 10))
                    .build())
            .owner(this)
            .title(resourceBundle.getString("select_font_size"))
            .onOk(() -> {
              Integer selectedFontSizeMultiplier = comboBoxModel.getSelectedItem().value();
              if (!selectedFontSizeMultiplier.equals(fontSizeMultiplier)) {
                UserPreferences.putUserPreference(applicationFontSizeProperty, selectedFontSizeMultiplier.toString());
                JOptionPane.showMessageDialog(this, resourceBundle.getString("font_size_selected_message"));
              }
            })
            .show();
  }

  @Override
  public final Optional<HierarchyPanel> parentPanel() {
    return Optional.empty();
  }

  @Override
  public final Optional<HierarchyPanel> selectedChildPanel() {
    if (applicationTabPane != null) {//initializeUI() may have been overridden
      return Optional.of((HierarchyPanel) applicationTabPane.getSelectedComponent());
    }

    return entityPanels.isEmpty() ? Optional.empty() : Optional.of(entityPanels.get(0));
  }

  @Override
  public final void selectChildPanel(HierarchyPanel childPanel) {
    if (applicationTabPane != null) {//initializeUI() may have been overridden
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
   * @see #addOnExitListener(EventListener)
   * @see EntityApplicationPanel#CONFIRM_EXIT
   * @see EntityApplicationModel#isWarnAboutUnsavedData()
   * @throws CancelException if the exit is cancelled
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
    shortcutsPanel.setPreferredSize(new Dimension(shortcutsPanel.getPreferredSize().width, Windows.getScreenSizeRatio(0.5).height));
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
   * @param listener a listener notified each time the always on top status changes
   */
  public final void addAlwaysOnTopListener(EventDataListener<Boolean> listener) {
    alwaysOnTopState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAlwaysOnTopListener(EventListener listener) {
    alwaysOnTopState.removeListener(listener);
  }

  /**
   * @param listener a listener notified when to application has been successfully started
   */
  public final void addApplicationStartedListener(EventDataListener<JFrame> listener) {
    applicationStartedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeApplicationStartedListener(EventDataListener<JFrame> listener) {
    applicationStartedEvent.removeDataListener(listener);
  }

  /**
   * @param entities the entities
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel createDependencyTreeModel(Entities entities) {
    requireNonNull(entities);
    DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    for (EntityDefinition definition : entities.definitions()) {
      if (definition.foreignKeys().isEmpty() || referencesOnlySelf(entities, definition.entityType())) {
        root.add(new EntityDependencyTreeNode(definition.entityType(), entities));
      }
    }

    return new DefaultTreeModel(root);
  }

  /**
   * @return a {@link Starter} for this application panel.
   */
  public Starter starter() {
    return new EntityApplicationPanelStarter(this);
  }

  /**
   * Returns the JTabbedPane used by the default UI, note that this can be null if the default UI
   * initialization has been overridden. Returns null until {@link #initializeUI()} has been called
   * @return the default application tab pane
   */
  protected final JTabbedPane applicationTabPane() {
    return applicationTabPane;
  }

  /**
   * @return the controls on which to base the main menu
   * @see #createFileControls()
   * @see #createSettingsControls()
   * @see #createViewControls()
   * @see #createToolsControls()
   * @see #createHelpControls()
   */
  protected Controls createMainMenuControls() {
    Controls menuControls = Controls.controls();
    Controls fileControls = createFileControls();
    if (fileControls != null && !fileControls.isEmpty()) {
      menuControls.add(fileControls);
    }
    Controls viewControls = createViewControls();
    if (viewControls != null && !viewControls.isEmpty()) {
      menuControls.add(viewControls);
    }
    Controls toolsControls = createToolsControls();
    if (toolsControls != null && !toolsControls.isEmpty()) {
      menuControls.add(toolsControls);
    }
    Controls supportTableControls = createSupportTableControls();
    if (supportTableControls != null && !supportTableControls.isEmpty()) {
      menuControls.add(supportTableControls);
    }
    List<Controls> additionalMenus = createAdditionalMenuControls();
    if (additionalMenus != null) {
      for (Controls set : additionalMenus) {
        menuControls.add(set);
      }
    }
    Controls helpControls = createHelpControls();
    if (helpControls != null && !helpControls.isEmpty()) {
      menuControls.add(helpControls);
    }

    return menuControls;
  }

  /**
   * @return the Controls specifying the items in the 'File' menu
   */
  protected Controls createFileControls() {
    return Controls.builder()
            .caption(FrameworkMessages.file())
            .mnemonic(FrameworkMessages.fileMnemonic())
            .control(createExitControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Settings' menu
   */
  protected Controls createSettingsControls() {
    return Controls.builder()
            .caption(FrameworkMessages.settings())
            .control(createLogLevelControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Tools' menu
   */
  protected Controls createToolsControls() {
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
  protected Controls createViewControls() {
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
  protected Controls createHelpControls() {
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
            .caption(FrameworkMessages.viewDependencies())
            .build();
  }

  /**
   * Allows the user the select between the available Look and Feels, saves the selection as a user preference.
   * @see LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)
   * @see LookAndFeelProvider#getLookAndFeelProvider(String)
   * @see Dialogs#lookAndFeelSelectionDialog()
   * @see LookAndFeelSelectionPanel#CHANGE_DURING_SELECTION
   * @return a Control for selecting the application look and feel
   */
  protected final Control createSelectLookAndFeelControl() {
    return Dialogs.lookAndFeelSelectionDialog()
            .dialogOwner(this)
            .userPreferencePropertyName(applicationLookAndFeelProperty)
            .createControl();
  }

  /**
   * @return a Control for selecting the font size
   */
  protected final Control createSelectFontSizeControl() {
    return Control.builder(this::selectFontSize)
            .caption(resourceBundle.getString("select_font_size"))
            .build();
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
    JPanel panel = new JPanel(Layouts.borderLayout());
    String versionString = Version.versionAndMetadataString();
    panel.add(new JLabel(FrameworkIcons.instance().logo(DEFAULT_LOGO_SIZE)), BorderLayout.WEST);
    Version version = clientVersion();
    JPanel versionMemoryPanel = new JPanel(Layouts.gridLayout(version == null ? 2 : 3, 2));
    versionMemoryPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    if (version != null) {
      versionMemoryPanel.add(new JLabel(resourceBundle.getString(APPLICATION_VERSION) + ":"));
      versionMemoryPanel.add(new JLabel(version.toString()));
    }
    versionMemoryPanel.add(new JLabel(resourceBundle.getString(CODION_VERSION) + ":"));
    versionMemoryPanel.add(new JLabel(versionString));
    versionMemoryPanel.add(new JLabel(resourceBundle.getString(MEMORY_USAGE) + ":"));
    versionMemoryPanel.add(new JLabel(Memory.memoryUsage()));
    panel.add(versionMemoryPanel, BorderLayout.CENTER);

    panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

    return panel;
  }

  /**
   * Initializes the entity connection provider
   * @param user the user
   * @param clientTypeId a string specifying the client type
   * @return an initialized EntityConnectionProvider
   * @throws CancelException in case the initialization is cancelled
   */
  protected EntityConnectionProvider initializeConnectionProvider(User user, String clientTypeId) {
    return EntityConnectionProvider.builder()
            .domainClassName(EntityConnectionProvider.CLIENT_DOMAIN_CLASS.getOrThrow())
            .clientTypeId(clientTypeId)
            .clientVersion(clientVersion())
            .user(user)
            .build();
  }

  /**
   * @return the client version if specified, null by default
   */
  protected Version clientVersion() {
    return null;
  }

  /**
   * Override to add event bindings after initialization
   */
  protected void bindEvents() {}

  /**
   * @return a List of Controls instances which should be added to the main menu bar
   */
  protected List<Controls> createAdditionalMenuControls() {
    return new ArrayList<>(0);
  }

  /**
   * @return the Controls on which to base the Support Tables menu
   */
  protected Controls createSupportTableControls() {
    if (supportPanelBuilders.isEmpty()) {
      return null;
    }

    Comparator<String> comparator = Text.spaceAwareCollator();
    Entities entities = applicationModel.entities();
    supportPanelBuilders.sort((ep1, ep2) -> {
      String thisCompare = ep1.getCaption() == null ? entities.definition(ep1.entityType()).caption() : ep1.getCaption();
      String thatCompare = ep2.getCaption() == null ? entities.definition(ep2.entityType()).caption() : ep2.getCaption();

      return comparator.compare(thisCompare, thatCompare);
    });
    Controls.Builder controlsBuilder = Controls.builder()
            .caption(FrameworkMessages.supportTables())
            .mnemonic(FrameworkMessages.supportTablesMnemonic());
    supportPanelBuilders.forEach(panelBuilder -> controlsBuilder.control(Control.builder(() -> displayEntityPanel(panelBuilder))
            .caption(panelBuilder.getCaption() == null ? entities.definition(panelBuilder.entityType()).caption() : panelBuilder.getCaption())));

    return controlsBuilder.build();
  }

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
      EntityPanel entityPanel = getEntityPanel(panelBuilder);
      if (entityPanel.isShowing()) {
        Utilities.getParentWindow(entityPanel).ifPresent(Window::toFront);
      }
      else {
        Windows.frame(entityPanel)
                .relativeTo(this)
                .title(panelBuilder.getCaption() == null ? applicationModel.entities().definition(panelBuilder.entityType()).caption() : panelBuilder.getCaption())
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
      EntityPanel entityPanel = getEntityPanel(panelBuilder);
      if (entityPanel.isShowing()) {
        Utilities.getParentWindow(entityPanel).ifPresent(Window::toFront);
      }
      else {
        String dialogTitle = panelBuilder.getCaption() == null ?
                applicationModel.entities().definition(panelBuilder.entityType()).caption() :
                panelBuilder.getCaption();
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
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = createApplicationTabPane();
    //base panel added for Look&Feel rendering
    JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(applicationTabPane, BorderLayout.CENTER);
    add(basePanel, BorderLayout.CENTER);

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
   * Creates the {@link EntityPanel}s to include in this application panel.
   * @param applicationModel the application model responsible for providing EntityModels for the panels
   * @return a List containing the {@link EntityPanel}s to include in this application panel
   */
  protected abstract List<EntityPanel> createEntityPanels(M applicationModel);

  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders(M applicationModel) {
    return Collections.emptyList();
  }

  /**
   * Returns the name of the look and feel to use, this default implementation fetches
   * it from user preferences, if no preference is available the default system look and feel is used.
   * @return the look and feel name to use
   * @see #defaultLookAndFeelName()
   */
  protected String lookAndFeelName() {
    return UserPreferences.getUserPreference(applicationLookAndFeelProperty, defaultLookAndFeelName());
  }

  /**
   * @return the default look and feel to use for the system we're running on.
   * @see Utilities#getSystemLookAndFeelClassName()
   */
  protected String defaultLookAndFeelName() {
    return Utilities.getSystemLookAndFeelClassName();
  }

  /**
   * Returns the font size multiplier to use, from user preferences, in percentages, e.g:<br>
   * 85 = decrease the default font size by 15%<br>
   * 100 = use the default font size<br>
   * 125 = increase the default font size by 25%<br>
   * @return the font size multiplier to use
   * @see #selectFontSize()
   */
  protected int fontSizeMultiplier() {
    return Integer.parseInt(UserPreferences.getUserPreference(applicationFontSizeProperty, "100"));
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
   * Initializes the icon panel to show in the startup dialog
   * @param icon the icon
   * @return an initialized startup icon panel
   */
  protected JPanel createStartupIconPanel(Icon icon) {
    JPanel panel = new JPanel(new BorderLayout());
    if (icon != null) {
      panel.add(new JLabel(icon), BorderLayout.CENTER);
    }

    return panel;
  }

  /**
   * @return a frame title based on the application name, version and the logged-in user
   */
  protected String frameTitle() {
    StringBuilder builder = new StringBuilder(applicationName == null ? "" : applicationName);
    Version version = clientVersion();
    if (version != null) {
      if (builder.length() > 0) {
        builder.append(" - ");
      }
      builder.append(version);
    }
    if (builder.length() > 0) {
      builder.append(" - ");
    }
    builder.append(getUserInfo(model().connectionProvider()));

    return builder.toString();
  }

  /**
   * Initializes a JFrame according to the given parameters, containing this EntityApplicationPanel
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize if the JFrame is not maximized then its preferredSize is set to this value
   * @param mainMenu yes if the main menu should be included
   * @return an initialized, but non-visible JFrame
   */
  protected final JFrame prepareFrame(boolean displayFrame, boolean maximizeFrame, Dimension frameSize,
                                      boolean mainMenu) {
    JFrame frame = frameProvider.get();
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setIconImage(applicationIcon().getImage());
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          exit();
        }
        catch (CancelException ignored) {/*ignored*/}
      }
    });
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(this, BorderLayout.CENTER);
    if (frameSize != null) {
      frame.setSize(frameSize);
    }
    else {
      frame.pack();
      Windows.setSizeWithinScreenBounds(frame);
    }
    frame.setLocationRelativeTo(null);
    if (maximizeFrame) {
      frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    }
    frame.setTitle(frameTitle());
    if (mainMenu) {
      frame.setJMenuBar(createMenuBar());
    }
    frame.setAlwaysOnTop(alwaysOnTopState.get());
    if (displayFrame) {
      frame.setVisible(true);
    }

    return frame;
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
    return createMainMenuControls().createMenuBar();
  }

  /**
   * Creates the application model
   * @param connectionProvider the connection provider
   * @return an initialized application model
   * @throws CancelException in case the initialization is cancelled
   */
  protected abstract M createApplicationModel(EntityConnectionProvider connectionProvider);

  /**
   * Returns the user, either via a login dialog or via override, called during startup if login is required
   * @param defaultUser the default user to display in the login dialog
   * @param loginValidator the user login validator
   * @return the application user
   * @throws CancelException in case a login dialog is cancelled
   */
  protected User loginUser(User defaultUser, LoginValidator loginValidator) {
    String loginDialogTitle = (!nullOrEmpty(applicationName) ? (applicationName + " - ") : "") + Messages.login();
    User user = Dialogs.loginDialog()
            .defaultUser(defaultUser == null ? User.user(defaultUsername()) : defaultUser)
            .validator(loginValidator)
            .title(loginDialogTitle)
            .icon(applicationIcon())
            .show();
    if (nullOrEmpty(user.username())) {
      throw new IllegalArgumentException(FrameworkMessages.emptyUsername());
    }

    return user;
  }

  /**
   * Saves the username so that it can be used as default the next time this application is started.
   * @param username the username
   */
  protected void saveDefaultUsername(String username) {
    UserPreferences.putUserPreference(applicationDefaultUsernameProperty, username);
  }

  /**
   * @return a default username previously saved to user preferences or the OS username
   */
  protected String defaultUsername() {
    return UserPreferences.getUserPreference(applicationDefaultUsernameProperty,
            EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name"));
  }

  /**
   * Returns a String identifying the application this EntityApplicationPanel represents,
   * by default the full class name is returned.
   * @return a String identifying the application type this panel represents
   */
  protected String applicationIdentifier() {
    return getClass().getName();
  }

  /**
   * Called during the exit() method, override to save user preferences on program exit,
   * remember to call super.savePreferences() when overriding
   * @see EntityApplicationModel#savePreferences()
   */
  protected void savePreferences() {
    entityPanels().forEach(EntityPanel::savePreferences);
    model().savePreferences();
  }

  final void startApplication(User defaultUser, User silentLoginUser, boolean loginRequired,
                              Dimension frameSize, boolean maximizeFrame, boolean displayFrame,
                              boolean includeMainMenu, boolean displayProgressDialog) {
    LOG.debug("{} application starting", applicationName);
    FrameworkMessages.class.getName();//hack to force-load the class, initializes UI caption constants
    LookAndFeelProvider.getLookAndFeelProvider(lookAndFeelName()).ifPresent(LookAndFeelProvider::enable);
    int fontSize = fontSizeMultiplier();
    if (fontSize != 100) {
      Utilities.setFontSize(fontSize / 100f);
    }

    EntityConnectionProvider connectionProvider = createConnectionProvider(defaultUser, silentLoginUser, loginRequired);

    if (silentLoginUser == null && EntityApplicationModel.SAVE_DEFAULT_USERNAME.get()) {
      saveDefaultUsername(connectionProvider.user().username());
    }

    startApplication(frameSize, maximizeFrame, displayFrame, includeMainMenu, displayProgressDialog, connectionProvider);
  }

  private void startApplication(Dimension frameSize, boolean maximizeFrame, boolean displayFrame,
                                boolean includeMainMenu, boolean displayProgressDialog,
                                EntityConnectionProvider connectionProvider) {
    setVersionProperty();
    long initializationStarted = System.currentTimeMillis();
    if (displayProgressDialog) {
      Dialogs.progressWorkerDialog(() -> createApplicationModel(connectionProvider))
              .title(applicationName)
              .icon(applicationIcon())
              .westPanel(createStartupIconPanel(applicationIcon()))
              .onResult(model -> startApplication(model, frameSize, maximizeFrame, displayFrame, includeMainMenu, initializationStarted))
              .onException(this::displayException)
              .execute();
    }
    else {
      startApplication(createApplicationModel(connectionProvider), frameSize, maximizeFrame, displayFrame, includeMainMenu, initializationStarted);
    }
  }

  private void startApplication(M applicationModel, Dimension frameSize, boolean maximizeFrame,
                                boolean displayFrame, boolean includeMainMenu, long initializationStarted) {
    try {
      this.applicationModel = applicationModel;
      initializePanel();
      JFrame frame = prepareFrame(displayFrame, maximizeFrame, frameSize, includeMainMenu);
      applicationStartedEvent.onEvent(frame);
      LOG.info(frame.getTitle() + ", application started successfully: " + (System.currentTimeMillis() - initializationStarted) + " ms");
    }
    catch (Exception exception) {
      displayException(exception);
      throw new CancelException();
    }
  }

  /**
   * Sets the application version as a system property, so that it appears automatically in exception dialogs.
   */
  private void setVersionProperty() {
    Version version = clientVersion();
    if (version != null) {
      System.setProperty(CODION_CLIENT_VERSION, version.toString());
    }
  }

  private EntityConnectionProvider createConnectionProvider(User defaultUser, User silentLoginUser,
                                                            boolean loginRequired) {
    if (silentLoginUser == null && loginRequired) {
      EntityLoginValidator loginValidator = new EntityLoginValidator();
      loginUser(defaultUser, loginValidator);

      return loginValidator.connectionProvider;
    }

    EntityConnectionProvider connectionProvider = initializeConnectionProvider(silentLoginUser, applicationIdentifier());
    connectionProvider.connection();//throws exception if the server is not reachable

    return connectionProvider;
  }

  private JTabbedPane createApplicationTabPane() {
    JTabbedPane tabbedPane = new JTabbedPane(TAB_PLACEMENT.get());
    tabbedPane.setFocusable(false);
    tabbedPane.addChangeListener(e -> ((EntityPanel) tabbedPane.getSelectedComponent()).initializePanel());
    for (EntityPanel entityPanel : entityPanels) {
      tabbedPane.addTab(entityPanel.getCaption(), entityPanel);
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

  /**
   * Sets the uncaught exception handler
   */
  private void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
      Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
      if (focusOwner == null) {
        focusOwner = EntityApplicationPanel.this;
      }
      displayException(exception);
    });
  }

  private void bindEventsInternal() {
    applicationModel.connectionValidObserver().addDataListener(connectionValid -> SwingUtilities.invokeLater(() ->
            setParentWindowTitle(connectionValid ? frameTitle() : (frameTitle() + " - " + resourceBundle.getString("not_connected")))));
    alwaysOnTopState.addDataListener(alwaysOnTop ->
            parentWindow().ifPresent(parent -> parent.setAlwaysOnTop(alwaysOnTop)));
  }

  private void initializePanel() {
    this.entityPanels.addAll(createEntityPanels(applicationModel));
    this.supportPanelBuilders.addAll(createSupportEntityPanelBuilders(applicationModel));
    initializeUI();
    bindEventsInternal();
    bindEvents();
  }

  private EntityPanel getEntityPanel(EntityPanel.Builder panelBuilder) {
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

  private void setParentWindowTitle(String title) {
    Window parentWindow = Utilities.getParentWindow(this).orElse(null);
    if (parentWindow instanceof JFrame) {
      ((JFrame) parentWindow).setTitle(title);
    }
    else if (parentWindow instanceof JDialog) {
      ((JDialog) parentWindow).setTitle(title);
    }
  }

  private boolean cancelExit() {
    boolean cancelForUnsavedData = model().isWarnAboutUnsavedData() && model().containsUnsavedData() &&
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
      logLevelStateGroup.addState(logLevelState);
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

  private static String getUserInfo(EntityConnectionProvider connectionProvider) {
    String description = connectionProvider.description();

    return getUsername(connectionProvider.user().username().toUpperCase()) + (description != null ? "@" + description.toUpperCase() : "");
  }

  private static String getUsername(String username) {
    String usernamePrefix = EntityApplicationModel.USERNAME_PREFIX.get();
    if (!nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
      return username.substring(usernamePrefix.length());
    }

    return username;
  }

  private static boolean referencesOnlySelf(Entities entities, EntityType entityType) {
    return entities.definition(entityType).foreignKeys().stream()
            .allMatch(foreignKey -> foreignKey.referencedType().equals(entityType));
  }

  /**
   * A starter for entity application panels.
   */
  public interface Starter {

    /**
     * @param includeMainMenu if true then a main menu is included
     * @return this Starter instance
     */
    Starter includeMainMenu(boolean includeMainMenu);

    /**
     * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
     * @return this Starter instance
     */
    Starter maximizeFrame(boolean maximizeFrame);

    /**
     * @param displayFrame specifies whether the frame should be displayed or left invisible
     * @return this Starter instance
     */
    Starter displayFrame(boolean displayFrame);

    /**
     * @param displayProgressDialog if true then a progress dialog is displayed while the application is being initialized
     * @return this Starter instance
     */
    Starter displayProgressDialog(boolean displayProgressDialog);

    /**
     * @param frameSize the frame size when not maximized
     * @return this Starter instance
     */
    Starter frameSize(Dimension frameSize);

    /**
     * @param loginRequired true if a login dialog is required for this application,
     * false if the user is supplied differently
     * @return this Starter instance
     */
    Starter loginRequired(boolean loginRequired);

    /**
     * @param defaultLoginUser the default user to display in the login dialog
     * @return this Starter instance
     */
    Starter defaultLoginUser(User defaultLoginUser);

    /**
     * @param silentLoginUser if specified the application is started silently with that user, displaying no login or progress dialog
     * @return this Starter instance
     */
    Starter silentLoginUser(User silentLoginUser);

    /**
     * Starts the application, should be called on the Event Dispatch Thread
     */
    void start();
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
          if (fkProperty.referencedEntityType().equals(entityType()) && !fkProperty.softReference()
                  && !foreignKeyCycle(fkProperty.referencedEntityType())) {
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

  private final class EntityLoginValidator implements LoginValidator {

    private EntityConnectionProvider connectionProvider;

    @Override
    public void validate(User user) throws Exception {
      connectionProvider = initializeConnectionProvider(user, applicationIdentifier());
      connectionProvider.connection();//throws exception if the server is not reachable
    }
  }

  private static final class FontSizeCellRenderer implements ListCellRenderer<Item<Integer>> {

    private final DefaultListCellRenderer defaultListCellRenderer = new DefaultListCellRenderer();
    private final List<Item<Integer>> values;
    private final Integer defaultFontSize;

    private FontSizeCellRenderer(List<Item<Integer>> values, Integer defaultFontSize) {
      this.values = values;
      this.defaultFontSize = defaultFontSize;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Item<Integer>> list, Item<Integer> value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
      Component component = defaultListCellRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (index >= 0) {
        Font font = component.getFont();
        int newSize = Math.round(font.getSize() * (values.get(index).value() / (float) defaultFontSize.doubleValue()));
        component.setFont(new Font(font.getName(), font.getStyle(), newSize));
      }

      return component;
    }
  }
}
