/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.Events;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.ExceptionHandler;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.MasterDetailPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.DefaultEntityApplicationModel;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.db.provider.EntityConnectionProvider;
import org.jminor.framework.db.provider.EntityConnectionProviders;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.i18n.FrameworkMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A central application panel class.
 */
public abstract class EntityApplicationPanel extends JPanel implements ExceptionHandler, MasterDetailPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationPanel.class);

  public static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";

  private final List<EntityPanelProvider> entityPanelProviders = new ArrayList<EntityPanelProvider>();
  private final List<EntityPanelProvider> supportPanelProviders = new ArrayList<EntityPanelProvider>();
  private final List<EntityPanel> entityPanels = new ArrayList<EntityPanel>();

  private EntityApplicationModel applicationModel;
  private JTabbedPane applicationTabPane;

  private final Event evtApplicationStarted = Events.event();
  private final Event evtAlwaysOnTopChanged = Events.event();

  private final boolean persistEntityPanels = Configuration.getBooleanValue(Configuration.PERSIST_ENTITY_PANELS);
  private final Map<EntityPanelProvider, EntityPanel> persistentEntityPanels = new HashMap<EntityPanelProvider, EntityPanel>();

  private boolean loginRequired = Configuration.getBooleanValue(Configuration.AUTHENTICATION_REQUIRED);
  private boolean showStartupDialog = Configuration.getBooleanValue(Configuration.SHOW_STARTUP_DIALOG);

  private String frameTitle = "<no title>";

  public EntityApplicationPanel() {
    setUncaughtExceptionHandler();
  }

  /** {@inheritDoc} */
  @Override
  public final void handleException(final Throwable exception, final JComponent dialogParent) {
    LOG.error(exception.getMessage(), exception);
    DefaultExceptionHandler.getInstance().handleException(exception, dialogParent);
  }

  /**
   * @return the application model this application panel is based on
   */
  public final EntityApplicationModel getModel() {
    return applicationModel;
  }

  /**
   * Adds main application panels, displayed on application start
   * @param panelProviders the main application panel providers
   * @return this application panel instance
   */
  public final EntityApplicationPanel addEntityPanelProviders(final EntityPanelProvider... panelProviders) {
    Util.rejectNullValue(panelProviders, "panelProviders");
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
  public final EntityApplicationPanel addEntityPanelProvider(final EntityPanelProvider panelProvider) {
    entityPanelProviders.add(panelProvider);
    return this;
  }

  /**
   * Adds support application panels, available via a support panel manu
   * @param panelProviders the support application panel providers
   * @return this application panel instance
   */
  public final EntityApplicationPanel addSupportPanelProviders(final EntityPanelProvider... panelProviders) {
    Util.rejectNullValue(panelProviders, "panelProviders");
    for (final EntityPanelProvider panelProvider : panelProviders) {
      addSupportPanelProvider(panelProvider);
    }
    return this;
  }

  /**
   * Adds a support application panel, available via a support panel manu
   * @param panelProvider the support application panel provider
   * @return this application panel instance
   */
  public final EntityApplicationPanel addSupportPanelProvider(final EntityPanelProvider panelProvider) {
    supportPanelProviders.add(panelProvider);
    return this;
  }

  /**
   * @param entityID the entity ID
   * @return the first entity panel found based on the given entity type, null if none is found
   */
  public final EntityPanel getEntityPanel(final String entityID) {
    for (final EntityPanel entityPanel : entityPanels) {
      if (entityPanel.getModel().getEntityID().equals(entityID)) {
        return entityPanel;
      }
    }

    return null;
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
      evtAlwaysOnTopChanged.fire();
    }
  }

  /**
   * Performs a login, fetching user information via <code>getUser</code>
   * @throws CancelException in case the login is cancelled
   * @see #getUser(String, org.jminor.common.model.User, javax.swing.ImageIcon)
   */
  public final void login() throws CancelException {
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

  public final void viewApplicationTree() {
    UiUtil.showInDialog(getParentWindow(), initializeApplicationTree(), false,
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), false, true, null);
  }

  /**
   * Shows a dialog containing a dependency tree view of all defined entities
   */
  public final void viewDependencyTree() {
    UiUtil.showInDialog(getParentWindow(), initializeDependencyTree(), false,
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES), false, true, null);
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
    try {
      return startApplicationInternal(frameCaption, iconName, maximize, frameSize, defaultUser, showFrame);
    }
    catch (CancelException e) {
      System.exit(0);
    }
    catch (Exception e) {
      handleException(e, this);
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
  public final List<? extends MasterDetailPanel> getDetailPanels() {
    return Collections.unmodifiableList(entityPanels);
  }

  /** {@inheritDoc} */
  @Override
  public final void activatePanel() {}

  /**
   * Exists this application
   * @throws CancelException if the exit is cancelled
   */
  public final void exit() throws CancelException {
    if (Configuration.getBooleanValue(Configuration.CONFIRM_EXIT) && JOptionPane.showConfirmDialog(this, FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT),
            FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT_TITLE), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
      throw new CancelException();
    }

    try {
      savePreferences();
      Util.flushUserPreferences();
    }
    catch (Exception e) {
      LOG.debug("Exception while saving preferences", e);
    }
    try {
      applicationModel.getConnectionProvider().disconnect();
    }
    catch (Exception e) {
      LOG.debug("Exception while disconnecting from database", e);
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
            FrameworkMessages.get(FrameworkMessages.HELP));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    UiUtil.resizeWindow(dialog, 0.1, new Dimension(600, 750));
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
            FrameworkMessages.get(FrameworkMessages.ABOUT));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setModal(true);
    dialog.setVisible(true);
  }

  /**
   * @param listener a listener notified each time the always on top status changes
   */
  public final void addAlwaysOnTopListener(final EventListener listener) {
    evtAlwaysOnTopChanged.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeAlwaysOnTopListener(final EventListener listener) {
    evtAlwaysOnTopChanged.removeListener(listener);
  }

  /**
   * @param listener a listener notified when to application has been successfully started
   */
  public final void addApplicationStartedListener(final EventListener listener) {
    evtApplicationStarted.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeApplicationStartedListener(final EventListener listener) {
    evtApplicationStarted.removeListener(listener);
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
    file.add(Controls.methodControl(this, "exit", FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP),
            FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0)));

    return file;
  }

  /**
   * @return the ControlSet specifying the items in the 'Settings' menu
   */
  protected ControlSet getSettingsControlSet() {
    final ImageIcon setLoggingIcon = Images.loadImage(Images.ICON_LOGGING);
    final Control ctrSetLoggingLevel = Controls.methodControl(this, "setLoggingLevel",
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL));
    ctrSetLoggingLevel.setDescription(FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL_DESC));
    ctrSetLoggingLevel.setIcon(setLoggingIcon);

    final ControlSet controlSet = new ControlSet(Messages.get(Messages.SETTINGS));

    controlSet.add(ctrSetLoggingLevel);

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'Tools' menu
   */
  protected ControlSet getToolsControlSet() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.TOOLS),
            FrameworkMessages.get(FrameworkMessages.TOOLS_MNEMONIC).charAt(0));
    controlSet.add(getSettingsControlSet());

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'View' menu
   */
  protected ControlSet getViewControlSet() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.VIEW),
            FrameworkMessages.get(FrameworkMessages.VIEW_MNEMONIC).charAt(0));
    final Control ctrRefreshAll = Controls.methodControl(applicationModel, "refresh",
            FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
    controlSet.add(ctrRefreshAll);
    controlSet.addSeparator();
    controlSet.add(Controls.methodControl(this, "viewApplicationTree",
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), null, null));
    controlSet.add(Controls.methodControl(this, "viewDependencyTree",
            FrameworkMessages.get(FrameworkMessages.VIEW_DEPENDENCIES), null, null));
    controlSet.addSeparator();
    final Control ctrAlwaysOnTop = Controls.toggleControl(this,
            "alwaysOnTop", FrameworkMessages.get(FrameworkMessages.ALWAYS_ON_TOP), evtAlwaysOnTopChanged);
    controlSet.add(ctrAlwaysOnTop);

    return controlSet;
  }

  /**
   * @return the ControlSet specifying the items in the 'Help' menu
   */
  protected ControlSet getHelpControlSet() {
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.HELP),
            FrameworkMessages.get(FrameworkMessages.HELP_MNEMONIC).charAt(0));
    final Control ctrHelp = Controls.methodControl(this, "showHelp",
            FrameworkMessages.get(FrameworkMessages.HELP) + "...", null, null);
    controlSet.add(ctrHelp);
    controlSet.addSeparator();
    final Control ctrAbout = Controls.methodControl(this, "showAbout",
            FrameworkMessages.get(FrameworkMessages.ABOUT) + "...", null, null);
    controlSet.add(ctrAbout);

    return controlSet;
  }

  /**
   * @return the panel shown when Help -> Help is selected
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
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the text to show in the help panel
   * @throws IOException in case of an IO exception
   */
  protected String getHelpText() throws IOException {
    return Util.getTextFileContents(EntityApplicationPanel.class, TIPS_AND_TRICKS_FILE);
  }

  /**
   * @return the panel shown when Help -> About is selected
   */
  protected JPanel getAboutPanel() {
    final JPanel panel = new JPanel(UiUtil.createBorderLayout());
    final String versionString = Util.getVersionAndBuildNumber();
    panel.add(new JLabel(Images.loadImage("jminor_logo32.gif")), BorderLayout.WEST);
    final JTextField txtVersionMemory = new JTextField(versionString + " (" + Util.getMemoryUsageString() + ")");
    txtVersionMemory.setEditable(false);
    txtVersionMemory.setFocusable(false);
    panel.add(txtVersionMemory, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Initializes the entity db provider
   * @param user the user
   * @param clientTypeID a string specifying the client type
   * @return an initialized EntityConnectionProvider
   * @throws CancelException in case the initialization is cancelled
   */
  protected EntityConnectionProvider initializeConnectionProvider(final User user, final String clientTypeID) throws CancelException {
    return EntityConnectionProviders.createConnectionProvider(user, clientTypeID);
  }

  /**
   * Initializes this application panel
   * @param applicationModel the application model
   * @throws IllegalStateException if the application model has not been set
   * @throws org.jminor.common.model.CancelException in case the initialization is cancelled
   */
  protected final void initialize(final EntityApplicationModel applicationModel) throws CancelException {
    Util.rejectNullValue(applicationModel, "applicationModel");
    this.applicationModel = applicationModel;
    setupEntityPanelProviders();
    this.entityPanels.addAll(initializeEntityPanels(applicationModel));
    initializeUI();
    bindEventsInternal();
    bindEvents();
  }

  /**
   * Override to add event bindings after initialization
   * @see #initialize(org.jminor.framework.client.model.EntityApplicationModel)
   */
  protected void bindEvents() {}

  /**
   * @return a List of ControlSet objects which are to be added to the main menu bar
   */
  protected List<ControlSet> getAdditionalMenuControlSet() {
    return new ArrayList<ControlSet>(0);
  }

  /**
   * @return the ControlSet on which the Support Tables menu item is based on
   */
  protected ControlSet getSupportTableControlSet() {
    if (supportPanelProviders.isEmpty()) {
      return null;
    }

    Collections.sort(supportPanelProviders);
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES),
            FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0));
    for (final EntityPanelProvider panelProvider : supportPanelProviders) {
      String caption = panelProvider.getCaption();
      if (caption == null) {
        caption = Entities.getCaption(panelProvider.getEntityID());
      }
      controlSet.add(new Control(caption) {
        @Override
        public void actionPerformed(final ActionEvent e) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              showEntityPanelDialog(panelProvider);
            }
          });
        }
      });
    }

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
    final JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, this);
      final EntityPanel entityPanel;
      if (persistEntityPanels && persistentEntityPanels.containsKey(panelProvider)) {
        entityPanel = persistentEntityPanels.get(panelProvider);
        if (entityPanel.isShowing()) {
          return;
        }
      }
      else {
        entityPanel = panelProvider.createPanel(applicationModel.getConnectionProvider());
        entityPanel.initializePanel();
        if (persistEntityPanels) {
          persistentEntityPanels.put(panelProvider, entityPanel);
        }
      }
      String caption = panelProvider.getCaption();
      if (caption == null) {
        caption = Entities.getCaption(panelProvider.getEntityID());
      }
      dialog = new JDialog(getParentWindow(), caption);
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
    }
    finally {
      UiUtil.setWaitCursor(false, this);
    }
    dialog.setVisible(true);
  }

  /**
   * @return a JToolBar instance to show in the NORTH position
   */
  protected JToolBar getNorthToolBar() {
    return null;
  }

  /**
   * Called during initialization, after the application model has been initialized,
   * override to keep all entity panel provider definitions in one place
   */
  protected void setupEntityPanelProviders() {}

  /**
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = new JTabbedPane(Configuration.getIntValue(Configuration.TAB_PLACEMENT));
    applicationTabPane.setFocusable(false);
    applicationTabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    applicationTabPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(final ChangeEvent e) {
        ((EntityPanel) applicationTabPane.getSelectedComponent()).initializePanel();
      }
    });
    for (final EntityPanel entityPanel : entityPanels) {
      applicationTabPane.addTab(entityPanel.getCaption(), entityPanel);
      if (entityPanel.getEditPanel() != null) {
        entityPanel.getEditPanel().getActiveObserver().addListener(new EventAdapter() {
          /** {@inheritDoc} */
          @Override
          public void eventOccurred() {
            if (entityPanel.getEditPanel().isActive()) {
              LOG.debug("{} selectApplicationTab", entityPanel.getEditModel().getEntityID());
              applicationTabPane.setSelectedComponent(entityPanel);
            }
          }
        });
      }
    }
    //initialize first panel
    if (applicationTabPane.getTabCount() > 0) {
      ((EntityPanel) applicationTabPane.getSelectedComponent()).initializePanel();
    }
    add(applicationTabPane, BorderLayout.CENTER);

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
  protected List<EntityPanel> initializeEntityPanels(final EntityApplicationModel applicationModel) {
    final List<EntityPanel> panels = new ArrayList<EntityPanel>();
    for (final EntityPanelProvider provider : entityPanelProviders) {
      final EntityPanel entityPanel;
      if (applicationModel.containsEntityModel(provider.getEntityID())) {
        entityPanel = provider.createPanel(applicationModel.getEntityModel(provider.getEntityID()));
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
   */
  protected String getDefaultLookAndFeelClassName() {
    return UIManager.getSystemLookAndFeelClassName();
  }

  /**
   * Initializes a panel to show in the SOUTH position of this application frame,
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
   * @param user the user
   * @return a frame title based on the caption and user information
   */
  protected String getFrameTitle(final String frameCaption, final User user) {
    return frameCaption + " - " + getUserInfo(user, applicationModel.getConnectionProvider().getDescription());
  }

  /**
   * Initializes a JFrame according to the given parameters, containing this EntityApplicationPanel
   * @param title the title string for the JFrame
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximized then it's preferredSize is set to this value
   * @param applicationIcon the application icon
   * @param setVisible if true then the JFrame is set visible
   * @return an initialized, but non-visible JFrame
   * @see #getNorthToolBar()
   */
  protected final JFrame prepareFrame(final String title, final boolean maximize,
                                      final boolean showMenuBar, final Dimension size,
                                      final ImageIcon applicationIcon, final boolean setVisible) {
    final JFrame frame = new JFrame();
    frame.setIconImage(applicationIcon.getImage());
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        try {
          exit();
        }
        catch(CancelException ignored) {}
      }
    });
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(this, BorderLayout.CENTER);
    final JToolBar toolbar = getNorthToolBar();
    if (toolbar != null) {
      frame.getContentPane().add(toolbar, BorderLayout.NORTH);
    }
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
      frame.setJMenuBar(createMenuBar());
    }
    if (setVisible) {
      frame.setVisible(true);
    }

    return frame;
  }

  /**
   * @return a JMenuBar based on the main menu control set
   * @see #getMainMenuControlSet()
   */
  protected final JMenuBar createMenuBar() {
    return ControlProvider.createMenuBar(getMainMenuControlSet());
  }

  /**
   * Sets the uncaught exception handler, override to add specific uncaught exception handling
   */
  protected void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        DefaultExceptionHandler.getInstance().handleException(e, EntityApplicationPanel.this);
      }
    });
  }

  /**
   * Initializes the application model
   * @param connectionProvider the db provider
   * @return an initialized application model
   * @throws CancelException in case the initialization is cancelled
   */
  protected abstract EntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException;

  /**
   * Returns the user, either via a login dialog or via override, called during startup
   * @param frameCaption the application frame caption
   * @param defaultUser the default user
   * @param applicationIcon the application icon
   * @return the application user
   * @throws CancelException in case a login dialog is cancelled
   */
  protected User getUser(final String frameCaption, final User defaultUser, final ImageIcon applicationIcon) throws CancelException {
    final String defaultUserName = Configuration.getValue(Configuration.USERNAME_PREFIX) + System.getProperty("user.name");
    final User user = LoginPanel.showLoginPanel(null, defaultUser == null ? new User(Util.getDefaultUserName(getApplicationIdentifier(),
            defaultUserName), null) : defaultUser, applicationIcon, frameCaption + " - " + Messages.get(Messages.LOGIN), null, null);
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
    Util.setDefaultUserName(getApplicationIdentifier(), username);
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
   */
  protected void savePreferences() {
    for (final EntityPanel entityPanel : entityPanels) {
      entityPanel.savePreferences();
    }
  }

  private void bindEventsInternal() {
    final StateObserver connected = applicationModel.getConnectionProvider().getConnectedObserver();
    connected.addActivateListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        setParentWindowTitle(frameTitle);
      }
    });
    connected.addDeactivateListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        setParentWindowTitle(frameTitle + " - " + Messages.get(Messages.NOT_CONNECTED));
      }
    });
  }

  private JFrame startApplicationInternal(final String frameCaption, final String iconName, final boolean maximize,
                                          final Dimension frameSize, final User defaultUser, final boolean showFrame) throws Exception {
    LOG.debug("{} application starting", frameCaption);
    FrameworkMessages.class.getName();//hack to force-load the class, initializes UI caption constants
    UIManager.setLookAndFeel(getDefaultLookAndFeelClassName());
    final ImageIcon applicationIcon = iconName != null ? Images.getImageIcon(getClass(), iconName) : Images.loadImage("jminor_logo32.gif");
    final JDialog startupDialog = showStartupDialog ? initializeStartupDialog(applicationIcon, frameCaption) : null;
    while (true) {
      final User user = loginRequired ? getUser(frameCaption, defaultUser, applicationIcon) : new User("", "");
      if (startupDialog != null) {
        startupDialog.setVisible(true);
      }
      final EntityConnectionProvider connectionProvider = initializeConnectionProvider(user, getApplicationIdentifier());
      try {
        connectionProvider.getConnection();//throws exception if the server is not reachable
        final long initializationStarted = System.currentTimeMillis();
        initialize(initializeApplicationModel(connectionProvider));

        if (startupDialog != null) {
          startupDialog.dispose();
        }
        saveDefaultUserName(connectionProvider.getUser().getUsername());
        this.frameTitle = getFrameTitle(frameCaption, connectionProvider.getUser());
        final JFrame frame = prepareFrame(this.frameTitle, maximize, true, frameSize, applicationIcon, showFrame);
        this.evtApplicationStarted.fire();
        LOG.info(this.frameTitle + ", application started successfully, " + connectionProvider.getUser().getUsername()
                + ": " + (System.currentTimeMillis() - initializationStarted) + " ms");
        return frame;
      }
      catch (Throwable e) {
        try {
          if (connectionProvider != null) {
            connectionProvider.disconnect();
          }
        }
        catch (Exception ex) {
          LOG.debug("Exception while disconnecting after a failed startup", ex);
        }
        handleException(e, null);
        if (JOptionPane.YES_OPTION != JOptionPane.showConfirmDialog(null,
                FrameworkMessages.get(FrameworkMessages.RETRY),
                FrameworkMessages.get(FrameworkMessages.RETRY_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE)) {
          if (startupDialog != null) {
            startupDialog.dispose();
          }
          throw new CancelException();
        }
      }
    }
  }

  private JScrollPane initializeApplicationTree() {
    return initializeTree(createApplicationTree(entityPanels));
  }

  private JScrollPane initializeDependencyTree() {
    return initializeTree(DefaultEntityApplicationModel.getDependencyTreeModel());
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

  private static DefaultTreeModel createApplicationTree(final Collection<? extends EntityPanel> entityPanels) {
    final DefaultTreeModel applicationTreeModel = new DefaultTreeModel(new DefaultMutableTreeNode());
    addModelsToTree((DefaultMutableTreeNode) applicationTreeModel.getRoot(), entityPanels);

    return applicationTreeModel;
  }

  private static void addModelsToTree(final DefaultMutableTreeNode root, final Collection<? extends EntityPanel> panels) {
    for (final EntityPanel entityPanel : panels) {
      final DefaultMutableTreeNode node = new DefaultMutableTreeNode(entityPanel);
      root.add(node);
      if (!entityPanel.getDetailPanels().isEmpty()) {
        addModelsToTree(node, entityPanel.getDetailPanels());
      }
    }
  }

  private static String getUserInfo(final User user, final String dbDescription) {
    return getUsername(user.getUsername().toUpperCase()) + (dbDescription != null ? "@" + dbDescription.toUpperCase() : "");
  }

  private static String getUsername(final String username) {
    final String usernamePrefix = (String) Configuration.getValue(Configuration.USERNAME_PREFIX);
    if (!Util.nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
      return username.substring(usernamePrefix.length(), username.length());
    }

    return username;
  }
}
