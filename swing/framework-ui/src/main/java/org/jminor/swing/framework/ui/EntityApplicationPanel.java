/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.CredentialsProvider;
import org.jminor.common.Event;
import org.jminor.common.EventDataListener;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.Item;
import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.Util;
import org.jminor.common.Value;
import org.jminor.common.Version;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.PreferencesUtil;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.swing.common.model.combobox.ItemComboBoxModel;
import org.jminor.swing.common.ui.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.DialogExceptionHandler;
import org.jminor.swing.common.ui.LoginPanel;
import org.jminor.swing.common.ui.MasterDetailPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.ServiceLoader;

/**
 * A central application panel class.
 * @param <M> the application model type
 */
public abstract class EntityApplicationPanel<M extends SwingEntityApplicationModel>
        extends JPanel implements DialogExceptionHandler, MasterDetailPanel {

  /** Non-static so that Locale.setDefault(...) can be called in the main method of a subclass */
  private final ResourceBundle resourceBundle = ResourceBundle.getBundle(EntityApplicationPanel.class.getName(), Locale.getDefault());

  private static final String MSG_SELECT_LOOK_AND_FEEL = "select_look_and_feel";
  private static final String MSG_HELP = "help";
  private static final String MSG_ABOUT = "about";

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

  /**
   * Indicates whether the application should ask for confirmation when exiting<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final Value<Boolean> CONFIRM_EXIT = Configuration.booleanValue("jminor.swing.confirmExit", false);

  /**
   * Specifies whether a startup dialog should be shown<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final Value<Boolean> SHOW_STARTUP_DIALOG = Configuration.booleanValue("jminor.swing.showStartupDialog", true);

  /**
   * Specifies if EntityPanels opened via the {@code EntityApplicationPanel.showEntityPanelDialog} method
   * should be persisted, or kept in memory, when the dialog is closed.<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see EntityApplicationPanel#showEntityPanelDialog(EntityPanelProvider)
   */
  public static final Value<Boolean> PERSIST_ENTITY_PANELS = Configuration.booleanValue("jminor.swing.persistEntityPanels", false);

  /**
   * Specifies the tab placement<br>
   * Value type: Integer (JTabbedPane.TOP, JTabbedPane.BOTTOM, JTabbedPane.LEFT, JTabbedPane.RIGHT)<br>
   * Default value: JTabbedPane.TOP
   */
  public static final Value<Integer> TAB_PLACEMENT = Configuration.integerValue("jminor.swing.tabPlacement", JTabbedPane.TOP);

  private static final String LOOK_AND_FEEL_PROPERTY = "org.jminor.swing.framework.ui.LookAndFeel";
  private static final String FONT_SIZE_PROPERTY = "org.jminor.swing.framework.ui.FontSize";
  private static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";
  private static final Dimension MINIMUM_HELP_WINDOW_SIZE = new Dimension(600, 750);
  private static final double HELP_DIALOG_SCREEN_SIZE_RATIO = 0.1;

  private final String applicationLookAndFeelProperty;
  private final String applicationFontSizeProperty;

  private final List<EntityPanelProvider> entityPanelProviders = new ArrayList<>();
  private final List<EntityPanelProvider> supportPanelProviders = new ArrayList<>();
  private final List<EntityPanel> entityPanels = new ArrayList<>();

  private M applicationModel;
  private JTabbedPane applicationTabPane;

  private final Event applicationStartedEvent = Events.event();
  private final Event<Boolean> alwaysOnTopChangedEvent = Events.event();
  private final Event onExitEvent = Events.event();

  private final Map<EntityPanelProvider, EntityPanel> persistentEntityPanels = new HashMap<>();

  private boolean loginRequired = EntityApplicationModel.AUTHENTICATION_REQUIRED.get();
  private boolean showStartupDialog = SHOW_STARTUP_DIALOG.get();

  private String frameTitle = "<no title>";

  /**
   * A default constructor
   */
  public EntityApplicationPanel() {
    this.applicationLookAndFeelProperty = LOOK_AND_FEEL_PROPERTY + "#" + getClass().getSimpleName();
    this.applicationFontSizeProperty = FONT_SIZE_PROPERTY + "#" + getClass().getSimpleName();
    setUncaughtExceptionHandler();
  }

  /**
   * Handles the given exception, which simply means displaying it to the user
   * @param throwable the exception to handle
   */
  public final void handleException(final Throwable throwable) {
    handleException(throwable, UiUtil.getParentWindow(this));
  }

  /** {@inheritDoc} */
  @Override
  public final void handleException(final Throwable exception, final Window dialogParent) {
    LOG.error(exception.getMessage(), exception);
    DefaultDialogExceptionHandler.getInstance().handleException(exception, dialogParent);
  }

  /**
   * @return the application model this application panel is based on
   */
  public final M getModel() {
    return applicationModel;
  }

  /**
   * Adds main application panels, displayed on application start
   * @param panelProviders the main application panel providers
   * @return this application panel instance
   */
  public final EntityApplicationPanel<M> addEntityPanelProviders(final EntityPanelProvider... panelProviders) {
    Objects.requireNonNull(panelProviders, "panelProviders");
    for (final EntityPanelProvider panelProvider : panelProviders) {
      addEntityPanelProvider(panelProvider);
    }
    return this;
  }

  /**
   * Adds a main application panel, displayed on application start
   * @param panelProvider the main application panel provider
   * @return this application panel instance
   */
  public final EntityApplicationPanel<M> addEntityPanelProvider(final EntityPanelProvider panelProvider) {
    entityPanelProviders.add(panelProvider);
    return this;
  }

  /**
   * Adds support application panels, available via a support panel menu
   * @param panelProviders the support application panel providers
   * @return this application panel instance
   */
  public final EntityApplicationPanel<M> addSupportPanelProviders(final EntityPanelProvider... panelProviders) {
    Objects.requireNonNull(panelProviders, "panelProviders");
    for (final EntityPanelProvider panelProvider : panelProviders) {
      addSupportPanelProvider(panelProvider);
    }
    return this;
  }

  /**
   * Adds a support application panel, available via a support panel menu
   * @param panelProvider the support application panel provider
   * @return this application panel instance
   */
  public final EntityApplicationPanel<M> addSupportPanelProvider(final EntityPanelProvider panelProvider) {
    supportPanelProviders.add(panelProvider);
    return this;
  }

  /**
   * @param entityId the entity ID
   * @return the first entity panel found based on the given entity type, null if none is found
   */
  public final EntityPanel getEntityPanel(final String entityId) {
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
    return UiUtil.getParentWindow(this);
  }

  /**
   * @return true if the frame this application panel is shown in should be 'alwaysOnTop'
   */
  public final boolean isAlwaysOnTop() {
    final Window parent = getParentWindow();
    return parent != null && parent.isAlwaysOnTop();
  }

  /**
   * fires: evtAlwaysOnTopChanged
   * @param value the new value
   */
  public final void setAlwaysOnTop(final boolean value) {
    final Window parent = getParentWindow();
    if (parent != null) {
      parent.setAlwaysOnTop(value);
      alwaysOnTopChangedEvent.fire(value);
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
   * Shows a dialog for setting the logging level
   */
  public final void setLoggingLevel() {
    EntityUiUtil.setLoggingLevel(this);
  }

  /**
   * Displays in a dialog a tree describing the application layout
   */
  public final void viewApplicationTree() {
    UiUtil.displayInDialog(this, initializeApplicationTree(), resourceBundle.getString("view_application_tree"), false);
  }

  /**
   * Shows a dialog containing a dependency tree view of all defined entities
   */
  public final void viewDependencyTree() {
    UiUtil.displayInDialog(this, initializeDependencyTree(), FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES), false);
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
            resourceBundle.getString(MSG_SELECT_LOOK_AND_FEEL), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (option == JOptionPane.OK_OPTION) {
      PreferencesUtil.putUserPreference(applicationLookAndFeelProperty, (String) lookAndFeelComboBox.getSelectedItem());
      JOptionPane.showMessageDialog(this, resourceBundle.getString("look_and_feel_selected_message"));
    }
  }

  /**
   * Display a dialog for selecting the application font size
   */
  public final void selectFontSize() {
    final List<Item<Integer>> values = new ArrayList<>(21);
    for (int i = 100; i <= 200; i += 5) {
      values.add(new Item<>(i, i + "%"));
    }
    final ItemComboBoxModel<Integer> comboBoxModel = new ItemComboBoxModel<>(values);
    final Integer defaultFontSize = getDefaultFontSize();
    comboBoxModel.setSelectedItem(defaultFontSize);

    final JComboBox comboBox = new JComboBox(comboBoxModel);
    comboBox.setRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                                                    final boolean isSelected, final boolean cellHasFocus) {
        final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (index >= 0) {
          final Font font = component.getFont();
          final int newSize = Math.round(font.getSize() * (values.get(index).getItem() / (float) defaultFontSize.doubleValue()));
          component.setFont(new Font(font.getName(), font.getStyle(), newSize));
        }

        return component;
      }
    });

    final int option = JOptionPane.showOptionDialog(this, comboBox,
            resourceBundle.getString("select_font_size"), JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, null, null);
    if (option == JOptionPane.OK_OPTION) {
      PreferencesUtil.putUserPreference(applicationFontSizeProperty, comboBoxModel.getSelectedItem().getItem().toString());
      JOptionPane.showMessageDialog(this, resourceBundle.getString("font_size_selected_message"));
    }
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximize if true the application frame is maximized on startup
   * @param frameSize the frame size when not maximized
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final boolean maximize,
                                       final Dimension frameSize) {
    return startApplication(frameCaption, iconName, maximize, frameSize, null);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximize if true the application frame is maximized on startup
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final boolean maximize,
                                       final Dimension frameSize, final User defaultUser) {
    return startApplication(frameCaption, iconName, maximize, frameSize, defaultUser, true);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximize if true the application frame is maximized on startup
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @param showFrame if true the frame is set visible
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final boolean maximize,
                                       final Dimension frameSize, final User defaultUser, final boolean showFrame) {
    return startApplication(frameCaption, iconName, maximize, frameSize, defaultUser, showFrame, null);
  }

  /**
   * Starts this application.
   * @param frameCaption the caption to display on the frame
   * @param iconName the name of the icon to use
   * @param maximize if true the application frame is maximized on startup
   * @param frameSize the frame size when not maximized
   * @param defaultUser the default user to display in the login dialog
   * @param showFrame if true the frame is set visible
   * @param silentLoginUser if specified the application is started silently with that user, displaying no login or progress dialog
   * @return the JFrame instance containing this application panel
   */
  public final JFrame startApplication(final String frameCaption, final String iconName, final boolean maximize,
                                       final Dimension frameSize, final User defaultUser, final boolean showFrame,
                                       final User silentLoginUser) {
    try {
      return startApplicationInternal(frameCaption, iconName, maximize, frameSize, defaultUser, showFrame, silentLoginUser);
    }
    catch (final CancelException e) {
      System.exit(0);
    }
    catch (final Exception e) {
      handleException(e);
      System.exit(1);
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getMasterPanel() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final EntityPanel getActiveDetailPanel() {
    return getEntityPanels().get(0);
  }

  /** {@inheritDoc} */
  @Override
  public final void setActiveDetailPanel(final MasterDetailPanel detailPanel) {
    if (applicationTabPane != null) {//initializeUI() may have been overridden
      applicationTabPane.setSelectedComponent((JComponent) detailPanel);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getPreviousPanel() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final MasterDetailPanel getNextPanel() {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public final List<MasterDetailPanel> getDetailPanels() {
    return Collections.unmodifiableList(entityPanels);
  }

  /** {@inheritDoc} */
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
      onExitEvent.fire();
    }
    catch (final CancelException e) {
      throw e;
    }
    catch (final Exception e) {
      LOG.debug("Exception while exiting", e);
    }
    try {
      savePreferences();
      PreferencesUtil.flushUserPreferences();
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
  public final void showHelp() {
    final JOptionPane pane = new JOptionPane(getHelpPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            resourceBundle.getString(MSG_HELP));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    UiUtil.resizeWindow(dialog, HELP_DIALOG_SCREEN_SIZE_RATIO, MINIMUM_HELP_WINDOW_SIZE);
    dialog.setLocationRelativeTo(this);
    dialog.setResizable(true);
    dialog.setModal(false);
    dialog.setVisible(true);
  }

  /**
   * Shows an about dialog
   * @see #getAboutPanel()
   */
  public final void showAbout() {
    final JOptionPane pane = new JOptionPane(getAboutPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.DEFAULT_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            resourceBundle.getString(MSG_ABOUT));
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
  public TreeModel getDependencyTreeModel() {
    final DefaultMutableTreeNode root = new DefaultMutableTreeNode(null);
    final Entities domain = applicationModel.getDomain();
    for (final String entityId : domain.getDefinedEntities()) {
      if (domain.getForeignKeyProperties(entityId).isEmpty() || referencesOnlySelf(entityId)) {
        root.add(new EntityDependencyTreeNode(domain.getDomainId(), entityId, domain));
      }
    }

    return new DefaultTreeModel(root);
  }

  private boolean referencesOnlySelf(final String entityId) {
    return applicationModel.getDomain().getForeignKeyProperties(entityId).stream()
            .allMatch(fkProperty -> fkProperty.getForeignEntityId().equals(entityId));
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
   * @return the control set on which to base the main menu
   * @see #getFileControlSet()
   * @see #getSettingsControlSet()
   * @see #getViewControlSet()
   * @see #getToolsControlSet()
   * @see #getHelpControlSet()
   */
  protected ControlSet getMainMenuControlSet() {
    final ControlSet menuControlSets = new ControlSet();
    menuControlSets.add(getFileControlSet());
    menuControlSets.add(getViewControlSet());
    menuControlSets.add(getToolsControlSet());
    final ControlSet supportTableControlSet = getSupportTableControlSet();
    if (supportTableControlSet != null) {
      menuControlSets.add(supportTableControlSet);
    }
    final List<ControlSet> additionalMenus = getAdditionalMenuControlSet();
    if (additionalMenus != null) {
      for (final ControlSet set : additionalMenus) {
        menuControlSets.add(set);
      }
    }
    menuControlSets.add(getHelpControlSet());

    return menuControlSets;
  }

  /**
   * @return the ControlSet specifying the items in the 'File' menu
   */
  protected ControlSet getFileControlSet() {
    final ControlSet file = new ControlSet(FrameworkMessages.get(FrameworkMessages.FILE));
    file.setMnemonic(FrameworkMessages.get(FrameworkMessages.FILE_MNEMONIC).charAt(0));
    file.add(Controls.control(this::exit, FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP),
            FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0)));

    return file;
  }

  /**
   * @return the ControlSet specifying the items in the 'Settings' menu
   */
  protected ControlSet getSettingsControlSet() {
    final ImageIcon setLoggingIcon = Images.loadImage(Images.ICON_LOGGING);
    final Control ctrSetLoggingLevel = Controls.control(this::setLoggingLevel,
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL));
    ctrSetLoggingLevel.setDescription(FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL_DESC));
    ctrSetLoggingLevel.setIcon(setLoggingIcon);

    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SETTINGS));

    controlSet.add(ctrSetLoggingLevel);

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'Tools' menu
   */
  protected ControlSet getToolsControlSet() {
    final ControlSet controlSet = new ControlSet(resourceBundle.getString("tools"),
            resourceBundle.getString("tools_mnemonic").charAt(0));
    controlSet.add(getSettingsControlSet());

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'View' menu
   */
  protected ControlSet getViewControlSet() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.VIEW),
            FrameworkMessages.get(FrameworkMessages.VIEW_MNEMONIC).charAt(0));
    final Control ctrRefreshAll = Controls.control(applicationModel::refresh,
            FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
    controlSet.add(ctrRefreshAll);
    controlSet.addSeparator();
    controlSet.add(Controls.control(this::viewApplicationTree,
            resourceBundle.getString("view_application_tree")));
    controlSet.add(Controls.control(this::viewDependencyTree,
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES)));
    controlSet.add(Controls.control(this::selectLookAndFeel,
            resourceBundle.getString(MSG_SELECT_LOOK_AND_FEEL)));
    controlSet.add(Controls.control(this::selectFontSize,
            resourceBundle.getString("select_font_size")));
    controlSet.addSeparator();
    final Control ctrAlwaysOnTop = Controls.toggleControl(this,
            "alwaysOnTop", FrameworkMessages.get(FrameworkMessages.ALWAYS_ON_TOP), alwaysOnTopChangedEvent);
    controlSet.add(ctrAlwaysOnTop);

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'Help' menu
   */
  protected ControlSet getHelpControlSet() {
    final ControlSet controlSet = new ControlSet(resourceBundle.getString(MSG_HELP),
            resourceBundle.getString("help_mnemonic").charAt(0));
    final Control ctrHelp = Controls.control(this::showHelp,
            resourceBundle.getString(MSG_HELP) + "...", null, null);
    controlSet.add(ctrHelp);
    controlSet.addSeparator();
    final Control ctrAbout = Controls.control(this::showAbout,
            resourceBundle.getString(MSG_ABOUT) + "...", null, null);
    controlSet.add(ctrAbout);

    return controlSet;
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
    return TextUtil.getTextFileContents(EntityApplicationPanel.class, TIPS_AND_TRICKS_FILE);
  }

  /**
   * @return the panel shown when Help -&#62; About is selected
   */
  protected JPanel getAboutPanel() {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    final String versionString = Version.getVersionAndBuildNumberString();
    panel.add(new JLabel(Images.loadImage("jminor_logo32.gif")), BorderLayout.WEST);
    final JTextField txtVersionMemory = new JTextField(versionString + " (" + Util.getMemoryUsageString() + ")");
    txtVersionMemory.setEditable(false);
    txtVersionMemory.setFocusable(false);
    panel.add(txtVersionMemory, BorderLayout.CENTER);

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
    Objects.requireNonNull(applicationModel, "applicationModel");
    this.applicationModel = applicationModel;
    clearEntityPanelProviders();
    setupEntityPanelProviders();
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
   * @return a List of ControlSet objects which are to be added to the main menu bar
   */
  protected List<ControlSet> getAdditionalMenuControlSet() {
    return new ArrayList<>(0);
  }

  /**
   * @return the ControlSet on which the Support Tables menu item is based on
   */
  protected ControlSet getSupportTableControlSet() {
    if (supportPanelProviders.isEmpty()) {
      return null;
    }

    final Comparator<String> comparator = TextUtil.getSpaceAwareCollator();
    final Entities domain = applicationModel.getDomain();
    supportPanelProviders.sort((ep1, ep2) -> {
        final String thisCompare = ep1.getCaption() == null ? domain.getCaption(ep1.getEntityId()) : ep1.getCaption();
        final String thatCompare = ep2.getCaption() == null ? domain.getCaption(ep2.getEntityId()) : ep2.getCaption();

        return comparator.compare(thisCompare, thatCompare);
    });
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES),
            FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0));
    supportPanelProviders.forEach(panelProvider -> controlSet.add(Controls.control(() -> showEntityPanelDialog(panelProvider),
            panelProvider.getCaption() == null ? domain.getCaption(panelProvider.getEntityId()) : panelProvider.getCaption())));

    return controlSet;
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel provider
   * @param panelProvider the entity panel provider
   */
  protected final void showEntityPanelDialog(final EntityPanelProvider panelProvider) {
    showEntityPanelDialog(panelProvider, false);
  }

  /**
   * Shows a dialog containing the entity panel provided by the given panel provider
   * @param panelProvider the entity panel provider
   * @param modalDialog if true the dialog is made modal
   */
  protected final void showEntityPanelDialog(final EntityPanelProvider panelProvider, final boolean modalDialog) {
    try {
      UiUtil.setWaitCursor(true, this);
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
              applicationModel.getDomain().getCaption(panelProvider.getEntityId()) : panelProvider.getCaption());
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(final WindowEvent e) {
          entityPanel.savePreferences();
        }
      });
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityPanel, BorderLayout.CENTER);
      UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0,
              JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new UiUtil.DisposeWindowAction(dialog));
      dialog.pack();
      dialog.setLocationRelativeTo(this);
      if (modalDialog) {
        dialog.setModal(true);
      }
      dialog.setResizable(true);
      SwingUtilities.invokeLater(() -> dialog.setVisible(true));
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
  }

  /**
   * Called during initialization, after the application model has been initialized,
   * override to keep all entity panel provider definitions in one place.
   * @see #addEntityPanelProvider(EntityPanelProvider)
   * @see #addEntityPanelProviders(EntityPanelProvider...)
   * @see #addSupportPanelProvider(EntityPanelProvider)
   * @see #addSupportPanelProviders(EntityPanelProvider...)
   */
  protected void setupEntityPanelProviders() {}

  /**
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = new JTabbedPane(TAB_PLACEMENT.get());
    applicationTabPane.setFocusable(false);
    applicationTabPane.addChangeListener(e -> ((EntityPanel) applicationTabPane.getSelectedComponent()).initializePanel());
    for (final EntityPanel entityPanel : entityPanels) {
      applicationTabPane.addTab(entityPanel.getCaption(), entityPanel);
      if (entityPanel.getEditPanel() != null) {
        entityPanel.getEditPanel().getActiveObserver().addListener(() -> {
          if (entityPanel.getEditPanel().isActive()) {
            LOG.debug("{} selectApplicationTab", entityPanel.getEditModel().getEntityId());
            applicationTabPane.setSelectedComponent(entityPanel);
          }
        });
      }
    }
    //initialize first panel
    if (applicationTabPane.getTabCount() > 0) {
      ((EntityPanel) applicationTabPane.getSelectedComponent()).initializePanel();
    }
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
   * By default this method returns the panels defined by the available {@link EntityPanelProvider}s.
   * @param applicationModel the application model responsible for providing EntityModels for the panels
   * @return a List containing the {@link EntityPanel}s to include in this application panel
   * @see #addEntityPanelProvider(EntityPanelProvider)
   */
  protected List<EntityPanel> initializeEntityPanels(final M applicationModel) {
    final List<EntityPanel> panels = new ArrayList<>();
    for (final EntityPanelProvider provider : entityPanelProviders) {
      final EntityPanel entityPanel;
      if (applicationModel.containsEntityModel(provider.getEntityId())) {
        entityPanel = provider.createPanel(applicationModel.getEntityModel(provider.getEntityId()));
      }
      else {
        entityPanel = provider.createPanel(applicationModel.getConnectionProvider());
        applicationModel.addEntityModel(entityPanel.getModel());
      }
      panels.add(entityPanel);
    }

    return panels;
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
   * @see UiUtil#getDefaultLookAndFeelClassName()
   */
  protected String getDefaultLookAndFeelClassName() {
    return PreferencesUtil.getUserPreference(applicationLookAndFeelProperty, UiUtil.getDefaultLookAndFeelClassName());
  }

  /**
   * @return the default font size multiplier
   * @see #selectFontSize()
   */
  protected Integer getDefaultFontSize() {
    return Integer.parseInt(PreferencesUtil.getUserPreference(applicationFontSizeProperty, "100"));
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
    UiUtil.centerWindow(initializationDialog);

    return initializationDialog;
  }

  /**
   * Initializes the progress panel to show in the startup dialog
   * @param icon the icon
   * @return an initialized startup progress panel
   */
  protected JPanel initializeStartupProgressPanel(final Icon icon) {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    final JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    progressBar.setIndeterminate(true);
    panel.add(progressBar, BorderLayout.CENTER);
    if (icon != null) {
      final JLabel lblIcon = new JLabel(icon);
      lblIcon.setBorder(BorderFactory.createRaisedBevelBorder());
      panel.add(lblIcon, BorderLayout.WEST);
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
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximized then its preferredSize is set to this value
   * @param applicationIcon the application icon
   * @param setVisible if true then the JFrame is set visible
   * @return an initialized, but non-visible JFrame
   */
  protected final JFrame prepareFrame(final String title, final boolean maximize,
                                      final boolean showMenuBar, final Dimension size,
                                      final ImageIcon applicationIcon, final boolean setVisible) {
    final JFrame frame = new JFrame();
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
      UiUtil.setSizeWithinScreenBounds(frame);
    }
    UiUtil.centerWindow(frame);
    if (maximize) {
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    frame.setTitle(title);
    if (showMenuBar) {
      frame.setJMenuBar(initializeMenuBar());
    }
    if (setVisible) {
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
   * @return by default a JMenuBar based on the main menu control set
   * @see #getMainMenuControlSet()
   */
  protected JMenuBar initializeMenuBar() {
    return ControlProvider.createMenuBar(getMainMenuControlSet());
  }

  /**
   * Initializes the application model
   * @param connectionProvider the db provider
   * @return an initialized application model
   * @throws CancelException in case the initialization is cancelled
   */
  protected abstract M initializeApplicationModel(final EntityConnectionProvider connectionProvider);

  /**
   * Returns the user, either via a login dialog or via override, called during startup
   * @param frameCaption the application frame caption
   * @param defaultUser the default user
   * @param applicationIcon the application icon
   * @return the application user
   * @throws CancelException in case a login dialog is cancelled
   */
  protected User getUser(final String frameCaption, final User defaultUser, final ImageIcon applicationIcon) {
    final String defaultUserName = EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name");
    final LoginPanel loginPanel = new LoginPanel(defaultUser == null ? new User(PreferencesUtil.getDefaultUserName(getApplicationIdentifier(),
            defaultUserName), null) : defaultUser);
    final String loginTitle = (!Util.nullOrEmpty(frameCaption) ? (frameCaption + " - ") : "") + Messages.get(Messages.LOGIN);
    final User user = loginPanel.showLoginPanel(null, loginTitle, applicationIcon);
    if (Util.nullOrEmpty(user.getUsername())) {
      throw new IllegalArgumentException(FrameworkMessages.get(FrameworkMessages.EMPTY_USERNAME));
    }

    return user;
  }

  /**
   * Saves the username so that it can be used as default the next time this application is started.
   * @param username the username
   */
  protected void saveDefaultUserName(final String username) {
    PreferencesUtil.setDefaultUserName(getApplicationIdentifier(), username);
  }

  /**
   * Returns a String identifying the type of application this EntityApplicationPanel represents,
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

  private void clearEntityPanelProviders() {
    entityPanelProviders.clear();
    supportPanelProviders.clear();
  }

  /**
   * Sets the uncaught exception handler
   */
  private void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler((t, e) -> handleException(e));
  }

  private void bindEventsInternal() {
    applicationModel.getConnectionProvider().getConnectedObserver().addDataListener(active ->
            setParentWindowTitle(active ? frameTitle : frameTitle + " - " + Messages.get(Messages.NOT_CONNECTED)));
  }

  private JFrame startApplicationInternal(final String frameCaption, final String iconName, final boolean maximize,
                                          final Dimension frameSize, final User defaultUser, final boolean showFrame,
                                          final User silentLoginUser) throws Exception {
    LOG.debug("{} application starting", frameCaption);
    FrameworkMessages.class.getName();//hack to force-load the class, initializes UI caption constants
    UIManager.setLookAndFeel(getDefaultLookAndFeelClassName());
    final Integer fontSize = getDefaultFontSize();
    if (!fontSize.equals(100)) {
      UiUtil.setFontSize(fontSize / 100f);
    }
    final ImageIcon applicationIcon = iconName != null ? Images.getImageIcon(getClass(), iconName) : Images.loadImage("jminor_logo32.gif");
    final JDialog startupDialog = showStartupDialog ? initializeStartupDialog(applicationIcon, frameCaption) : null;
    while (true) {
      final User user = silentLoginUser != null ? silentLoginUser : loginRequired ? getUser(frameCaption, defaultUser, applicationIcon) : new User("", null);
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
          saveDefaultUserName(connectionProvider.getUser().getUsername());
        }
        this.frameTitle = getFrameTitle(frameCaption, connectionProvider);
        final JFrame frame = prepareFrame(this.frameTitle, maximize, true, frameSize, applicationIcon, showFrame);
        this.applicationStartedEvent.fire();
        LOG.info(this.frameTitle + ", application started successfully, " + connectionProvider.getUser().getUsername()
                + ": " + (System.currentTimeMillis() - initializationStarted) + " ms");
        return frame;
      }
      catch (final Throwable exception) {
        handleStartupException(startupDialog, connectionProvider, exception);
      }
    }
  }

  private void handleStartupException(final JDialog startupDialog, final EntityConnectionProvider connectionProvider, final Throwable e) {
    try {
      if (connectionProvider != null) {
        connectionProvider.disconnect();
      }
    }
    catch (final Exception ex) {
      LOG.debug("Exception while disconnecting after a failed startup", ex);
    }
    handleException(e, null);
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

  private JScrollPane initializeTree(final TreeModel treeModel) {
    final JTree tree = new JTree(treeModel);
    tree.setShowsRootHandles(true);
    tree.setToggleClickCount(1);
    tree.setRootVisible(false);
    UiUtil.expandAll(tree, new TreePath(tree.getModel().getRoot()), true);

    return new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  private void setParentWindowTitle(final String title) {
    final Window parentWindow = UiUtil.getParentWindow(this);
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
   * Looks up user credentials via a {@link org.jminor.common.CredentialsProvider} service using an authentication token
   * found in the program arguments list. Useful for single sign on application launch.
   * <pre>javaws -open authenticationToken:123-123-123 http://jminor.org/demo/demo.jnlp</pre>
   * <pre>java -jar application/getdown-1.7.1.jar app_dir app_id authenticationToken:123-123-123</pre>
   * @param args the program arguments
   * @return the User credentials associated with the authentication token, null if no authentication token is found,
   * the user credentials have expired or if no authentication server is running
   */
  protected static User getUser(final String[] args) {
    final ServiceLoader<CredentialsProvider> loader = ServiceLoader.load(CredentialsProvider.class);
    final Optional<CredentialsProvider> credentialsProvider = loader.findFirst();
    if (credentialsProvider.isEmpty()) {
      LOG.debug("No CredentialsProvider service available");
      return null;
    }
    final CredentialsProvider provider = credentialsProvider.get();
    try {
      return provider.getCredentials(provider.getAuthenticationToken(args));
    }
    catch (final IllegalArgumentException e) {
      LOG.debug("Invalid UUID authentication token");
      return null;
    }
  }

  private static DefaultTreeModel createApplicationTree(final Collection<? extends MasterDetailPanel> entityPanels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

    return applicationTreeModel;
  }

  private static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends MasterDetailPanel> panels) {
    for (final MasterDetailPanel entityPanel : panels) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel);
      root.add(node);
      if (!entityPanel.getDetailPanels().isEmpty()) {
        addModelsToTree(node, entityPanel.getDetailPanels());
      }
    }
  }

  private static String getUserInfo(final EntityConnectionProvider connectionProvider) {
    final String description = connectionProvider.getDescription();

    return getUsername(connectionProvider.getUser().getUsername().toUpperCase()) + (description != null ? "@" + description.toUpperCase() : "");
  }

  private static String getUsername(final String username) {
    final String usernamePrefix = EntityApplicationModel.USERNAME_PREFIX.get();
    if (!Util.nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
      return username.substring(usernamePrefix.length());
    }

    return username;
  }

  private static final class EntityDependencyTreeNode extends DefaultMutableTreeNode {

    private final Entities domain;
    private final String domainId;

    private EntityDependencyTreeNode(final String domainId, final String entityId, final Entities domain) {
      super(entityId);
      this.domainId = domainId;
      this.domain = domain;
      Objects.requireNonNull(entityId, "entityId");
    }

    /**
     * @return the ID of the entity this node represents
     */
    public String getEntityId() {
      return (String) getUserObject();
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
      for (final String entityId : domain.getDefinedEntities()) {
        for (final Property.ForeignKeyProperty fkProperty : domain.getForeignKeyProperties(entityId)) {
          if (fkProperty.getForeignEntityId().equals(getEntityId()) && !foreignKeyCycle(fkProperty.getForeignEntityId())) {
            childrenList.add(new EntityDependencyTreeNode(domainId, entityId, domain));
          }
        }
      }

      return childrenList;
    }

    private boolean foreignKeyCycle(final String referencedEntityId) {
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
