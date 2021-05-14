/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.PropertyValue;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Components.LookAndFeelProvider;
import is.codion.swing.common.ui.HierarchyPanel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.LoginPanel;
import is.codion.swing.common.ui.LoginPanel.LoginValidator;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.worker.ProgressWorker;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static is.codion.common.Util.nullOrEmpty;
import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Objects.requireNonNull;

/**
 * A central application panel class.
 * @param <M> the application model type
 */
public abstract class EntityApplicationPanel<M extends SwingEntityApplicationModel>
        extends JPanel implements DialogExceptionHandler, HierarchyPanel {

  /** Non-static so that Locale.setDefault(...) can be called in the main method of a subclass */
  private final ResourceBundle resourceBundle = ResourceBundle.getBundle(EntityApplicationPanel.class.getName());

  private static final String SET_LOG_LEVEL = "set_log_level";
  private static final String SET_LOG_LEVEL_DESC = "set_log_level_desc";
  private static final String SELECT_LOOK_AND_FEEL = "select_look_and_feel";
  private static final String LOOK_AND_FEEL_SELECTED_MESSAGE = "look_and_feel_selected_message";
  private static final String HELP = "help";
  private static final String ABOUT = "about";
  private static final String ALWAYS_ON_TOP = "always_on_top";

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

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
   * Specifies if EntityPanels opened via the {@code EntityApplicationPanel.displayEntityPanelDialog} method
   * should be persisted, or kept in memory, when the dialog is closed, instead of being created each time.<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see EntityApplicationPanel#displayEntityPanelDialog(EntityPanel.Builder)
   */
  public static final PropertyValue<Boolean> PERSIST_ENTITY_PANELS = Configuration.booleanValue("codion.swing.persistEntityPanels", false);

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)<br>
   * Default value: JTabbedPane.TOP
   */
  public static final PropertyValue<Integer> TAB_PLACEMENT = Configuration.integerValue("codion.swing.tabPlacement", JTabbedPane.TOP);

  private static final String DEFAULT_USERNAME_PROPERTY = "is.codion.swing.framework.ui.defaultUsername";
  private static final String LOOK_AND_FEEL_PROPERTY = "is.codion.swing.framework.ui.LookAndFeel";
  private static final String FONT_SIZE_PROPERTY = "is.codion.swing.framework.ui.FontSize";
  private static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";
  private static final Dimension MINIMUM_HELP_WINDOW_SIZE = new Dimension(600, 750);
  private static final double HELP_DIALOG_SCREEN_SIZE_RATIO = 0.1;

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

  private final String applicationName;
  private final ImageIcon applicationIcon;

  /**
   * @param applicationName the application name
   */
  public EntityApplicationPanel(final String applicationName) {
    this(applicationName, null);
  }

  /**
   * @param applicationName the application name
   * @param applicationIcon the application icon
   */
  public EntityApplicationPanel(final String applicationName, final ImageIcon applicationIcon) {
    this(applicationName, applicationIcon, JFrame::new);
  }

  /**
   * @param applicationName the application name
   * @param applicationIcon the application icon
   * @param frameProvider the JFrame provider
   */
  public EntityApplicationPanel(final String applicationName, final ImageIcon applicationIcon, final Supplier<JFrame> frameProvider) {
    this.frameProvider = frameProvider;
    this.applicationName = applicationName == null ? "" : applicationName;
    this.applicationIcon = applicationIcon == null ? icons().logoTransparent() : applicationIcon;
    this.applicationDefaultUsernameProperty = DEFAULT_USERNAME_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationLookAndFeelProperty = LOOK_AND_FEEL_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationFontSizeProperty = FONT_SIZE_PROPERTY + "#" + getClass().getSimpleName();
    //initialize button captions, not in a static initializer since applications may set the locale in main()
    UiManagerDefaults.initialize();
    setUncaughtExceptionHandler();
  }

  @Override
  public final void displayException(final Throwable exception, final Window dialogParent) {
    LOG.error(exception.getMessage(), exception);
    DefaultDialogExceptionHandler.getInstance().displayException(exception, dialogParent);
  }

  /**
   * @return the application model this application panel is based on
   */
  public final M getModel() {
    return applicationModel;
  }

  /**
   * @param entityType the entityType
   * @return the first entity panel found based on the given entity type, null if none is found
   */
  public final EntityPanel getEntityPanel(final EntityType<?> entityType) {
    return entityPanels.stream().filter(entityPanel ->
            entityPanel.getModel().getEntityType().equals(entityType)).findFirst().orElse(null);
  }

  /**
   * @return an unmodifiable view of the main application panels
   */
  public final List<EntityPanel> getEntityPanels() {
    return Collections.unmodifiableList(entityPanels);
  }

  /**
   * @return the parent window of this panel, if one exists, null otherwise
   */
  public final Window getParentWindow() {
    return Windows.getParentWindow(this);
  }

  /**
   * @return the application name
   */
  public final String getApplicationName() {
    return applicationName;
  }

  /**
   * @return the application icon, if any
   */
  public final ImageIcon getApplicationIcon() {
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
  public final void setAlwaysOnTop(final boolean alwaysOnTop) {
    alwaysOnTopState.set(alwaysOnTop);
  }

  /**
   * @return a State controlling the alwaysOnTop state of this panels parent window
   */
  public final State getAlwaysOnTopState() {
    return alwaysOnTopState;
  }

  /**
   * Performs a login, fetching user information
   * @throws CancelException in case the login is cancelled
   */
  public final void login() {
    applicationModel.login(getLoginUser(null, new EntityLoginValidator(getModel().getConnectionProvider())));
  }

  /**
   * Performs a logout
   */
  public final void logout() {
    applicationModel.logout();
  }

  /**
   * Shows a dialog for setting the log level
   */
  public final void setLogLevel() {
    final LoggerProxy loggerProxy = LoggerProxy.loggerProxy();
    if (loggerProxy == null) {
      throw new RuntimeException("No LoggerProxy implementation available");
    }
    final DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>(loggerProxy.getLogLevels().toArray());
    model.setSelectedItem(loggerProxy.getLogLevel());
    JOptionPane.showMessageDialog(this, new JComboBox<>(model),
            resourceBundle.getString(SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    loggerProxy.setLogLevel(model.getSelectedItem());

  }

  /**
   * Displays in a dialog a tree describing the application layout
   */
  public final void viewApplicationTree() {
    Dialogs.builder()
            .owner(this)
            .component(initializeApplicationTree())
            .title(resourceBundle.getString("view_application_tree"))
            .modal(false)
            .build().setVisible(true);
  }

  /**
   * Shows a dialog containing a dependency tree view of all defined entities
   */
  public final void viewDependencyTree() {
    Dialogs.builder()
            .owner(this)
            .component(initializeDependencyTree())
            .title(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES))
            .modal(false)
            .build().setVisible(true);
  }

  /**
   * Allows the user the select between the available Look and Feels, saving the selection as a user preference,
   * which will be activated on next appliation start.
   * @see Components#addLookAndFeelProvider(LookAndFeelProvider)
   * @see Components#getLookAndFeelProvider(String)
   * @see Components#selectLookAndFeel(JComponent, String)
   */
  public final void selectLookAndFeel() {
    final LookAndFeelProvider provider = Components.selectLookAndFeel(this, resourceBundle.getString(SELECT_LOOK_AND_FEEL));
    if (provider != null) {
      UserPreferences.putUserPreference(applicationLookAndFeelProperty, provider.getName());
      JOptionPane.showMessageDialog(this, resourceBundle.getString(LOOK_AND_FEEL_SELECTED_MESSAGE));
    }
  }

  /**
   * Display a dialog for selecting the application font size
   */
  public final void selectFontSize() {
    final List<Item<Integer>> values = new ArrayList<>(21);
    for (int i = 100; i <= 200; i += 5) {
      values.add(Item.item(i, i + "%"));
    }
    final ItemComboBoxModel<Integer> comboBoxModel = new ItemComboBoxModel<>(values);
    final Integer defaultFontSize = getDefaultFontSize();
    comboBoxModel.setSelectedItem(defaultFontSize);

    final JComboBox<Item<Integer>> comboBox = new JComboBox<>(comboBoxModel);
    comboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList list, final Object value, final int index,
                                                    final boolean isSelected, final boolean cellHasFocus) {
        final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (index >= 0) {
          final Font font = component.getFont();
          final int newSize = Math.round(font.getSize() * (values.get(index).getValue() / (float) defaultFontSize.doubleValue()));
          component.setFont(new Font(font.getName(), font.getStyle(), newSize));
        }

        return component;
      }
    });

    final int option = JOptionPane.showOptionDialog(this, comboBox,
            resourceBundle.getString("select_font_size"), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (option == JOptionPane.OK_OPTION) {
      UserPreferences.putUserPreference(applicationFontSizeProperty, ((Item<Integer>) comboBoxModel.getSelectedItem()).getValue().toString());
      JOptionPane.showMessageDialog(this, resourceBundle.getString("font_size_selected_message"));
    }
  }

  @Override
  public final HierarchyPanel getParentPanel() {
    return null;
  }

  @Override
  public final EntityPanel getSelectedChildPanel() {
    if (applicationTabPane != null) {//initializeUI() may have been overridden
      applicationTabPane.getSelectedComponent();
    }

    return entityPanels.isEmpty() ? null : entityPanels.get(0);
  }

  @Override
  public final void setSelectedChildPanel(final HierarchyPanel childPanel) {
    if (applicationTabPane != null) {//initializeUI() may have been overridden
      applicationTabPane.setSelectedComponent((JComponent) childPanel);
    }
  }

  @Override
  public final HierarchyPanel getPreviousSiblingPanel() {
    return null;
  }

  @Override
  public final HierarchyPanel getNextSiblingPanel() {
    return null;
  }

  @Override
  public final List<HierarchyPanel> getChildPanels() {
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
    catch (final CancelException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.debug("Exception while exiting", e);
    }
    try {
      savePreferences();
      UserPreferences.flushUserPreferences();
    }
    catch (final Exception e) {
      LOG.debug("Exception while saving preferences", e);
    }
    try {
      applicationModel.getConnectionProvider().close();
    }
    catch (final Exception e) {
      LOG.debug("Exception while disconnecting from database", e);
    }
    final Window parent = getParentWindow();
    if (parent != null) {
      parent.dispose();
    }
    System.exit(0);
  }

  /**
   * Shows a help dialog
   * @see #getHelpPanel()
   */
  public final void displayHelp() {
    final JOptionPane pane = new JOptionPane(getHelpPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            resourceBundle.getString(HELP));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    Windows.resizeWindow(dialog, HELP_DIALOG_SCREEN_SIZE_RATIO, MINIMUM_HELP_WINDOW_SIZE);
    dialog.setLocationRelativeTo(this);
    dialog.setResizable(true);
    dialog.setModal(false);
    dialog.setVisible(true);
  }

  /**
   * Shows an about dialog
   * @see #getAboutPanel()
   */
  public final void displayAbout() {
    final JOptionPane pane = new JOptionPane(getAboutPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            resourceBundle.getString(ABOUT));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setModal(true);
    dialog.setVisible(true);
  }

  /**
   * @param listener a listener notified each time the always on top status changes
   */
  public final void addAlwaysOnTopListener(final EventDataListener<Boolean> listener) {
    alwaysOnTopState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAlwaysOnTopListener(final EventListener listener) {
    alwaysOnTopState.removeListener(listener);
  }

  /**
   * @param listener a listener notified when to application has been successfully started
   */
  public final void addApplicationStartedListener(final EventDataListener<JFrame> listener) {
    applicationStartedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeApplicationStartedListener(final EventDataListener<JFrame> listener) {
    applicationStartedEvent.removeDataListener(listener);
  }

  /**
   * @param entities the entities
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public static TreeModel getDependencyTreeModel(final Entities entities) {
    requireNonNull(entities);
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    for (final EntityDefinition definition : entities.getDefinitions()) {
      if (definition.getForeignKeys().isEmpty() || referencesOnlySelf(entities, definition.getEntityType())) {
        root.add(new EntityDependencyTreeNode(definition.getEntityType(), entities));
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
  protected final JTabbedPane getApplicationTabPane() {
    return applicationTabPane;
  }

  /**
   * @return the controls on which to base the main menu
   * @see #getFileControls()
   * @see #getSettingsControls()
   * @see #getViewControls()
   * @see #getToolsControls()
   * @see #getHelpControls()
   */
  protected Controls getMainMenuControls() {
    final Controls menuControls = Controls.controls();
    final Controls fileControls = getFileControls();
    if (fileControls != null && !fileControls.isEmpty()) {
      menuControls.add(fileControls);
    }
    final Controls viewControls = getViewControls();
    if (viewControls != null && !viewControls.isEmpty()) {
      menuControls.add(viewControls);
    }
    final Controls toolsControls = getToolsControls();
    if (toolsControls != null && !toolsControls.isEmpty()) {
      menuControls.add(toolsControls);
    }
    final Controls supportTableControls = getSupportTableControls();
    if (supportTableControls != null && !supportTableControls.isEmpty()) {
      menuControls.add(supportTableControls);
    }
    final List<Controls> additionalMenus = getAdditionalMenuControls();
    if (additionalMenus != null) {
      for (final Controls set : additionalMenus) {
        menuControls.add(set);
      }
    }
    final Controls helpControls = getHelpControls();
    if (helpControls != null && !helpControls.isEmpty()) {
      menuControls.add(helpControls);
    }

    return menuControls;
  }

  /**
   * @return the Controls specifying the items in the 'File' menu
   */
  protected Controls getFileControls() {
    return Controls.builder()
            .name(FrameworkMessages.get(FrameworkMessages.FILE))
            .mnemonic(FrameworkMessages.get(FrameworkMessages.FILE_MNEMONIC).charAt(0))
            .control(createExitControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Settings' menu
   */
  protected Controls getSettingsControls() {
    return Controls.builder()
            .name(FrameworkMessages.get(FrameworkMessages.SETTINGS))
            .control(createLogLevelControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Tools' menu
   */
  protected Controls getToolsControls() {
    return Controls.builder()
            .name(resourceBundle.getString("tools"))
            .mnemonic(resourceBundle.getString("tools_mnemonic").charAt(0))
            .control(getSettingsControls())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'View' menu
   */
  protected Controls getViewControls() {
    return Controls.builder()
            .name(FrameworkMessages.get(FrameworkMessages.VIEW))
            .mnemonic(FrameworkMessages.get(FrameworkMessages.VIEW_MNEMONIC).charAt(0))
            .control(createRefreshAllControl())
            .separator()
            .control(createViewApplicationTreeControl())
            .control(createViewDependencyTree())
            .control(createSelectLookAndFeelControl())
            .control(createSelectFontSizeControl())
            .separator()
            .control(createAlwaysOnTopControl())
            .build();
  }

  /**
   * @return the Controls specifying the items in the 'Help' menu
   */
  protected Controls getHelpControls() {
    return Controls.builder()
            .name(resourceBundle.getString(HELP))
            .mnemonic(resourceBundle.getString("help_mnemonic").charAt(0))
            .control(createHelpControl())
            .separator()
            .control(createAboutControl())
            .build();
  }

  /**
   * @return a Control for exiting the application
   */
  protected final Control createExitControl() {
    return Control.builder()
            .command(this::exit)
            .name(FrameworkMessages.get(FrameworkMessages.EXIT))
            .description(FrameworkMessages.get(FrameworkMessages.EXIT_TIP))
            .mnemonic(FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0))
            .build();
  }

  /**
   * @return a Control for setting the log level
   */
  protected final Control createLogLevelControl() {
    return Control.builder()
            .command(this::setLogLevel)
            .name(resourceBundle.getString(SET_LOG_LEVEL))
            .description(resourceBundle.getString(SET_LOG_LEVEL_DESC))
            .build();
  }

  /**
   * @return a Control for refreshing the application model
   */
  protected final Control createRefreshAllControl() {
    return Control.builder()
            .command(applicationModel::refresh)
            .name(FrameworkMessages.get(FrameworkMessages.REFRESH_ALL))
            .build();
  }

  /**
   * @return a Control for viewing the application structure tree
   */
  protected final Control createViewApplicationTreeControl() {
    return Control.builder()
            .command(this::viewApplicationTree)
            .name(resourceBundle.getString("view_application_tree"))
            .build();
  }

  /**
   * @return a Control for viewing the application dependency tree
   */
  protected final Control createViewDependencyTree() {
    return Control.builder()
            .command(this::viewDependencyTree)
            .name(FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES))
            .build();
  }

  /**
   * @return a Control for selecting the application look and feel
   */
  protected final Control createSelectLookAndFeelControl() {
    return Control.builder()
            .command(this::selectLookAndFeel)
            .name(resourceBundle.getString(SELECT_LOOK_AND_FEEL))
            .build();
  }

  /**
   * @return a Control for selecting the font size
   */
  protected final Control createSelectFontSizeControl() {
    return Control.builder()
            .command(this::selectFontSize)
            .name(resourceBundle.getString("select_font_size"))
            .build();
  }

  /**
   * @return a Control controlling the always on top status
   */
  protected final ToggleControl createAlwaysOnTopControl() {
    return ToggleControl.builder()
            .state(alwaysOnTopState)
            .name(resourceBundle.getString(ALWAYS_ON_TOP))
            .build();
  }

  /**
   * @return a Control for viewing information about the application
   */
  protected final Control createAboutControl() {
    return Control.builder()
            .command(this::displayAbout)
            .name(resourceBundle.getString(ABOUT) + "...")
            .build();
  }

  /**
   * @return a Control for displaying the help
   */
  protected final Control createHelpControl() {
    return Control.builder()
            .command(this::displayHelp)
            .name(resourceBundle.getString(HELP) + "...")
            .build();
  }

  /**
   * @return the panel shown when Help -&#62; Help is selected
   */
  protected JPanel getHelpPanel() {
    try {
      final JPanel panel = new JPanel(new BorderLayout());
      final String contents = getHelpText();
      final JTextArea text = new JTextArea(contents);
      final JScrollPane scrollPane = new JScrollPane(text);
      text.setEditable(false);
      text.setFocusable(false);
      text.setLineWrap(true);
      text.setWrapStyleWord(true);
      panel.add(scrollPane, BorderLayout.CENTER);

      return panel;
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the text to show in the help panel
   * @throws IOException in case of an IO exception
   */
  protected String getHelpText() throws IOException {
    return Text.getTextFileContents(EntityApplicationPanel.class, TIPS_AND_TRICKS_FILE);
  }

  /**
   * @return the panel shown when Help -&#62; About is selected
   */
  protected JPanel getAboutPanel() {
    final JPanel panel = new JPanel(Layouts.borderLayout());
    final String versionString = Version.getVersionAndMetadataString();
    panel.add(new JLabel(icons().logoTransparent()), BorderLayout.WEST);
    final JTextField versionMemoryField = new JTextField(versionString + " (" + Memory.getMemoryUsage() + ")");
    versionMemoryField.setEditable(false);
    versionMemoryField.setFocusable(false);
    panel.add(versionMemoryField, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Initializes the entity connection provider
   * @param user the user
   * @param clientTypeId a string specifying the client type
   * @return an initialized EntityConnectionProvider
   * @throws CancelException in case the initialization is cancelled
   */
  protected EntityConnectionProvider initializeConnectionProvider(final User user, final String clientTypeId) {
    return EntityConnectionProvider.connectionProvider()
            .setDomainClassName(EntityConnectionProvider.CLIENT_DOMAIN_CLASS.getOrThrow())
            .setClientTypeId(clientTypeId)
            .setClientVersion(getClientVersion())
            .setUser(user);
  }

  /**
   * @return the client version if specified, null by default
   */
  protected Version getClientVersion() {
    return null;
  }

  /**
   * Override to add event bindings after initialization
   */
  protected void bindEvents() {}

  /**
   * @return a List of Controls instances which should be added to the main menu bar
   */
  protected List<Controls> getAdditionalMenuControls() {
    return new ArrayList<>(0);
  }

  /**
   * @return the Controls on which to base the Support Tables menu
   */
  protected Controls getSupportTableControls() {
    if (supportPanelBuilders.isEmpty()) {
      return null;
    }

    final Comparator<String> comparator = Text.getSpaceAwareCollator();
    final Entities entities = applicationModel.getEntities();
    supportPanelBuilders.sort((ep1, ep2) -> {
      final String thisCompare = ep1.getCaption() == null ? entities.getDefinition(ep1.getEntityType()).getCaption() : ep1.getCaption();
      final String thatCompare = ep2.getCaption() == null ? entities.getDefinition(ep2.getEntityType()).getCaption() : ep2.getCaption();

      return comparator.compare(thisCompare, thatCompare);
    });
    final Controls controls = Controls.builder()
            .name(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES))
            .mnemonic(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0))
            .build();
    supportPanelBuilders.forEach(panelBuilder -> controls.add(Control.builder()
            .command(() -> displayEntityPanelDialog(panelBuilder))
            .name(panelBuilder.getCaption() == null ? entities.getDefinition(panelBuilder.getEntityType()).getCaption() : panelBuilder.getCaption())
            .build()));

    return controls;
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel builder
   * @param panelBuilder the entity panel builder
   */
  protected final void displayEntityPanelDialog(final EntityPanel.Builder panelBuilder) {
    displayEntityPanelDialog(panelBuilder, false);
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel builder
   * @param panelBuilder the entity panel builder
   * @param modalDialog if true the dialog is made modal
   */
  protected final void displayEntityPanelDialog(final EntityPanel.Builder panelBuilder, final boolean modalDialog) {
    requireNonNull(panelBuilder, "panelBuilder");
    try {
      Components.showWaitCursor(this);
      final EntityPanel entityPanel;
      if (PERSIST_ENTITY_PANELS.get() && persistentEntityPanels.containsKey(panelBuilder)) {
        entityPanel = persistentEntityPanels.get(panelBuilder);
        if (entityPanel.isShowing()) {
          return;
        }
      }
      else {
        entityPanel = panelBuilder.buildPanel(applicationModel.getConnectionProvider());
        entityPanel.initializePanel();
        if (PERSIST_ENTITY_PANELS.get()) {
          persistentEntityPanels.put(panelBuilder, entityPanel);
        }
      }
      final JDialog dialog = new JDialog(getParentWindow(), panelBuilder.getCaption() == null ?
              applicationModel.getEntities().getDefinition(panelBuilder.getEntityType()).getCaption() : panelBuilder.getCaption());
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(final WindowEvent e) {
          entityPanel.getModel().savePreferences();
          entityPanel.savePreferences();
        }
      });
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityPanel, BorderLayout.CENTER);
      KeyEvents.builder()
              .keyEvent(KeyEvent.VK_ESCAPE)
              .condition(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(Control.control(dialog::dispose))
              .enable(dialog.getRootPane());
      dialog.pack();
      dialog.setLocationRelativeTo(this);
      if (modalDialog) {
        dialog.setModal(true);
      }
      dialog.setResizable(true);
      SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }
    finally {
      Components.hideWaitCursor(this);
    }
  }

  /**
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = initializeApplicationTabPane();
    //base panel added for Look&Feel rendering
    final JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(applicationTabPane, BorderLayout.CENTER);
    add(basePanel, BorderLayout.CENTER);

    final JPanel northPanel = initializeNorthPanel();
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }

    final JPanel southPanel = initializeSouthPanel();
    if (southPanel != null) {
      add(southPanel, BorderLayout.SOUTH);
    }
  }

  /**
   * By default this method returns the panels defined by the available {@link EntityPanel.Builder}s.
   * @param applicationModel the application model responsible for providing EntityModels for the panels
   * @return a List containing the {@link EntityPanel}s to include in this application panel
   */
  protected abstract List<EntityPanel> initializeEntityPanels(final M applicationModel);

  protected List<EntityPanel.Builder> initializeSupportEntityPanelBuilders(final M applicationModel) {
    return Collections.emptyList();
  }

  /**
   * Returns the name of the default look and feel to use, this default implementation fetches
   * it from user preferences, if no preference is available the default system look and feel is used.
   * @return the look and feel name to use
   * @see Components#getSystemLookAndFeelClassName()
   */
  protected String getDefaultLookAndFeelName() {
    return UserPreferences.getUserPreference(applicationLookAndFeelProperty, Components.getSystemLookAndFeelClassName());
  }

  /**
   * @return the default font size multiplier
   * @see #selectFontSize()
   */
  protected Integer getDefaultFontSize() {
    return Integer.parseInt(UserPreferences.getUserPreference(applicationFontSizeProperty, "100"));
  }

  /**
   * Initializes a panel to display in the NORTH position of this application panel.
   * override to provide a north panel.
   * @return a panel for the NORTH position
   */
  protected JPanel initializeNorthPanel() {
    return null;
  }

  /**
   * Initializes a panel to display in the SOUTH position of this application frame,
   * override to provide a south panel.
   * @return a panel for the SOUTH position
   */
  protected JPanel initializeSouthPanel() {
    return null;
  }

  /**
   * Initializes the icon panel to show in the startup dialog
   * @param icon the icon
   * @return an initialized startup icon panel
   */
  protected JPanel initializeStartupIconPanel(final Icon icon) {
    final JPanel panel = new JPanel(new BorderLayout());
    if (icon != null) {
      panel.add(new JLabel(icon), BorderLayout.CENTER);
    }

    return panel;
  }

  /**
   * @return a frame title based on the application name and the logged in user
   */
  protected String getFrameTitle() {
    return (applicationName.isEmpty() ? "" : (applicationName + " - ")) + getUserInfo(getModel().getConnectionProvider());
  }

  /**
   * Initializes a JFrame according to the given parameters, containing this EntityApplicationPanel
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize if the JFrame is not maximized then its preferredSize is set to this value
   * @param mainMenu yes if the main menu should be included
   * @return an initialized, but non-visible JFrame
   */
  protected final JFrame prepareFrame(final boolean displayFrame, final boolean maximizeFrame, final Dimension frameSize,
                                      final boolean mainMenu) {
    final JFrame frame = frameProvider.get();
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    if (applicationIcon != null) {
      frame.setIconImage(applicationIcon.getImage());
    }
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        try {
          exit();
        }
        catch (final CancelException ignored) {/*ignored*/}
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
    Windows.centerWindow(frame);
    if (maximizeFrame) {
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    frame.setTitle(getFrameTitle());
    if (mainMenu) {
      frame.setJMenuBar(initializeMenuBar());
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
  protected final void addOnExitListener(final EventListener listener) {
    onExitEvent.addListener(listener);
  }

  /**
   * Initializes the JMenuBar to use on the application Frame
   * @return by default a JMenuBar based on the main menu controls
   * @see #getMainMenuControls()
   */
  protected JMenuBar initializeMenuBar() {
    return getMainMenuControls().createMenuBar();
  }

  /**
   * Initializes the application model
   * @param connectionProvider the db provider
   * @return an initialized application model
   * @throws CancelException in case the initialization is cancelled
   */
  protected abstract M initializeApplicationModel(EntityConnectionProvider connectionProvider);

  /**
   * Returns the user, either via a login dialog or via override, called during startup if login is required
   * @param defaultUser the default user to display in the login dialog
   * @param loginValidator the user login validator
   * @return the application user
   * @throws CancelException in case a login dialog is cancelled
   */
  protected User getLoginUser(final User defaultUser, final LoginValidator loginValidator) {
    final String loginDialogTitle = (!nullOrEmpty(applicationName) ? (applicationName + " - ") : "") + Messages.get(Messages.LOGIN);
    final User user = LoginPanel.builder()
            .defaultUser(defaultUser == null ? User.user(getDefaultUsername()) : defaultUser)
            .validator(loginValidator)
            .dialogTitle(loginDialogTitle)
            .icon(applicationIcon)
            .show();
    if (nullOrEmpty(user.getUsername())) {
      throw new IllegalArgumentException(FrameworkMessages.get(FrameworkMessages.EMPTY_USERNAME));
    }

    return user;
  }

  /**
   * Saves the username so that it can be used as default the next time this application is started.
   * @param username the username
   */
  protected void saveDefaultUsername(final String username) {
    UserPreferences.putUserPreference(applicationDefaultUsernameProperty, username);
  }

  /**
   * @return a default username previously saved to user preferences or the OS username
   */
  protected String getDefaultUsername() {
    return UserPreferences.getUserPreference(applicationDefaultUsernameProperty,
            EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name"));
  }

  /**
   * Returns a String identifying the application this EntityApplicationPanel represents,
   * by default the full class name is returned.
   * @return a String identifying the application type this panel represents
   */
  protected String getApplicationIdentifier() {
    return getClass().getName();
  }

  /**
   * Called during the exit() method, override to save user preferences on program exit,
   * remember to call super.savePreferences() when overriding
   * @see EntityApplicationModel#savePreferences()
   */
  protected void savePreferences() {
    getEntityPanels().forEach(EntityPanel::savePreferences);
    getModel().savePreferences();
  }

  final void startApplication(final User defaultUser, final User silentLoginUser, final boolean loginRequired,
                              final Dimension frameSize, final boolean maximizeFrame, final boolean displayFrame,
                              final boolean includeMainMenu, final boolean displayProgressDialog) {
    LOG.debug("{} application starting", applicationName);
    FrameworkMessages.class.getName();//hack to force-load the class, initializes UI caption constants
    Components.getLookAndFeelProvider(getDefaultLookAndFeelName()).ifPresent(LookAndFeelProvider::configure);
    final Integer fontSize = getDefaultFontSize();
    if (!Objects.equals(fontSize, 100)) {
      Components.setFontSize(fontSize / 100f);
    }

    final EntityConnectionProvider connectionProvider = createConnectionProvider(defaultUser, silentLoginUser, loginRequired);

    if (EntityApplicationModel.SAVE_DEFAULT_USERNAME.get()) {
      saveDefaultUsername(connectionProvider.getUser().getUsername());
    }

    startApplication(frameSize, maximizeFrame, displayFrame, includeMainMenu, displayProgressDialog, connectionProvider);
  }

  private void startApplication(final Dimension frameSize, final boolean maximizeFrame, final boolean displayFrame,
                                final boolean includeMainMenu, final boolean displayProgressDialog,
                                final EntityConnectionProvider connectionProvider) {
    final ApplicationStarter applicationStarter = new ApplicationStarter(connectionProvider);
    if (displayProgressDialog) {
      ProgressWorker.builder()
              .task(applicationStarter)
              .dialogTitle(applicationName)
              .westPanel(initializeStartupIconPanel(applicationIcon))
              .onSuccess(() -> applicationStartedEvent.onEvent(prepareFrame(displayFrame, maximizeFrame, frameSize, includeMainMenu)))
              .build()
              .execute();
    }
    else {
      applicationStarter.perform();
      applicationStartedEvent.onEvent(prepareFrame(displayFrame, maximizeFrame, frameSize, includeMainMenu));
    }
  }

  private EntityConnectionProvider createConnectionProvider(final User defaultUser, final User silentLoginUser,
                                                            final boolean loginRequired) {
    if (silentLoginUser == null && loginRequired) {
      final EntityLoginValidator loginValidator = new EntityLoginValidator();
      getLoginUser(defaultUser, loginValidator);

      return loginValidator.connectionProvider;
    }

    final EntityConnectionProvider connectionProvider = initializeConnectionProvider(silentLoginUser, getApplicationIdentifier());
    connectionProvider.getConnection();//throws exception if the server is not reachable

    return connectionProvider;
  }

  private JTabbedPane initializeApplicationTabPane() {
    final JTabbedPane tabbedPane = new JTabbedPane(TAB_PLACEMENT.get());
    tabbedPane.setFocusable(false);
    tabbedPane.addChangeListener(e -> ((EntityPanel) tabbedPane.getSelectedComponent()).initializePanel());
    for (final EntityPanel entityPanel : entityPanels) {
      tabbedPane.addTab(entityPanel.getCaption(), entityPanel);
      if (entityPanel.getEditPanel() != null) {
        entityPanel.getEditPanel().addActiveListener(panelActivated -> {
          if (panelActivated) {
            setSelectedChildPanel(entityPanel);
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
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> displayException(e, Windows.getParentWindow(EntityApplicationPanel.this)));
  }

  private void bindEventsInternal() {
    applicationModel.getConnectionValidObserver().addDataListener(active -> SwingUtilities.invokeLater(() ->
            setParentWindowTitle(active ? getFrameTitle() : getFrameTitle() + " - " + Messages.get(Messages.NOT_CONNECTED))));
    alwaysOnTopState.addDataListener(alwaysOnTop -> {
      final Window parent = getParentWindow();
      if (parent != null) {
        parent.setAlwaysOnTop(alwaysOnTop);
      }
    });
  }

  private void onLoginException(final Throwable throwable) {
    displayException(throwable, null);
    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
            resourceBundle.getString("retry"), resourceBundle.getString("retry_title"),
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
      throw new CancelException();
    }
  }

  private void initializePanel() {
    this.entityPanels.addAll(initializeEntityPanels(applicationModel));
    this.supportPanelBuilders.addAll(initializeSupportEntityPanelBuilders(applicationModel));
    initializeUI();
    bindEventsInternal();
    bindEvents();
  }

  private JScrollPane initializeApplicationTree() {
    return initializeTree(createApplicationTree(entityPanels));
  }

  private JScrollPane initializeDependencyTree() {
    return initializeTree(getDependencyTreeModel(applicationModel.getEntities()));
  }

  private void setParentWindowTitle(final String title) {
    final Window parentWindow = Windows.getParentWindow(this);
    if (parentWindow instanceof JFrame) {
      ((JFrame) parentWindow).setTitle(title);
    }
    else if (parentWindow instanceof JDialog) {
      ((JDialog) parentWindow).setTitle(title);
    }
  }

  private boolean cancelExit() {
    final boolean cancelForUnsavedData = getModel().isWarnAboutUnsavedData() && getModel().containsUnsavedData() &&
            JOptionPane.showConfirmDialog(this, FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING),
                    FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING_TITLE),
                    JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION;
    final boolean exitNotConfirmed = CONFIRM_EXIT.get() && JOptionPane.showConfirmDialog(this,
            FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT), FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT_TITLE),
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
  protected static User getUser(final String[] args) {
    try {
      final CredentialsProvider provider = CredentialsProvider.credentialsProvider();
      if (provider != null) {
        return provider.getCredentials(provider.getAuthenticationToken(args));
      }

      LOG.debug("No CredentialsProvider available");
      return null;
    }
    catch (final CredentialsException e) {
      LOG.debug("CredentialsService not reachable", e);
      return null;
    }
    catch (final IllegalArgumentException e) {
      LOG.debug("Invalid UUID authentication token");
      return null;
    }
  }

  private static JScrollPane initializeTree(final TreeModel treeModel) {
    final JTree tree = new JTree(treeModel);
    tree.setShowsRootHandles(true);
    tree.setToggleClickCount(1);
    tree.setRootVisible(false);
    Components.expandAll(tree, new TreePath(tree.getModel().getRoot()));

    return new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  private static DefaultTreeModel createApplicationTree(final Collection<? extends EntityPanel> entityPanels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

    return applicationTreeModel;
  }

  private static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends EntityPanel> panels) {
    for (final EntityPanel entityPanel : panels) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel.getCaption());
      root.add(node);
      if (!entityPanel.getChildPanels().isEmpty()) {
        addModelsToTree(node, (Collection<? extends EntityPanel>) entityPanel.getChildPanels());
      }
    }
  }

  private static String getUserInfo(final EntityConnectionProvider connectionProvider) {
    final String description = connectionProvider.getDescription();

    return getUsername(connectionProvider.getUser().getUsername().toUpperCase()) + (description != null ? "@" + description.toUpperCase() : "");
  }

  private static String getUsername(final String username) {
    final String usernamePrefix = EntityApplicationModel.USERNAME_PREFIX.get();
    if (!nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
      return username.substring(usernamePrefix.length());
    }

    return username;
  }

  private static boolean referencesOnlySelf(final Entities entities, final EntityType<?> entityType) {
    return entities.getDefinition(entityType).getForeignKeys().stream()
            .allMatch(foreignKey -> foreignKey.getReferencedEntityType().equals(entityType));
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

    private EntityDependencyTreeNode(final EntityType<?> entityType, final Entities entities) {
      super(requireNonNull(entityType, "entityType"));
      this.entities = entities;
    }

    /**
     * @return the type of the entity this node represents
     */
    public EntityType<?> getEntityType() {
      return (EntityType<?>) getUserObject();
    }

    @Override
    public void setParent(final MutableTreeNode newParent) {
      super.setParent(newParent);
      removeAllChildren();
      for (final EntityDependencyTreeNode child : initializeChildren()) {
        add(child);
      }
    }

    private List<EntityDependencyTreeNode> initializeChildren() {
      final List<EntityDependencyTreeNode> childrenList = new ArrayList<>();
      for (final EntityDefinition definition : entities.getDefinitions()) {
        for (final ForeignKeyProperty fkProperty : definition.getForeignKeyProperties()) {
          if (fkProperty.getReferencedEntityType().equals(getEntityType()) && !fkProperty.isSoftReference()
                  && !foreignKeyCycle(fkProperty.getReferencedEntityType())) {
            childrenList.add(new EntityDependencyTreeNode(definition.getEntityType(), entities));
          }
        }
      }

      return childrenList;
    }

    private boolean foreignKeyCycle(final EntityType<?> referencedEntityType) {
      TreeNode tmp = getParent();
      while (tmp instanceof EntityDependencyTreeNode) {
        if (((EntityDependencyTreeNode) tmp).getEntityType().equals(referencedEntityType)) {
          return true;
        }
        tmp = tmp.getParent();
      }

      return false;
    }
  }

  private final class EntityLoginValidator implements LoginValidator {

    private EntityConnectionProvider connectionProvider;

    private EntityLoginValidator() {
      this(null);
    }

    private EntityLoginValidator(final EntityConnectionProvider connectionProvider) {
      this.connectionProvider = connectionProvider;
    }

    @Override
    public void validate(final User user) throws Exception {
      if (connectionProvider == null) {
        connectionProvider = initializeConnectionProvider(user, getApplicationIdentifier());
      }
      else {
        connectionProvider.setUser(user);
      }
      connectionProvider.getConnection();//throws exception if the server is not reachable
    }
  }

  private final class ApplicationStarter implements Control.Command {

    private final EntityConnectionProvider connectionProvider;

    private ApplicationStarter(final EntityConnectionProvider connectionProvider) {
      this.connectionProvider = connectionProvider;
    }

    @Override
    public void perform() {
      try {
        final long initializationStarted = System.currentTimeMillis();
        applicationModel = initializeApplicationModel(connectionProvider);
        if (SwingUtilities.isEventDispatchThread()) {
          initializePanel();
        }
        else {
          SwingUtilities.invokeAndWait(EntityApplicationPanel.this::initializePanel);
        }
        applicationModel.getEntityModels().forEach(this::refreshComboBoxModels);
        LOG.info(getFrameTitle() + ", application started successfully: " + (System.currentTimeMillis() - initializationStarted) + " ms");
      }
      catch (final Throwable exception) {
        displayException(exception, null);
      }
    }

    private void refreshComboBoxModels(final SwingEntityModel model) {
      model.getEditModel().refreshForeignKeyComboBoxModels();
      model.getDetailModels().forEach(this::refreshComboBoxModels);
    }
  }
}
