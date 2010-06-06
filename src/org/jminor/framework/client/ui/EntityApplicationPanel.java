/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.ExceptionHandler;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanValueLink;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.i18n.FrameworkMessages;

import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A central application panel class.
 */
public abstract class EntityApplicationPanel extends JPanel implements ExceptionHandler {

  private static final Logger log = Util.getLogger(EntityApplicationPanel.class);

  public static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";

  private final List<EntityPanel> mainApplicationPanels = new ArrayList<EntityPanel>();

  private EntityApplicationModel applicationModel;
  private JTabbedPane applicationTabPane;

  private final Event evtApplicationStarted = new Event();
  private final Event evtSelectedEntityPanelChanged = new Event();
  private final Event evtAlwaysOnTopChanged = new Event();

  private final boolean persistEntityPanels;
  private Map<EntityPanelProvider, EntityPanel> persistentEntityPanels = new HashMap<EntityPanelProvider, EntityPanel>();

  private static final int DIVIDER_JUMP = 30;

  private static final String NAV_UP = "navigateUp";
  private static final String NAV_DOWN = "navigateDown";
  private static final String NAV_RIGHT = "navigateRight";
  private static final String NAV_LEFT = "navigateLeft";

  private static final String DIV_LEFT = "divLeft";
  private static final String DIV_RIGHT = "divRight";
  private static final String DIV_UP = "divUp";
  private static final String DIV_DOWN = "divDown";

  /** Constructs a new EntityApplicationPanel. */
  public EntityApplicationPanel() {
    configureApplication();
    persistEntityPanels = Configuration.getBooleanValue(Configuration.PERSIST_ENTITY_PANELS);
    ToolTipManager.sharedInstance().setInitialDelay(Configuration.getIntValue(Configuration.TOOLTIP_DELAY));
  }

  /** {@inheritDoc} */
  public void handleException(final Throwable e, final JComponent component) {
    log.error(this, e);
    DefaultExceptionHandler.get().handleException(e, component);
  }

  /**
   * @return the application model this application panel uses
   */
  public EntityApplicationModel getModel() {
    return this.applicationModel;
  }

  /**
   * @return true if the frame this application panel is shown in should be 'alwaysOnTop'
   */
  public boolean isAlwaysOnTop() {
    final JFrame parent = UiUtil.getParentFrame(this);
    return parent != null && parent.isAlwaysOnTop();
  }

  /**
   * fires: evtAlwaysOnTopChanged
   * @param value the new value
   */
  public void setAlwaysOnTop(final boolean value) {
    final JFrame parent = UiUtil.getParentFrame(this);
    if (parent != null) {
      parent.setAlwaysOnTop(value);
      evtAlwaysOnTopChanged.fire();
    }
  }

  public void login() throws CancelException {
    getModel().login(getUser(Messages.get(Messages.LOGIN), null, getClass().getSimpleName(), null));
  }

  public void logout() {
    getModel().logout();
  }

  public void setLoggingLevel() {
    EntityUiUtil.setLoggingLevel(this);
  }

