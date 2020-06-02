/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.CredentialsProvider;
import is.codion.common.LoggerProxy;
import is.codion.common.Memory;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.event.Events;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.item.Items;
import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.common.value.PropertyValue;
import is.codion.common.version.Version;
import is.codion.common.version.Versions;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.EntityConnectionProviders;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityId;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.model.combobox.ItemComboBoxModel;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.HierarchyPanel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.LoginPanel;
import is.codion.swing.common.ui.UiManagerDefaults;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.ControlProvider;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import is.codion.swing.common.ui.dialog.DialogExceptionHandler;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.common.ui.images.Images;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.SwingEntityModelBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
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
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
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
  private static final String HELP = "help";
  private static final String ABOUT = "about";

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
   * @see EntityApplicationPanel#displayEntityPanelDialog(EntityPanelBuilder)
   */
  public static final PropertyValue<Boolean> PERSIST_ENTITY_PANELS = Configuration.booleanValue("codion.swing.persistEntityPanels", false);

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)<br>
   * Default value: JTabbedPane.TOP
   */
  public static final PropertyValue<Integer> TAB_PLACEMENT = Configuration.integerValue("codion.swing.tabPlacement", JTabbedPane.TOP);

  /**
   * Specifies whether an application frame should be maximized on startup.
   */
  public enum MaximizeFrame {
    /**
     * Frame should be maximized on startup.
     */
    YES,
    /**
     * Frame should not be maximized on startup.
     */
    NO
  }

  /**
   * Specifies whether an application frame should be displayed on startup.
   */
  public enum DisplayFrame {
    /**
     * Frame should be displayed on startup.
     */
    YES,
    /**
     * Frame should not be displayed on startup.
     */
    NO
  }

  /**
   * Specifies whether a application frame should include a main menu.
   * @see #getMainMenuControls()
   * @see #initializeMenuBar()
   */
  public enum MainMenu {
    /**
     * Main menu should be included.
     */
    YES,
    /**
     * Main menu should not be included.
     */
    NO
  }

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
  private final List<EntityPanelBuilder> entityPanelBuilders = new ArrayList<>();
  private final List<EntityPanelBuilder> supportPanelBuilders = new ArrayList<>();
  private final List<EntityPanel> entityPanels = new ArrayList<>();

  private M applicationModel;
  private JTabbedPane applicationTabPane;

  private final Event applicationStartedEvent = Events.event();
  private final Event<Boolean> alwaysOnTopChangedEvent = Events.event();
  private final Event onExitEvent = Events.event();

  private final Map<EntityPanelBuilder, EntityPanel> persistentEntityPanels = new HashMap<>();

  private boolean loginRequired = EntityApplicationModel.AUTHENTICATION_REQUIRED.get();
  private boolean showStartupDialog = SHOW_STARTUP_DIALOG.get();

  private String frameTitle = "<no title>";

  /**
   * A default constructor
   */
  public EntityApplicationPanel() {
    this(JFrame::new);
  }

  public EntityApplicationPanel(final Supplier<JFrame> frameProvider) {
    this.frameProvider = frameProvider;
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
   * Adds main application panels, displayed on application start
   * @param panelBuilders the main application panel providers
   */
  public final void addEntityPanelBuilders(final EntityPanelBuilder... panelBuilders) {
    requireNonNull(panelBuilders, "panelBuilders");
    for (final EntityPanelBuilder panelProvider : panelBuilders) {
      addEntityPanelBuilder(panelProvider);
    }
  }

  /**
   * Adds a main application panel, displayed on application start
   * @param panelBuilder the main application panel provider
   */
  public final void addEntityPanelBuilder(final EntityPanelBuilder panelBuilder) {
    entityPanelBuilders.add(panelBuilder);
  }

  /**
   * Adds support application panels, available via a support panel menu
   * @param panelBuilders the support application panel providers
   */
  public final void addSupportPanelBuilders(final EntityPanelBuilder... panelBuilders) {
    requireNonNull(panelBuilders, "panelBuilders");
    for (final EntityPanelBuilder panelProvider : panelBuilders) {
      addSupportPanelBuilder(panelProvider);
    }
  }

  /**
   * Adds a support application panel, available via a support panel menu
   * @param panelBuilder the support application panel provider
   */
  public final void addSupportPanelBuilder(final EntityPanelBuilder panelBuilder) {
    supportPanelBuilders.add(panelBuilder);
  }

  /**
   * @param entityId the entityId
   * @return the first entity panel found based on the given entity type, null if none is found
   */
  public final EntityPanel getEntityPanel(final EntityId entityId) {
    return entityPanels.stream().filter(entityPanel ->
            entityPanel.getModel().getEntityId().equals(entityId)).findFirst().orElse(null);
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
   * @return true if the frame this application panel is shown in should be 'alwaysOnTop'
   */
  public final boolean isAlwaysOnTop() {
    final Window parent = getParentWindow();
    return parent != null && parent.isAlwaysOnTop();
  }

  /**
   * @param alwaysOnTop the new value
   * @see #addAlwaysOnTopListener(EventDataListener)
   */
  public final void setAlwaysOnTop(final boolean alwaysOnTop) {
    final Window parent = getParentWindow();
    if (parent != null) {
      parent.setAlwaysOnTop(alwaysOnTop);
      alwaysOnTopChangedEvent.onEvent(alwaysOnTop);
    }
  }

  /**
   * Performs a login, fetching user information via {@link #getUser}
   * @throws CancelException in case the login is cancelled
   * @see #getUser(String, User, javax.swing.ImageIcon)
   */
  public final void login() {
    applicationModel.login(getUser(frameTitle, null, null));
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
    final LoggerProxy loggerProxy = LoggerProxy.createLoggerProxy();
    if (loggerProxy == null) {
      throw new RuntimeException("No LoggerProxy implementation available");
    }
    final DefaultComboBoxModel model = new DefaultComboBoxModel(loggerProxy.getLogLevels().toArray());
    model.setSelectedItem(loggerProxy.getLogLevel());
    JOptionPane.showMessageDialog(this, new JComboBox(model),
            resourceBundle.getString(SET_LOG_LEVEL), JOptionPane.QUESTION_MESSAGE);
    loggerProxy.setLogLevel(model.getSelectedItem());

  }

  /**
   * Displays in a dialog a tree describing the application layout
   */
  public final void viewApplicationTree() {
    Dialogs.displayInDialog(this, initializeApplicationTree(), resourceBundle.getString("view_application_tree"), Modal.NO);
  }

  /**
   * Shows a dialog containing a dependency tree view of all defined entities
   */
  public final void viewDependencyTree() {
    Dialogs.displayInDialog(this, initializeDependencyTree(), FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES), Modal.NO);
  }

  /**
   * Allows the user the select between the system and cross platform Look and Feel, activated on next appliation start
   */
  public final void selectLookAndFeel() {
    final JComboBox<String> lookAndFeelComboBox = new JComboBox<>();
    lookAndFeelComboBox.addItem(UIManager.getSystemLookAndFeelClassName());
    lookAndFeelComboBox.addItem(UIManager.getCrossPlatformLookAndFeelClassName());
    lookAndFeelComboBox.setSelectedItem(UIManager.getLookAndFeel().getClass().getName());

    final int option = JOptionPane.showOptionDialog(this, lookAndFeelComboBox,
            resourceBundle.getString(SELECT_LOOK_AND_FEEL), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (option == JOptionPane.OK_OPTION) {
      UserPreferences.putUserPreference(applicationLookAndFeelProperty, (String) lookAndFeelComboBox.getSelectedItem());
      JOptionPane.showMessageDialog(this, resourceBundle.getString("look_and_feel_selected_message"));
    }
  }

  /**
   * Display a dialog for selecting the application font size
   */
  public final void selectFontSize() {
    final List<Item<Integer>> values = new ArrayList<>(21);
    for (int i = 100; i <= 200; i += 5) {
      values.add(Items.item(i, i + "%"));
    }
    final ItemComboBoxModel<Integer> comboBoxModel = new ItemComboBoxModel<>(values);
    final Integer defaultFontSize = getDefaultFontSize();
    comboBoxModel.setSelectedItem(defaultFontSize);

    final JComboBox comboBox = new JComboBox(comboBoxModel);
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
      UserPreferences.putUserPreference(applicationFontSizeProperty, comboBoxModel.getSelectedItem().getValue().toString());
      JOptionPane.showMessageDialog(this, resourceBundle.getString("font_size_selected_message"));
    }
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize the frame size when not maximized
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final MaximizeFrame maximizeFrame,
                                       final Dimension frameSize) {
    return startApplication(frameCaption, iconName, maximizeFrame, frameSize, null);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final MaximizeFrame maximizeFrame,
                                       final Dimension frameSize, final User defaultUser) {
    return startApplication(frameCaption, iconName, maximizeFrame, frameSize, defaultUser, DisplayFrame.YES);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final MaximizeFrame maximizeFrame,
                                       final Dimension frameSize, final User defaultUser, final DisplayFrame displayFrame) {
    return startApplication(frameCaption, iconName, maximizeFrame, frameSize, defaultUser, displayFrame, null);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @param silentLoginUser if specified the application is started silently with that user, displaying no login or progress dialog
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final MaximizeFrame maximizeFrame,
                                       final Dimension frameSize, final User defaultUser, final DisplayFrame displayFrame,
                                       final User silentLoginUser) {
    try {
      return startApplicationInternal(frameCaption, iconName, maximizeFrame, frameSize, defaultUser, displayFrame, silentLoginUser);
    }
    catch (final CancelException e) {
      System.exit(0);
    }
    catch (final Exception e) {
      displayException(e, null);
      System.exit(1);
    }
    return null;
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
      applicationModel.getConnectionProvider().disconnect();
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
    alwaysOnTopChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAlwaysOnTopListener(final EventListener listener) {
    alwaysOnTopChangedEvent.removeListener(listener);
  }

  /**
   * @param listener a listener notified when to application has been successfully started
   */
  public final void addApplicationStartedListener(final EventListener listener) {
    applicationStartedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeApplicationStartedListener(final EventListener listener) {
    applicationStartedEvent.removeListener(listener);
  }

  /**
   * @return a tree model showing the dependencies between entities via foreign keys
   */
  public final TreeModel getDependencyTreeModel() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    final Entities entities = applicationModel.getEntities();
    for (final EntityDefinition definition : entities.getDefinitions()) {
      if (definition.getForeignKeyProperties().isEmpty() || referencesOnlySelf(applicationModel.getEntities(), definition.getEntityId())) {
        root.add(new EntityDependencyTreeNode(definition.getEntityId(), entities));
      }
    }

    return new DefaultTreeModel(root);
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
   * @return the control list on which to base the main menu
   * @see #getFileControls()
   * @see #getSettingsControls()
   * @see #getViewControls()
   * @see #getToolsControls()
   * @see #getHelpControls()
   */
  protected ControlList getMainMenuControls() {
    final ControlList menuControls = Controls.controlList();
    menuControls.add(getFileControls());
    menuControls.add(getViewControls());
    menuControls.add(getToolsControls());
    final ControlList supportTableControlList = getSupportTableControls();
    if (supportTableControlList != null) {
      menuControls.add(supportTableControlList);
    }
    final List<ControlList> additionalMenus = getAdditionalMenuControls();
    if (additionalMenus != null) {
      for (final ControlList set : additionalMenus) {
        menuControls.add(set);
      }
    }
    menuControls.add(getHelpControls());

    return menuControls;
  }

  /**
   * @return the ControlList specifying the items in the 'File' menu
   */
  protected ControlList getFileControls() {
    final ControlList file = Controls.controlList(FrameworkMessages.get(FrameworkMessages.FILE));
    file.setMnemonic(FrameworkMessages.get(FrameworkMessages.FILE_MNEMONIC).charAt(0));
    file.add(createExitControl());

    return file;
  }

  /**
   * @return the ControlList specifying the items in the 'Settings' menu
   */
  protected ControlList getSettingsControls() {
    return Controls.controlList(FrameworkMessages.get(FrameworkMessages.SETTINGS), createLogLevelControl());
  }

  /**
   * @return the ControlList specifying the items in the 'Tools' menu
   */
  protected ControlList getToolsControls() {
    return Controls.controlList(resourceBundle.getString("tools"), resourceBundle.getString("tools_mnemonic").charAt(0), getSettingsControls());
  }

  /**
   * @return the ControlList specifying the items in the 'View' menu
   */
  protected ControlList getViewControls() {
    final ControlList controls = Controls.controlList(FrameworkMessages.get(FrameworkMessages.VIEW),
            FrameworkMessages.get(FrameworkMessages.VIEW_MNEMONIC).charAt(0));
    controls.add(createRefreshAllControl());
    controls.addSeparator();
    controls.add(createViewApplicationTreeControl());
    controls.add(createViewDependencyTree());
    controls.add(createSelectLookAndFeelControl());
    controls.add(createSelectFontSizeControl());
    controls.addSeparator();
    controls.add(createAlwaysOnTopControl());

    return controls;
  }

  /**
   * @return the ControlList specifying the items in the 'Help' menu
   */
  protected ControlList getHelpControls() {
    final ControlList controls = Controls.controlList(resourceBundle.getString(HELP), resourceBundle.getString("help_mnemonic").charAt(0));
    controls.add(createHelpControl());
    controls.addSeparator();
    controls.add(createAboutControl());

    return controls;
  }

  /**
   * @return a Control for exiting the application
   */
  protected final Control createExitControl() {
    return Controls.control(this::exit, FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP),
            FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0));
  }

  /**
   * @return a Control for setting the log level
   */
  protected final Control createLogLevelControl() {
    final Control setLogLevel = Controls.control(this::setLogLevel,
            resourceBundle.getString(SET_LOG_LEVEL));
    setLogLevel.setDescription(resourceBundle.getString(SET_LOG_LEVEL_DESC));

    return setLogLevel;
  }

  /**
   * @return a Control for refreshing the application model
   */
  protected final Control createRefreshAllControl() {
    return Controls.control(applicationModel::refresh, FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
  }

  /**
   * @return a Control for viewing the application structure tree
   */
  protected final Control createViewApplicationTreeControl() {
    return Controls.control(this::viewApplicationTree, resourceBundle.getString("view_application_tree"));
  }

  /**
   * @return a Control for viewing the application dependency tree
   */
  protected final Control createViewDependencyTree() {
    return Controls.control(this::viewDependencyTree, FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES));
  }

  /**
   * @return a Control for selecting the application look and feel
   */
  protected final Control createSelectLookAndFeelControl() {
    return Controls.control(this::selectLookAndFeel, resourceBundle.getString(SELECT_LOOK_AND_FEEL));
  }

  /**
   * @return a Control for selecting the font size
   */
  protected final Control createSelectFontSizeControl() {
    return Controls.control(this::selectFontSize, resourceBundle.getString("select_font_size"));
  }

  /**
   * @return a Control controlling the always on top status
   */
  protected final ToggleControl createAlwaysOnTopControl() {
    return Controls.toggleControl(this,
            "alwaysOnTop", FrameworkMessages.get(FrameworkMessages.ALWAYS_ON_TOP), alwaysOnTopChangedEvent);
  }

  /**
   * @return a Control for viewing information about the application
   */
  protected final Control createAboutControl() {
    return Controls.control(this::displayAbout, resourceBundle.getString(ABOUT) + "...", null, null);
  }

  /**
   * @return a Control for displaying the help
   */
  protected final Control createHelpControl() {
    return Controls.control(this::displayHelp, resourceBundle.getString(HELP) + "...", null, null);
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
    final String versionString = Versions.getVersionAndBuildNumberString();
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
    return EntityConnectionProviders.connectionProvider().setDomainClassName(EntityConnectionProvider.CLIENT_DOMAIN_CLASS.get())
            .setClientTypeId(clientTypeId).setClientVersion(getClientVersion()).setUser(user);
  }

  /**
   * @return the client version if specified, null by default
   */
  protected Version getClientVersion() {
    return null;
  }

  /**
   * Initializes this application panel
   * @param applicationModel the application model
   * @throws IllegalStateException if the application model has not been set
   * @throws CancelException in case the initialization is cancelled
   */
  protected final void initialize(final M applicationModel) {
    requireNonNull(applicationModel, "applicationModel");
    this.applicationModel = applicationModel;
    clearEntityPanelBuilders();
    setupEntityPanelBuilders();
    this.entityPanels.addAll(initializeEntityPanels(applicationModel));
    initializeUI();
    bindEventsInternal();
    bindEvents();
  }

  /**
   * Override to add event bindings after initialization
   */
  protected void bindEvents() {}

  /**
   * @return a List of ControlList objects which are to be added to the main menu bar
   */
  protected List<ControlList> getAdditionalMenuControls() {
    return new ArrayList<>(0);
  }

  /**
   * @return the ControlList on which the Support Tables menu item is based on
   */
  protected ControlList getSupportTableControls() {
    if (supportPanelBuilders.isEmpty()) {
      return null;
    }

    final Comparator<String> comparator = Text.getSpaceAwareCollator();
    final Entities entities = applicationModel.getEntities();
    supportPanelBuilders.sort((ep1, ep2) -> {
      final String thisCompare = ep1.getCaption() == null ? entities.getDefinition(ep1.getEntityId()).getCaption() : ep1.getCaption();
      final String thatCompare = ep2.getCaption() == null ? entities.getDefinition(ep2.getEntityId()).getCaption() : ep2.getCaption();

      return comparator.compare(thisCompare, thatCompare);
    });
    final ControlList controls = Controls.controlList(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES),
            FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0));
    supportPanelBuilders.forEach(panelProvider -> controls.add(Controls.control(() -> displayEntityPanelDialog(panelProvider),
            panelProvider.getCaption() == null ?
                    entities.getDefinition(panelProvider.getEntityId()).getCaption() : panelProvider.getCaption())));

    return controls;
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel provider
   * @param panelProvider the entity panel provider
   */
  protected final void displayEntityPanelDialog(final EntityPanelBuilder panelProvider) {
    displayEntityPanelDialog(panelProvider, false);
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel provider
   * @param panelProvider the entity panel provider
   * @param modalDialog if true the dialog is made modal
   */
  protected final void displayEntityPanelDialog(final EntityPanelBuilder panelProvider, final boolean modalDialog) {
    try {
      Components.showWaitCursor(this);
      final EntityPanel entityPanel;
      if (PERSIST_ENTITY_PANELS.get() && persistentEntityPanels.containsKey(panelProvider)) {
        entityPanel = persistentEntityPanels.get(panelProvider);
        if (entityPanel.isShowing()) {
          return;
        }
      }
      else {
        entityPanel = panelProvider.createPanel(applicationModel.getConnectionProvider());
        entityPanel.initializePanel();
        if (PERSIST_ENTITY_PANELS.get()) {
          persistentEntityPanels.put(panelProvider, entityPanel);
        }
      }
      final JDialog dialog = new JDialog(getParentWindow(), panelProvider.getCaption() == null ?
              applicationModel.getEntities().getDefinition(panelProvider.getEntityId()).getCaption() : panelProvider.getCaption());
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
      KeyEvents.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, Controls.control(dialog::dispose));
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
   * Called during initialization, after the application model has been initialized,
   * override to keep all entity panel provider definitions in one place.
   * @see #addEntityPanelBuilder(EntityPanelBuilder)
   * @see #addEntityPanelBuilders(EntityPanelBuilder...)
   * @see #addSupportPanelBuilder(EntityPanelBuilder)
   * @see #addSupportPanelBuilders(EntityPanelBuilder...)
   */
  protected void setupEntityPanelBuilders() {}

  /**
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = initializeApplicationTabPane();
    add(applicationTabPane, BorderLayout.CENTER);

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
   * By default this method returns the panels defined by the available {@link EntityPanelBuilder}s.
   * @param applicationModel the application model responsible for providing EntityModels for the panels
   * @return a List containing the {@link EntityPanel}s to include in this application panel
   * @see #addEntityPanelBuilder(EntityPanelBuilder)
   */
  protected List<EntityPanel> initializeEntityPanels(final M applicationModel) {
    final List<EntityPanel> panels = new ArrayList<>();
    for (final EntityPanelBuilder panelBuilder : entityPanelBuilders) {
      final SwingEntityModel entityModel = initializeEntityModel(applicationModel, panelBuilder.getModelBuilder());
      final EntityPanel entityPanel;
      if (applicationModel.containsEntityModel(panelBuilder.getEntityId())) {
        entityPanel = panelBuilder.createPanel(entityModel);
      }
      else {
        entityPanel = panelBuilder.createPanel(applicationModel.getConnectionProvider());
        applicationModel.addEntityModel(entityPanel.getModel());
      }
      panels.add(entityPanel);
    }

    return panels;
  }

  /**
   * Initializes a {@link SwingEntityModel}, according to the given {@link SwingEntityModelBuilder},
   * either by fetching it from the application model or creating one and adding it to the application model.
   * @param applicationModel the application model
   * @param modelBuilder the model builder
   * @return an initialized {@link SwingEntityModel} instance.
   */
  protected final SwingEntityModel initializeEntityModel(final M applicationModel, final SwingEntityModelBuilder modelBuilder) {
    final SwingEntityModel entityModel;
    final Class<? extends SwingEntityModel> modelClass = modelBuilder.getModelClass();
    if (!modelClass.equals(SwingEntityModel.class)) {
      if (applicationModel.containsEntityModel(modelClass)) {
        entityModel = applicationModel.getEntityModel(modelClass);
      }
      else {
        entityModel = modelBuilder.createModel(applicationModel.getConnectionProvider());
        applicationModel.addEntityModel(entityModel);
      }
    }
    else {
      if (applicationModel.containsEntityModel(modelBuilder.getEntityId())) {
        entityModel = applicationModel.getEntityModel(modelBuilder.getEntityId());
      }
      else {
        entityModel = modelBuilder.createModel(applicationModel.getConnectionProvider());
        applicationModel.addEntityModel(entityModel);
      }
    }

    return entityModel;
  }

  /**
   * @return true if a login dialog is required for this application,
   * false if the user is supplied differently
   */
  protected final boolean isLoginRequired() {
    return loginRequired;
  }

  /**
   * Sets whether or not this application requires a login dialog, setting this value
   * after the application has been started has no effect
   * @param loginRequired the login required status
   */
  protected final void setLoginRequired(final boolean loginRequired) {
    this.loginRequired = loginRequired;
  }

  /**
   * @return true if a startup dialog should be shown
   */
  protected final boolean isShowStartupDialog() {
    return showStartupDialog;
  }

  /**
   * @param startupDialog true if a startup dialog should be shown
   */
  protected final void setShowStartupDialog(final boolean startupDialog) {
    this.showStartupDialog = startupDialog;
  }

  /**
   * @return the look and feel class name to use
   * @see Components#getDefaultLookAndFeelClassName()
   */
  protected String getDefaultLookAndFeelClassName() {
    return UserPreferences.getUserPreference(applicationLookAndFeelProperty, Components.getDefaultLookAndFeelClassName());
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
   * Initializes the startup dialog
   * @param icon the icon
   * @param startupMessage the startup message
   * @return the startup dialog
   * @see #initializeStartupProgressPanel(javax.swing.Icon)
   */
  protected final JDialog initializeStartupDialog(final Icon icon, final String startupMessage) {
    final String message = startupMessage == null ? "Initializing Application" : startupMessage;
    final JDialog initializationDialog = new JDialog((JFrame) null, message, false);
    initializationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    initializationDialog.getContentPane().add(initializeStartupProgressPanel(icon), BorderLayout.CENTER);
    initializationDialog.pack();
    Windows.centerWindow(initializationDialog);

    return initializationDialog;
  }

  /**
   * Initializes the progress panel to show in the startup dialog
   * @param icon the icon
   * @return an initialized startup progress panel
   */
  protected JPanel initializeStartupProgressPanel(final Icon icon) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    progressBar.setIndeterminate(true);
    panel.add(progressBar, BorderLayout.CENTER);
    if (icon != null) {
      final JLabel iconLabel = new JLabel(icon);
      iconLabel.setBorder(BorderFactory.createRaisedBevelBorder());
      panel.add(iconLabel, BorderLayout.WEST);
    }

    return panel;
  }

  /**
   * @param frameCaption the caption for the frame
   * @param connectionProvider the EntityConnectionProvider this application is using
   * @return a frame title based on the logged in user
   */
  protected String getFrameTitle(final String frameCaption, final EntityConnectionProvider connectionProvider) {
    return frameCaption + " - " + getUserInfo(connectionProvider);
  }

  /**
   * Initializes a JFrame according to the given parameters, containing this EntityApplicationPanel
   * @param title the title string for the JFrame
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @param mainMenu yes if the main menu should be included
   * @param size if the JFrame is not maximized then its preferredSize is set to this value
   * @param applicationIcon the application icon
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @return an initialized, but non-visible JFrame
   */
  protected final JFrame prepareFrame(final String title, final MaximizeFrame maximizeFrame,
                                      final MainMenu mainMenu, final Dimension size,
                                      final ImageIcon applicationIcon, final DisplayFrame displayFrame) {
    final JFrame frame = frameProvider.get();
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setIconImage(applicationIcon.getImage());
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
    if (size != null) {
      frame.setSize(size);
    }
    else {
      frame.pack();
      Windows.setSizeWithinScreenBounds(frame);
    }
    Windows.centerWindow(frame);
    if (maximizeFrame == MaximizeFrame.YES) {
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    frame.setTitle(title);
    if (mainMenu == MainMenu.YES) {
      frame.setJMenuBar(initializeMenuBar());
    }
    if (displayFrame == DisplayFrame.YES) {
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
   * @return by default a JMenuBar based on the main menu control list
   * @see #getMainMenuControls()
   */
  protected JMenuBar initializeMenuBar() {
    return ControlProvider.createMenuBar(getMainMenuControls());
  }

  /**
   * Initializes the application model
   * @param connectionProvider the db provider
   * @return an initialized application model
   * @throws CancelException in case the initialization is cancelled
   */
  protected abstract M initializeApplicationModel(EntityConnectionProvider connectionProvider);

  /**
   * Returns the user, either via a login dialog or via override, called during startup
   * @param frameCaption the application frame caption
   * @param defaultUser the default user
   * @param applicationIcon the application icon
   * @return the application user
   * @throws CancelException in case a login dialog is cancelled
   */
  protected User getUser(final String frameCaption, final User defaultUser, final ImageIcon applicationIcon) {
    final LoginPanel loginPanel = new LoginPanel(defaultUser == null ? Users.user(getDefaultUsername()) : defaultUser);
    final String loginTitle = (!nullOrEmpty(frameCaption) ? (frameCaption + " - ") : "") + Messages.get(Messages.LOGIN);
    final User user = loginPanel.showLoginPanel(null, loginTitle, applicationIcon);
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

  private void clearEntityPanelBuilders() {
    entityPanelBuilders.clear();
    supportPanelBuilders.clear();
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
    applicationModel.getConnectionValidObserver().addDataListener(active ->
            setParentWindowTitle(active ? frameTitle : frameTitle + " - " + Messages.get(Messages.NOT_CONNECTED)));
  }

  private JFrame startApplicationInternal(final String frameCaption, final String iconName, final MaximizeFrame maximizeFrame,
                                          final Dimension frameSize, final User defaultUser, final DisplayFrame displayFrame,
                                          final User silentLoginUser) throws Exception {
    LOG.debug("{} application starting", frameCaption);
    FrameworkMessages.class.getName();//hack to force-load the class, initializes UI caption constants
    final String defaultLookAndFeelClassName = getDefaultLookAndFeelClassName();
    if (!UIManager.getLookAndFeel().getClass().getName().equals(defaultLookAndFeelClassName)) {
      UIManager.setLookAndFeel(defaultLookAndFeelClassName);
    }
    final Integer fontSize = getDefaultFontSize();
    if (!fontSize.equals(100)) {
      Components.setFontSize(fontSize / 100f);
    }
    final ImageIcon applicationIcon = iconName != null ? Images.loadIcon(getClass(), iconName) : icons().logoTransparent();
    final JDialog startupDialog = showStartupDialog ? initializeStartupDialog(applicationIcon, frameCaption) : null;
    while (true) {
      final User user = silentLoginUser != null ? silentLoginUser : loginRequired ? getUser(frameCaption, defaultUser, applicationIcon) : Users.user("");
      if (startupDialog != null) {
        startupDialog.setVisible(true);
      }
      EntityConnectionProvider connectionProvider = null;
      try {
        connectionProvider = initializeConnectionProvider(user, getApplicationIdentifier());
        connectionProvider.getConnection();//throws exception if the server is not reachable
        final long initializationStarted = System.currentTimeMillis();
        initialize(initializeApplicationModel(connectionProvider));

        if (startupDialog != null) {
          startupDialog.dispose();
        }
        if (EntityApplicationModel.SAVE_DEFAULT_USERNAME.get()) {
          saveDefaultUsername(connectionProvider.getUser().getUsername());
        }
        this.frameTitle = getFrameTitle(frameCaption, connectionProvider);
        final JFrame frame = prepareFrame(this.frameTitle, maximizeFrame, MainMenu.YES, frameSize, applicationIcon, displayFrame);
        this.applicationStartedEvent.onEvent();
        LOG.info(this.frameTitle + ", application started successfully, " + connectionProvider.getUser().getUsername()
                + ": " + (System.currentTimeMillis() - initializationStarted) + " ms");
        return frame;
      }
      catch (final Throwable exception) {
        onStartupException(startupDialog, connectionProvider, exception);
      }
    }
  }

  private void onStartupException(final JDialog startupDialog, final EntityConnectionProvider connectionProvider, final Throwable e) {
    try {
      if (connectionProvider != null) {
        connectionProvider.disconnect();
      }
    }
    catch (final Exception ex) {
      LOG.debug("Exception while disconnecting after a failed startup", ex);
    }
    //todo EDT mess
    displayException(e, null);
    if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
            resourceBundle.getString("retry"), resourceBundle.getString("retry_title"),
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
      if (startupDialog != null) {
        startupDialog.dispose();
      }
      throw new CancelException();
    }
  }

  private JScrollPane initializeApplicationTree() {
    return initializeTree(createApplicationTree(entityPanels));
  }

  private JScrollPane initializeDependencyTree() {
    return initializeTree(getDependencyTreeModel());
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
   * Looks up user credentials via a {@link is.codion.common.CredentialsProvider} service using an authentication token
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

  private static DefaultTreeModel createApplicationTree(final Collection<? extends HierarchyPanel> entityPanels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

    return applicationTreeModel;
  }

  private static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends HierarchyPanel> panels) {
    for (final HierarchyPanel entityPanel : panels) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel);
      root.add(node);
      if (!entityPanel.getChildPanels().isEmpty()) {
        addModelsToTree(node, entityPanel.getChildPanels());
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

  private static boolean referencesOnlySelf(final Entities entities, final EntityId entityId) {
    return entities.getDefinition(entityId).getForeignKeyProperties().stream()
            .allMatch(fkProperty -> fkProperty.getForeignEntityId().equals(entityId));
  }

  private static final class EntityDependencyTreeNode extends DefaultMutableTreeNode {

    private final Entities entities;

    private EntityDependencyTreeNode(final EntityId entityId, final Entities entities) {
      super(requireNonNull(entityId, "entityId"));
      this.entities = entities;
    }

    /**
     * @return the id of the entity this node represents
     */
    public EntityId getEntityId() {
      return (EntityId) getUserObject();
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
          if (fkProperty.getForeignEntityId().equals(getEntityId()) && !fkProperty.isSoftReference()
                  && !foreignKeyCycle(fkProperty.getForeignEntityId())) {
            childrenList.add(new EntityDependencyTreeNode(definition.getEntityId(), entities));
          }
        }
      }

      return childrenList;
    }

    private boolean foreignKeyCycle(final EntityId referencedEntityId) {
      TreeNode tmp = getParent();
      while (tmp instanceof EntityDependencyTreeNode) {
        if (((EntityDependencyTreeNode) tmp).getEntityId().equals(referencedEntityId)) {
          return true;
        }
        tmp = tmp.getParent();
      }

      return false;
    }
  }
}