  public void viewApplicationTree() {
    UiUtil.showInDialog(UiUtil.getParentWindow(this), initializeApplicationTree(), false,
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), false, true, null);
  }

  public void startApplication(final String frameCaption, final String iconName, final boolean maximize,
                               final Dimension frameSize) {
    startApplication(frameCaption, iconName, maximize, frameSize, null);
  }

  public void startApplication(final String frameCaption, final String iconName, final boolean maximize,
                               final Dimension frameSize, final User defaultUser) {
    startApplication(frameCaption, iconName, maximize, frameSize, defaultUser, true);
  }

  public void startApplication(final String frameCaption, final String iconName, final boolean maximize,
                               final Dimension frameSize, final User defaultUser, final boolean showFrame) {
    log.info(frameCaption + " starting");
    new Messages();//OptionPane messages are statically loaded
    final JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    final ImageIcon applicationIcon = iconName != null ? Images.getImageIcon(getClass(), iconName) :
            Images.loadImage("jminor_logo32.gif");
    frame.setIconImage(applicationIcon.getImage());
    JDialog startupDialog = null;
    boolean retry = true;
    while (retry) {
      try {
        final User user = isLoginRequired() ? getUser(frameCaption, defaultUser, getClass().getSimpleName(), applicationIcon) :
                new User("", "");

        final long now = System.currentTimeMillis();
        startupDialog = showStartupDialog(frame, applicationIcon, frameCaption);

        initialize(user);

        final String frameTitle = getFrameTitle(frameCaption, user);
        prepareFrame(frame, frameTitle, maximize, true, frameSize, showFrame);

        log.info(frameTitle + ", application started successfully " + "(" + (System.currentTimeMillis() - now) + " ms)");

        retry = false;//successful startup
        saveDefaultUser(user);
        evtApplicationStarted.fire();
      }
      catch (CancelException uce) {
        System.exit(0);
      }
      catch (Exception ue) {
        ExceptionDialog.showExceptionDialog(null, Messages.get(Messages.EXCEPTION), ue);

        retry = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                FrameworkMessages.get(FrameworkMessages.RETRY),
                FrameworkMessages.get(FrameworkMessages.RETRY_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (!retry)
          System.exit(0);
      }
      finally {
        if (startupDialog != null)
          startupDialog.dispose();
      }
    }
  }

  /**
   * @param user the user used when initializing the application model
   * @throws CancelException in case the initialization is cancelled
   */
  public void initialize(final User user) throws CancelException {
    if (user == null)
      throw new RuntimeException("Unable to initialize application panel without a user");

    this.applicationModel = initializeApplicationModel(user);
    setUncaughtExceptionHandler();
    initializeUI();
    initializeActiveEntityPanel();
    initializeResizingAndNavigation();
    bindEventsInternal();
    bindEvents();
  }

  public void exit() throws CancelException {
    if (Configuration.getBooleanValue(Configuration.CONFIRM_EXIT)) {
      if (JOptionPane.showConfirmDialog(this, FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT),
              FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT_TITLE), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
        throw new CancelException();
    }
    try {
      savePreferences();
    }
    catch (Exception e) {
      log.debug("Exception while saving preferences", e);
    }
    try {
      applicationModel.getDbProvider().disconnect();
    }
    catch (Exception e) {
      log.debug("Exception while disconnecting from database", e);
    }
    System.exit(0);
  }

  public void showHelp() {
    final JOptionPane pane = new JOptionPane(getHelpPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.NO_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            FrameworkMessages.get(FrameworkMessages.HELP));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    UiUtil.resizeWindow(dialog, 0.1, new Dimension(600, 750));
    dialog.setLocationRelativeTo(this);
    dialog.setResizable(true);
    dialog.setModal(false);
    dialog.setVisible(true);
  }

  public void showAbout() {
    final JOptionPane pane = new JOptionPane(getAboutPanel(), JOptionPane.PLAIN_MESSAGE,
            JOptionPane.NO_OPTION, null, new String[] {Messages.get(Messages.CLOSE)});
    final JDialog dialog = pane.createDialog(EntityApplicationPanel.this,
            FrameworkMessages.get(FrameworkMessages.ABOUT));
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    dialog.setLocationRelativeTo(this);
    dialog.setModal(true);
    dialog.setVisible(true);
  }

  public Event eventAlwaysOnTopChanged() {
    return evtAlwaysOnTopChanged;
  }

  public Event eventApplicationStarted() {
    return evtApplicationStarted;
  }

  public Event eventSelectedEntityPanelChanged() {
    return evtSelectedEntityPanelChanged;
  }

  protected ControlSet getMainMenuControlSet() {
    final ControlSet menuControlSets = new ControlSet();
    menuControlSets.add(getFileControlSet());
    menuControlSets.add(getViewControlSet());
    menuControlSets.add(getToolsControlSet());
    final ControlSet supportTableControlSet = getSupportTableControlSet();
    if (supportTableControlSet != null)
      menuControlSets.add(supportTableControlSet);
    final List<ControlSet> additionalMenus = getAdditionalMenuControlSet();
    if (additionalMenus != null) {
      for (final ControlSet set : additionalMenus)
        menuControlSets.add(set);
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
    if (isLoginRequired()) {
      file.add(ControlFactory.methodControl(this, "logout", Messages.get(Messages.LOGOUT)));
      file.add(ControlFactory.methodControl(this, "login", Messages.get(Messages.LOGIN)));
      file.addSeparator();
    }
    file.add(ControlFactory.methodControl(this, "exit", FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP),
            FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0)));

    return file;
  }

  /**
   * @return the ControlSet specifying the items in the 'Settings' menu
   */
  protected ControlSet getSettingsControlSet() {
    final ImageIcon selectionFiltersDetailIcon = Images.loadImage(Images.ICON_SELECTION_FILTERS_DETAIL);
    final Control ctrSelectDetail = ControlFactory.toggleControl(applicationModel, "selectionFiltersDetail",
            FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER), applicationModel.eventSelectionFiltersDetailChanged());
    ctrSelectDetail.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER_DESC));
    ctrSelectDetail.setIcon(selectionFiltersDetailIcon);

    final ImageIcon cascadeRefreshIcon = Images.loadImage(Images.ICON_CASCADE_REFRESH);
    final Control ctrCascadeRefresh = ControlFactory.toggleControl(applicationModel, "cascadeRefresh",
            FrameworkMessages.get(FrameworkMessages.CASCADE_REFRESH), applicationModel.eventCascadeRefreshChanged());
    ctrCascadeRefresh.setDescription(FrameworkMessages.get(FrameworkMessages.CASCADE_REFRESH_DESC));
    ctrCascadeRefresh.setIcon(cascadeRefreshIcon);

    final ImageIcon setLoggingIcon = Images.loadImage(Images.ICON_PRINT_QUERIES);
    final Control ctrSetLoggingLevel = ControlFactory.methodControl(this, "setLoggingLevel",
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL));
    ctrSetLoggingLevel.setDescription(FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL_DESC));
    ctrSetLoggingLevel.setIcon(setLoggingIcon);

    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SETTINGS));

    controlSet.add(ctrSelectDetail);
    controlSet.add(ctrCascadeRefresh);
    controlSet.addSeparator();
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
    final Control ctrRefreshAll = ControlFactory.methodControl(applicationModel, "refresh",
            FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
    controlSet.add(ctrRefreshAll);
    controlSet.addSeparator();
    controlSet.add(ControlFactory.methodControl(this, "viewApplicationTree",
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), null, null));
    controlSet.addSeparator();
    final ToggleBeanValueLink ctrAlwaysOnTop = ControlFactory.toggleControl(this,
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
    final Control ctrHelp = ControlFactory.methodControl(this, "showHelp",
            FrameworkMessages.get(FrameworkMessages.HELP) + "...", null, null);
    controlSet.add(ctrHelp);
    controlSet.addSeparator();
    final Control ctrAbout = ControlFactory.methodControl(this, "showAbout",
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
    final JPanel panel = new JPanel(new BorderLayout(5,5));
    final String versionString = Util.getVersionAndBuildNumber();
    panel.add(new JLabel(Images.loadImage("jminor_logo32.gif")), BorderLayout.WEST);
    final JTextField txtVersionMemory = new JTextField(versionString + " (" + Util.getMemoryUsageString() + ")");
    txtVersionMemory.setEditable(false);
    panel.add(txtVersionMemory, BorderLayout.CENTER);

    return panel;
  }

  protected EntityPanel getEntityPanel(final Class<? extends EntityPanel> entityPanelClass) {
    for (final EntityPanel entityPanel : mainApplicationPanels) {
      if (entityPanel.getClass().equals(entityPanelClass))
        return entityPanel;
    }

    return null;
  }

  /**
   * A convenience method for overriding, so that system wide configuration parameters can be set
   * before the application is initialized
   * @see org.jminor.framework.Configuration
   */
  protected void configureApplication() {}

  protected void bindEventsInternal() {
    evtSelectedEntityPanelChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        initializeActiveEntityPanel();
      }
    });
  }

  protected void bindEvents() {}

  /**
   * @return a List containing EntityPanelProvider objects specifying the main EntityPanels,
   * that is, the panels shown when the application frame is initialized
   */
  protected abstract List<EntityPanelProvider> getMainEntityPanelProviders();

  /**
   * @return a List containing EntityPanelProvider objects specifying the entity panels
   * that should be accessible via the Support Tables menu bar item.
   * The corresponding EntityModel objects should be returned by the
   * EntityApplicationModel.initializeMainApplicationModels() method
   * N.B. these EntityPanelProvider objects should be constructed with a <code>caption</code> parameter.
   * @see org.jminor.framework.client.model.EntityApplicationModel#initializeMainApplicationModels(org.jminor.framework.db.provider.EntityDbProvider) ()
   */
  protected List<EntityPanelProvider> getSupportEntityPanelProviders() {
    return new ArrayList<EntityPanelProvider>(0);
  }

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
    final List<EntityPanelProvider> supportDetailPanelProviders = getSupportEntityPanelProviders();
    if (supportDetailPanelProviders == null || supportDetailPanelProviders.size() == 0)
      return null;

    Collections.sort(supportDetailPanelProviders);
    final ControlSet controlSet = new ControlSet(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES),
            FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0));
    for (final EntityPanelProvider panelProvider : supportDetailPanelProviders) {
      controlSet.add(new Control(panelProvider.getCaption()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          showEntityPanelDialog(panelProvider);
        }
      });
    }

    return controlSet;
  }

  protected void showEntityPanelDialog(final EntityPanelProvider panelProvider) {
    showEntityPanelDialog(panelProvider, false);
  }

  protected void showEntityPanelDialog(final EntityPanelProvider panelProvider, final boolean modalDialog) {
    final JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, this);
      EntityPanel entityPanel;
      if (persistEntityPanels && persistentEntityPanels.containsKey(panelProvider)) {
        entityPanel = persistentEntityPanels.get(panelProvider);
        if (entityPanel.isShowing())
          return;
      }
      else {
        entityPanel = EntityPanel.createInstance(panelProvider, getModel().getDbProvider());
        entityPanel.initializePanel();
        if (persistEntityPanels)
          persistentEntityPanels.put(panelProvider, entityPanel);
      }
      dialog = new JDialog(UiUtil.getParentWindow(this), panelProvider.getCaption());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.setLayout(new BorderLayout());
      dialog.add(entityPanel, BorderLayout.CENTER);
      final Action closeAction = new AbstractAction(Messages.get(Messages.CLOSE)) {
        public void actionPerformed(ActionEvent e) {
          dialog.dispose();
        }
      };
      final JButton btnClose = new JButton(closeAction);
      btnClose.setMnemonic('L');
      UiUtil.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, closeAction);
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(btnClose);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
      dialog.pack();
      dialog.setLocationRelativeTo(this);
      if (modalDialog)
        dialog.setModal(true);
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
   * Initializes this EntityApplicationPanel
   */
  protected void initializeUI() {
    setLayout(new BorderLayout());
    applicationTabPane = new JTabbedPane(Configuration.getIntValue(Configuration.TAB_PLACEMENT));
    applicationTabPane.setFocusable(false);
    applicationTabPane.setUI(UiUtil.getBorderlessTabbedPaneUI());
    applicationTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        evtSelectedEntityPanelChanged.fire();
      }
    });
    final List<EntityPanelProvider> mainEntityPanelProviders = getMainEntityPanelProviders();
    if (mainEntityPanelProviders == null || mainEntityPanelProviders.size() == 0)
      throw new RuntimeException("No main entity panels provided");
    for (final EntityPanelProvider provider : mainEntityPanelProviders) {
      final EntityModel entityModel = applicationModel.getMainApplicationModel(provider.getEntityModelClass());
      final EntityPanel entityPanel = EntityPanel.createInstance(provider, entityModel);
      mainApplicationPanels.add(entityPanel);
      final String caption = (provider.getCaption() == null || provider.getCaption().length() == 0)
              ? entityPanel.getCaption() : provider.getCaption();
      applicationTabPane.addTab(caption, entityPanel);
    }
    add(applicationTabPane, BorderLayout.CENTER);

    final JPanel southPanel = initializeSouthPanel();
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);
  }

  protected boolean isLoginRequired() {
    return Configuration.getBooleanValue(Configuration.AUTHENTICATION_REQUIRED);
  }

  protected JPanel initializeSouthPanel() {
    return null;
  }

  protected JDialog showStartupDialog(final JFrame owner, final Icon icon, final String startupMessage) {
    final String message = startupMessage == null ? "Initializing Application" : startupMessage;
    final JDialog initializationDialog = new JDialog(owner, message, false);
    initializationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    panel.add(initializeStartupProgressPanel(icon));
    initializationDialog.getContentPane().add(panel, BorderLayout.CENTER);
    initializationDialog.pack();
    UiUtil.centerWindow(initializationDialog);
    initializationDialog.setVisible(true);

    return initializationDialog;
  }

  protected JPanel initializeStartupProgressPanel(final Icon icon) {
    final JPanel panel = new JPanel(new BorderLayout(5,5));
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

  protected String getFrameTitle(final String frameCaption, final User user) throws Exception {
    return frameCaption + " - " + getUserInfo(user, getModel().getDbProvider().getDescription());
  }

  /**
   * Initializes a JFrame according to the given parameters, containing this EntityApplicationPanel
   * @param frame the frame to prepare
   * @param title the title string for the JFrame
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximized then it's preferredSize is set to this value
   * @param setVisible if true then the JFrame is set visible
   * @return an initialized, but non-visible JFrame
   * @see #getNorthToolBar()
   */
  protected JFrame prepareFrame(final JFrame frame, final String title, final boolean maximize,
                                final boolean showMenuBar, final Dimension size, final boolean setVisible) {
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          exit();
        }
        catch(CancelException uc) {/**/}
      }
    });
    frame.setTitle(title);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(this, BorderLayout.CENTER);
    if (showMenuBar)
      frame.setJMenuBar(createMenuBar());
    final JToolBar toolbar = getNorthToolBar();
    if (toolbar != null)
      frame.getContentPane().add(toolbar, BorderLayout.NORTH);
    if (size != null)
      frame.setSize(size);
    else {
      frame.pack();
      UiUtil.setSizeWithinScreenBounds(frame);
    }
    UiUtil.centerWindow(frame);
    if (maximize)
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
    if (setVisible)
      frame.setVisible(true);

    return frame;
  }

  protected JMenuBar createMenuBar() {
    return ControlProvider.createMenuBar(getMainMenuControlSet());
  }

  protected void initializeResizingAndNavigation() {
    final DefaultTreeModel panelTree = createApplicationTree(mainApplicationPanels);
    final Enumeration enumeration = ((DefaultMutableTreeNode) panelTree.getRoot()).breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      final EntityPanel panel = (EntityPanel) ((DefaultMutableTreeNode) enumeration.nextElement()).getUserObject();
      if (panel != null) {
        initializeResizing(panel);
        if (Configuration.getBooleanValue(Configuration.USE_KEYBOARD_NAVIGATION))
          initializeNavigation(panel);
      }
    }
  }

  /**
   * Sets the uncaught exception handler, override to add specific uncaught exception handling
   */
  protected void setUncaughtExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(final Thread thread, final Throwable throwable) {
        DefaultExceptionHandler.get().handleException(throwable, EntityApplicationPanel.this);
      }
    });
  }

  protected abstract EntityApplicationModel initializeApplicationModel(final User user) throws CancelException;

  protected User getUser(final String frameCaption, final User defaultUser, final String applicationIdentifier,
                         final ImageIcon applicationIcon) throws CancelException {
    final User user = LoginPanel.showLoginPanel(null, defaultUser == null ?
            new User(Configuration.getDefaultUsername(applicationIdentifier), null) : defaultUser,
            applicationIcon, frameCaption + " - " + Messages.get(Messages.LOGIN), null, null);
    if (user.getUsername() == null || user.getUsername().length() == 0)
      throw new RuntimeException(FrameworkMessages.get(FrameworkMessages.EMPTY_USERNAME));

    return user;
  }

  /**
   * Saves the user info so that it can be used as default the next time this application is started.
   * This default implementation saves the username in the user preferences.
   * @param user the user
   */
  protected void saveDefaultUser(final User user) {
    Util.setDefaultUserName(getClass().getSimpleName(), user.getUsername());
  }

  /**
   * Called during the exit() method, override to save user preferences on program exit
   */
  protected void savePreferences() {}

  private JScrollPane initializeApplicationTree() {
    final JTree tree = new JTree(createApplicationTree(mainApplicationPanels));
    tree.setShowsRootHandles(true);
    tree.setToggleClickCount(1);
    tree.setRootVisible(false);
    UiUtil.expandAll(tree, new TreePath(tree.getModel().getRoot()), true);

    return new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  private void initializeActiveEntityPanel() {
    ((EntityPanel) applicationTabPane.getSelectedComponent()).initializePanel();
  }

  private void initializeResizing(final EntityPanel panel) {
    UiUtil.addKeyEvent(panel, KeyEvent.VK_LEFT, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new AbstractAction(DIV_LEFT) {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel activePanelParent = panel.getMasterPanel();
        if (activePanelParent != null)
          activePanelParent.resizePanel(EntityPanel.LEFT, DIVIDER_JUMP);
      }
    });
    UiUtil.addKeyEvent(panel, KeyEvent.VK_RIGHT, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new AbstractAction(DIV_RIGHT) {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel activePanelParent = panel.getMasterPanel();
        if (activePanelParent != null)
          activePanelParent.resizePanel(EntityPanel.RIGHT, DIVIDER_JUMP);
      }
    });
    UiUtil.addKeyEvent(panel, KeyEvent.VK_DOWN, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new AbstractAction(DIV_DOWN) {
      public void actionPerformed(ActionEvent e) {
        panel.resizePanel(EntityPanel.DOWN, DIVIDER_JUMP);
      }
    });
    UiUtil.addKeyEvent(panel, KeyEvent.VK_UP, KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            new AbstractAction(DIV_UP) {
      public void actionPerformed(ActionEvent e) {
        panel.resizePanel(EntityPanel.UP, DIVIDER_JUMP);
      }
    });
  }

  private void initializeNavigation(final EntityPanel panel) {
    final InputMap inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    final ActionMap actionMap = panel.getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK, true), NAV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK, true), NAV_DOWN);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, true), NAV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK, true), NAV_LEFT);

    actionMap.put(NAV_UP, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(EntityPanel.UP);
      }
    });
    actionMap.put(NAV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(EntityPanel.DOWN);
      }
    });
    actionMap.put(NAV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(EntityPanel.RIGHT);
      }
    });
    actionMap.put(NAV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        navigate(EntityPanel.LEFT);
      }
    });
  }

  private void navigate(final int direction) {
    final EntityPanel active = getActivePanel(mainApplicationPanels);
    if (active != null) {
      switch(direction) {
        case EntityPanel.UP:
          activatePanel(active.getMasterPanel());
          break;
        case EntityPanel.DOWN:
          if (active.getDetailPanels().size() > 0) {
            if (active.getDetailPanelState() == EntityPanel.HIDDEN)
              active.setDetailPanelState(EntityPanel.EMBEDDED);
            activatePanel(active.getLinkedDetailPanel());
          }
          else
            activatePanel((EntityPanel) applicationTabPane.getSelectedComponent());//go to top
          break;
        case EntityPanel.LEFT:
          activatePanel(getPanelOnLeft(active));
          break;
        case EntityPanel.RIGHT:
          activatePanel(getPanelOnRight(active));
          break;
      }
    }
  }

  private static void activatePanel(final EntityPanel panel) {
    if (panel != null)
      panel.getEditModel().setActive(true);
  }

  private EntityPanel getActivePanel(final List<EntityPanel> panels) {
    if (panels.size() == 0)
      return null;

    for (final EntityPanel panel : panels) {
      final EntityPanel activeDetailPanel = getActivePanel(panel.getDetailPanels());
      if (activeDetailPanel != null)
        return activeDetailPanel;
      if (panel.isActive())
        return panel;
    }

    return null;
  }

  private EntityPanel getPanelOnLeft(final EntityPanel panel) {
    final List<EntityPanel> siblings = panel.getMasterPanel() == null ?
            mainApplicationPanels : panel.getMasterPanel().getDetailPanels();
    final int index = siblings.indexOf(panel);
    if (index == 0)//leftmost panel
      return siblings.get(siblings.size()-1);
    else
      return siblings.get(index-1);
  }

  private EntityPanel getPanelOnRight(final EntityPanel panel) {
    final List<EntityPanel> siblings = panel.getMasterPanel() == null ?
            mainApplicationPanels : panel.getMasterPanel().getDetailPanels();
    final int index = siblings.indexOf(panel);
    if (index == siblings.size()-1)//rightmost panel
      return siblings.get(0);
    else
      return siblings.get(index+1);
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
      if (entityPanel.getDetailPanels().size() > 0)
        addModelsToTree(node, entityPanel.getDetailPanels());
    }
  }

  private static String getUserInfo(final User user, final String dbDescription) {
    return getUsername(user.getUsername().toUpperCase()) + (dbDescription != null ? "@" + dbDescription.toUpperCase() : "");
  }

  private static String getUsername(final String username) {
    final String usernamePrefix = (String) Configuration.getValue(Configuration.USERNAME_PREFIX);
    if (usernamePrefix != null && usernamePrefix.length() > 0 && username.toUpperCase().startsWith(usernamePrefix.toUpperCase()))
      return username.substring(usernamePrefix.length(), username.length());

    return username;
  }
}