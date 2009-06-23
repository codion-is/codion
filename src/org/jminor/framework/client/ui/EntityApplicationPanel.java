/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.User;
import org.jminor.common.db.dbms.IDatabase;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.IExceptionHandler;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class EntityApplicationPanel extends JPanel implements IExceptionHandler {

  private static final Logger log = Util.getLogger(EntityApplicationPanel.class);

  public static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";

  protected final List<EntityPanel> mainApplicationPanels = new ArrayList<EntityPanel>();

  protected EntityApplicationModel applicationModel;
  protected JTabbedPane applicationTabPane;

  protected Control ctrSetLoggingLevel;
  protected ToggleBeanPropertyLink ctrSelectDetail;
  protected ToggleBeanPropertyLink ctrCascadeRefresh;

  private final Event evtSelectedEntityPanelChanged = new Event("EntityApplicationPanel.evtSelectedEntityPanelChanged");
  private final Event evtAlwaysOnTopChanged = new Event("EntityApplicationPanel.evtAlwaysOnTopChanged");

  private static boolean persistEntityPanels;
  private static Map<EntityPanelProvider, EntityPanel> persistentEntityPanels = new HashMap<EntityPanelProvider, EntityPanel>();

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
    initializeSettings();
    persistEntityPanels = (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.PERSIST_ENTITY_PANELS);
    ToolTipManager.sharedInstance().setInitialDelay(
            (Integer) FrameworkSettings.get().getProperty(FrameworkSettings.TOOLTIP_DELAY));
  }

  /** {@inheritDoc} */
  public void handleException(final Throwable e) {
    log.error(this, e);
    FrameworkUiUtil.getExceptionHandler().handleException(e, null, this);
  }

  /**
   * @param applicationModel the application model this application panel should use
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public void initialize(final EntityApplicationModel applicationModel) throws UserException {
    if (applicationModel == null)
      throw new UserException("Unable to initialize application panel without application model");

    this.applicationModel = applicationModel;
    setUncaughtExceptionHandler();
    setupControls();
    initializeUI();
    initializeActiveEntityPanel();
    initializeResizingAndNavigation();
    bindEvents();
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

  public void setLoggingLevel() {
    FrameworkUiUtil.setLoggingLevel(this);
  }

  public void viewApplicationTree() {
    UiUtil.showInDialog(UiUtil.getParentWindow(this), initializeApplicationTree(), false,
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), false, true, null);
  }

  public static void startApplication(final String frameCaption,
                                      final Class<? extends EntityApplicationPanel> applicationPanelClass,
                                      final Class<? extends EntityApplicationModel> applicationModelClass,
                                      final String iconName, final boolean maximize, final Dimension frameSize) {
    startApplication(frameCaption, applicationPanelClass, applicationModelClass, iconName, maximize, frameSize, null);
  }

  public static void startApplication(final String frameCaption,
                                      final Class<? extends EntityApplicationPanel> applicationPanelClass,
                                      final Class<? extends EntityApplicationModel> applicationModelClass,
                                      final String iconName, final boolean maximize, final Dimension frameSize,
                                      final User defaultUser) {
    startApplication(frameCaption, applicationPanelClass, applicationModelClass, iconName, maximize, frameSize,
            defaultUser, true);
  }

  public static void startApplication(final String frameCaption,
                                      final Class<? extends EntityApplicationPanel> applicationPanelClass,
                                      final Class<? extends EntityApplicationModel> applicationModelClass,
                                      final String iconName, final boolean maximize, final Dimension frameSize,
                                      final User defaultUser, final boolean northToolBar) {
    startApplication(frameCaption, applicationPanelClass, applicationModelClass, iconName, maximize, frameSize,
            defaultUser, northToolBar, true);
  }

  public static void startApplication(final String frameCaption,
                                      final Class<? extends EntityApplicationPanel> applicationPanelClass,
                                      final Class<? extends EntityApplicationModel> applicationModelClass,
                                      final String iconName, final boolean maximize, final Dimension frameSize,
                                      final User defaultUser, final boolean northToolBar, final boolean showFrame) {
    log.info(frameCaption + " starting");
    final JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    final ImageIcon applicationIcon = iconName != null ? Images.getImageIcon(applicationPanelClass, iconName) :
            Images.loadImage("jminor_logo32.gif");
    frame.setIconImage(applicationIcon.getImage());
    JDialog initializationDialog = null;
    boolean retry = true;
    while (retry) {
      try {
        final EntityApplicationPanel applicationPanel = constructApplicationPanel(applicationPanelClass);
        final User user = applicationPanel.isLoginRequired() ?
                getUser(frameCaption, defaultUser, applicationPanelClass.getSimpleName(), applicationIcon) : new User("", "");

        final long now = System.currentTimeMillis();
        initializationDialog = showInitializationDialog(frame, applicationPanel, applicationIcon, frameCaption);

        applicationPanel.initialize(constructApplicationModel(applicationModelClass, user));

        final String frameTitle = applicationPanel.getFrameTitle(frameCaption, user);
        prepareFrame(frame, applicationPanel, frameTitle, maximize, northToolBar, true, frameSize);
        if (showFrame)
          frame.setVisible(true);

        initializationDialog.dispose();

        log.info(frameTitle + ", application started successfully " + "(" + (System.currentTimeMillis() - now) + " ms)");

        retry = false;//successful startup
        Util.setDefaultUserName(applicationPanelClass.getSimpleName(), user.getUsername());
      }
      catch (UserCancelException uce) {
        System.exit(0);
      }
      catch (Exception ue) {
        if (initializationDialog != null)
          initializationDialog.dispose();

        ExceptionDialog.showExceptionDialog(null, Messages.get(Messages.EXCEPTION), ue);

        retry = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null,
                FrameworkMessages.get(FrameworkMessages.RETRY),
                FrameworkMessages.get(FrameworkMessages.RETRY_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (!retry)
          System.exit(0);
      }
    }
  }

  public static String getUsername(final String username) {
    final String usernamePrefix = (String) FrameworkSettings.get().getProperty(FrameworkSettings.DEFAULT_USERNAME_PREFIX);
    if (usernamePrefix != null && usernamePrefix.length() > 0 && username.toUpperCase().startsWith(usernamePrefix.toUpperCase()))
      return username.substring(usernamePrefix.length(), username.length());

    return username;
  }

  public static void exit(final EntityApplicationPanel panel) throws UserCancelException {
    panel.exit();
  }

  public void exit() throws UserCancelException {
    if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.CONFIRM_EXIT)) {
      if (JOptionPane.showConfirmDialog(this, FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT),
              FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT_TITLE), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
        throw new UserCancelException();
    }
    try {
      applicationModel.getDbConnectionProvider().logout();
    }
    catch (Exception e) {
      log.debug("Unable to properly log out, no connection");
    }
    System.exit(0);
  }

  public void showHelp() throws UserException {
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

  public void showAbout() throws UserException {
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

  protected List<ControlSet> getMainMenuControlSet() {
    final List<ControlSet> menuControlSets = new ArrayList<ControlSet>();
    menuControlSets.add(getFileControlSet());
    menuControlSets.add(getViewControlSet());
    menuControlSets.add(getToolsControlSet());
    final ControlSet supportModelControlSet = getSupportModelControlSet();
    if (supportModelControlSet != null)
      menuControlSets.add(supportModelControlSet);
    final List<ControlSet> additionalMenus = getAdditionalMenuControlSet();
    if (additionalMenus != null && additionalMenus.size() > 0)
      menuControlSets.addAll(additionalMenus);

    menuControlSets.add(getHelpControlSet());

    return menuControlSets;
  }

  /**
   * @return the ControlSet specifying the items in the 'File' menu
   */
  protected ControlSet getFileControlSet() {
    final ControlSet file = new ControlSet(FrameworkMessages.get(FrameworkMessages.FILE));
    file.setMnemonic(FrameworkMessages.get(FrameworkMessages.FILE_MNEMONIC).charAt(0));
    file.add(ControlFactory.methodControl(this, "exit", FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP),
            FrameworkMessages.get(FrameworkMessages.EXIT_MNEMONIC).charAt(0)));

    return file;
  }

  /**
   * @return the ControlSet specifying the items in the 'Settings' menu
   */
  protected ControlSet getSettingsControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.SETTINGS));
    ret.add(ctrSelectDetail);
    ret.add(ctrCascadeRefresh);
    ret.addSeparator();
    ret.add(ctrSetLoggingLevel);

    return ret;
  }

  /**
   * @return the ControlSet specifying the items in the 'Tools' menu
   */
  protected ControlSet getToolsControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.TOOLS),
            FrameworkMessages.get(FrameworkMessages.TOOLS_MNEMONIC).charAt(0));
    ret.add(getSettingsControlSet());

    return ret;
  }

  /**
   * @return the ControlSet specifying the items in the 'View' menu
   */
  protected ControlSet getViewControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.VIEW),
            FrameworkMessages.get(FrameworkMessages.VIEW_MNEMONIC).charAt(0));
    final Control ctrRefreshAll = ControlFactory.methodControl(applicationModel, "refreshAll",
            FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
    ret.add(ctrRefreshAll);
    ret.addSeparator();
    ret.add(ControlFactory.methodControl(this, "viewApplicationTree",
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), null, null));
    ret.addSeparator();
    final ToggleBeanPropertyLink ctrAlwaysOnTop = ControlFactory.toggleControl(this,
            "alwaysOnTop", FrameworkMessages.get(FrameworkMessages.ALWAYS_ON_TOP), evtAlwaysOnTopChanged);
    ret.add(ctrAlwaysOnTop);

    return ret;
  }

  /**
   * @return the ControlSet specifying the items in the 'Help' menu
   */
  protected ControlSet getHelpControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.HELP),
            FrameworkMessages.get(FrameworkMessages.HELP_MNEMONIC).charAt(0));
    final Control ctrHelp = ControlFactory.methodControl(this, "showHelp",
            FrameworkMessages.get(FrameworkMessages.HELP) + "...", null, null);
    ret.add(ctrHelp);
    ret.addSeparator();
    final Control ctrAbout = ControlFactory.methodControl(this, "showAbout",
            FrameworkMessages.get(FrameworkMessages.ABOUT) + "...", null, null);
    ret.add(ctrAbout);

    return ret;
  }

  /**
   * @return the panel shown when Help -> Help is selected
   * @throws org.jminor.common.model.UserException in case there is an error reading the help file
   */
  protected JPanel getHelpPanel() throws UserException {
    try {
      final JPanel ret = new JPanel(new BorderLayout());
      final String contents = Util.getContents(EntityApplicationPanel.class, TIPS_AND_TRICKS_FILE);
      final JTextArea text = new JTextArea(contents);
      final JScrollPane scrollPane = new JScrollPane(text);
      text.setEditable(false);
      text.setFocusable(false);
      text.setLineWrap(true);
      text.setWrapStyleWord(true);
      ret.add(scrollPane, BorderLayout.CENTER);

      return ret;
    }
    catch (IOException e) {
      throw new UserException(e);
    }
  }

  /**
   * @return the panel shown when Help -> About is selected
   */
  protected JPanel getAboutPanel() {
    final JPanel ret = new JPanel(new BorderLayout(5,5));
    final String versionString = Util.getVersionAndBuildNumber();
    ret.add(new JLabel(Images.loadImage("jminor_logo32.gif")), BorderLayout.WEST);
    final JTextField txtVersion = new JTextField(versionString);
    txtVersion.setEditable(false);
    ret.add(txtVersion, BorderLayout.CENTER);

    return ret;
  }

  protected void setupControls() {
    final ImageIcon selectionFiltersDetailIcon = Images.loadImage(Images.ICON_SELECTION_FILTERS_DETAIL);
    ctrSelectDetail = ControlFactory.toggleControl(applicationModel, "selectionFiltersDetail",
            FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER), applicationModel.evtSelectionFiltersDetailChanged);
    ctrSelectDetail.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER_DESC));
    ctrSelectDetail.setIcon(selectionFiltersDetailIcon);

    final ImageIcon cascadeRefreshIcon = Images.loadImage(Images.ICON_CASCADE_REFRESH);
    ctrCascadeRefresh = ControlFactory.toggleControl(applicationModel, "cascadeRefresh",
            FrameworkMessages.get(FrameworkMessages.CASCADE_REFRESH), applicationModel.evtCascadeRefreshChanged);
    ctrCascadeRefresh.setDescription(FrameworkMessages.get(FrameworkMessages.CASCADE_REFRESH_DESC));
    ctrCascadeRefresh.setIcon(cascadeRefreshIcon);

    final ImageIcon setLoggingIcon = Images.loadImage(Images.ICON_PRINT_QUERIES);
    ctrSetLoggingLevel = ControlFactory.methodControl(this, "setLoggingLevel",
            FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL));
    ctrSetLoggingLevel.setDescription(FrameworkMessages.get(FrameworkMessages.SET_LOG_LEVEL_DESC));
    ctrSetLoggingLevel.setIcon(setLoggingIcon);
  }

  protected EntityPanel getEntityPanel(final Class<? extends EntityPanel> entityPanelClass) {
    for (final EntityPanel entityPanel : mainApplicationPanels) {
      if (entityPanel.getClass().equals(entityPanelClass))
        return entityPanel;
    }

    return null;
  }

  /**
   * A convenience method for overriding, so that system wide settings paramters can be set
   * before the application is initialized
   * @see FrameworkSettings
   */
  protected void initializeSettings() {}

  protected void bindEvents() {
    evtSelectedEntityPanelChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        initializeActiveEntityPanel();
      }
    });
  }

  /**
   * @return a List containing EntityPanelProvider objects specifying the main EntityPanels,
   * that is, the panels shown when the application frame is initialized
   */
  protected abstract List<EntityPanelProvider> getMainEntityPanelProviders();

  /**
   * @return a List containing EntityPanelProvider objects specifying the entity panels
   * that should be accessible via the Support Tables menu bar item.
   * The corresponding EntityModel class objects should be returned by the
   * EntityApplicationModel.getMainEntityModelClasses() method
   * N.B. these EntityPanelProvider objects should be constructed with a <code>caption</code> parameter.
   * @see org.jminor.framework.client.model.EntityApplicationModel#getMainEntityModelClasses()
   */
  protected List<EntityPanelProvider> getSupportEntityPanelProviders() {
    return new ArrayList<EntityPanelProvider>(0);
  }

  /**
   * @return a List of ControlSet objects which are to be added to the main menu bar
   */
  protected List<ControlSet> getAdditionalMenuControlSet() {
    return new ArrayList<ControlSet>();
  }

  /**
   * @return the ControlSet on which the Support Tables menu item is based on
   */
  protected ControlSet getSupportModelControlSet() {
    final List<EntityPanelProvider> supportDetailPanelProviders = getSupportEntityPanelProviders();
    if (supportDetailPanelProviders == null || supportDetailPanelProviders.size() == 0)
      return null;

    Collections.sort(supportDetailPanelProviders);
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES),
            FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES_MNEMONIC).charAt(0));
    for (final EntityPanelProvider panelProvider : supportDetailPanelProviders) {
      ret.add(new Control(panelProvider.getCaption()) {
        @Override
        public void actionPerformed(ActionEvent e) {
          showEntityPanel(panelProvider);
        }
      });
    }

    return ret;
  }

  protected void showEntityPanel(final EntityPanelProvider panelProvider) {
    try {
      showEntityPanelDialog(panelProvider, applicationModel.getDbConnectionProvider(), this);
    }
    catch (UserException ux) {
      throw ux.getRuntimeException();
    }
  }

  /**
   * @return a JToolBar instance to show in the NORTH position
   */
  protected JToolBar getNorthToolBar() {
    return null;
  }

  /**
   * Initializes this EntityApplicationPanel
   * @throws UserException in case of an exception
   */
  protected void initializeUI() throws UserException {
    setLayout(new BorderLayout());
    applicationTabPane = new JTabbedPane((Integer) FrameworkSettings.get().getProperty(FrameworkSettings.TAB_PLACEMENT));
    applicationTabPane.setFocusable(false);
    applicationTabPane.setUI(new BorderlessTabbedPaneUI());
    applicationTabPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        evtSelectedEntityPanelChanged.fire();
      }
    });
    final List<EntityPanelProvider> mainEntityPanelProviders = getMainEntityPanelProviders();
    for (final EntityPanelProvider provider : mainEntityPanelProviders) {
      final EntityModel entityModel = applicationModel.getMainApplicationModel(provider.getEntityModelClass());
      final EntityPanel entityPanel = provider.createInstance(entityModel);
      mainApplicationPanels.add(entityPanel);
      final String caption = (provider.getCaption() == null || provider.getCaption().length() == 0)
              ? entityModel.getCaption() : provider.getCaption();
      applicationTabPane.addTab(caption, entityPanel);
    }
    add(applicationTabPane, BorderLayout.CENTER);

    final JPanel southPanel = initializeSouthPanel();
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);
  }

  protected boolean isLoginRequired() {
    return (Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.AUTHENTICATION_REQUIRED);
  }

  protected JPanel initializeSouthPanel() {
    return null;
  }

  protected String getFrameTitle(final String frameCaption, final User user) throws Exception {
    final Properties properties =
            getModel().getDbConnectionProvider().getEntityDb().getUser().getProperties();
    return frameCaption + " - " + getUserInfo(user,
            properties != null ? properties.getProperty(IDatabase.DATABASE_SID_PROPERTY) : null);
  }

  protected JPanel initializeStartupProgressPane(final Icon icon) {
    final JPanel ret = new JPanel(new BorderLayout(5,5));
    final JProgressBar prog = new JProgressBar(JProgressBar.HORIZONTAL);
    prog.setIndeterminate(true);
    ret.add(prog, BorderLayout.CENTER);
    if (icon != null) {
      final JLabel lblIcon = new JLabel(icon);
      lblIcon.setBorder(BorderFactory.createRaisedBevelBorder());
      ret.add(lblIcon, BorderLayout.WEST);
    }

    return ret;
  }

  protected void initializeResizingAndNavigation() {
    final DefaultTreeModel panelTree = createApplicationTree(mainApplicationPanels);
    final Enumeration enumeration = ((DefaultMutableTreeNode) panelTree.getRoot()).breadthFirstEnumeration();
    while (enumeration.hasMoreElements()) {
      final EntityPanel panel = (EntityPanel) ((DefaultMutableTreeNode) enumeration.nextElement()).getUserObject();
      if (panel != null) {
        initializeResizing(panel);
        if ((Boolean) FrameworkSettings.get().getProperty(FrameworkSettings.USE_KEYBOARD_NAVIGATION))
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
        FrameworkUiUtil.getExceptionHandler().handleException(throwable, null, EntityApplicationPanel.this);
      }
    });
  }

  private JScrollPane initializeApplicationTree() {
    final JTree tree = new JTree(createApplicationTree(mainApplicationPanels));
    tree.setShowsRootHandles(true);
    tree.setToggleClickCount(1);
    tree.setRootVisible(false);
    UiUtil.expandAll(tree, new TreePath(tree.getModel().getRoot()), true);

    return new JScrollPane(tree, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  /**
   * Initializes a JFrame according to the given parameters
   * @param frame the frame to prepare
   * @param applicationPanel the EntityApplicationPanel to show in the frame
   * @param title the title string for the JFrame
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param northToolBar true if a toolbar should be included
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximed then it's preferredSize is set to this value
   * @return an initialized, but non-visible JFrame
   * @throws org.jminor.common.model.UserException in case of a user exception
   * @see #getNorthToolBar()
   */
  private static JFrame prepareFrame(final JFrame frame, final EntityApplicationPanel applicationPanel,
                                     final String title, final boolean maximize, final boolean northToolBar,
                                     final boolean showMenuBar, final Dimension size) throws UserException {
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          applicationPanel.exit();
        }
        catch(UserCancelException uc) {/**/}
      }
    });

    frame.setTitle(title);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(applicationPanel, BorderLayout.CENTER);
    if (showMenuBar)
      frame.setJMenuBar(applicationPanel.createMenuBar());
    if (northToolBar) {
      final JToolBar toolbar = applicationPanel.getNorthToolBar();
      if (toolbar != null)
        frame.getContentPane().add(toolbar, BorderLayout.NORTH);
    }
    if (size != null)
      frame.setSize(size);
    else {
      frame.pack();
      UiUtil.setSizeWithinScreenBounds(frame);
    }
    UiUtil.centerWindow(frame);
    if (maximize)
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

    return frame;
  }

  private JMenuBar createMenuBar() throws UserException {
    return ControlProvider.createMenuBar(getMainMenuControlSet());
  }

  private void initializeActiveEntityPanel() {
    ((EntityPanel) applicationTabPane.getSelectedComponent()).initialize();
  }

  private void initializeResizing(final EntityPanel panel) {
    final InputMap inputMap = panel.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    final ActionMap actionMap = panel.getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT,
            KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), DIV_LEFT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT,
            KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), DIV_RIGHT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP,
            KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), DIV_UP);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN,
            KeyEvent.SHIFT_DOWN_MASK + KeyEvent.ALT_DOWN_MASK, true), DIV_DOWN);

    actionMap.put(DIV_RIGHT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel activePanelParent = panel.getMasterPanel();
        if (activePanelParent != null)
          activePanelParent.resizePanel(EntityPanel.RIGHT, DIVIDER_JUMP);
      }
    });
    actionMap.put(DIV_LEFT, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        final EntityPanel activePanelParent = panel.getMasterPanel();
        if (activePanelParent != null)
          activePanelParent.resizePanel(EntityPanel.LEFT, DIVIDER_JUMP);
      }
    });
    actionMap.put(DIV_DOWN, new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        panel.resizePanel(EntityPanel.DOWN, DIVIDER_JUMP);
      }
    });
    actionMap.put(DIV_UP, new AbstractAction() {
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
      panel.setActive(true);
  }

  private EntityPanel getActivePanel(final List<EntityPanel> panels) {
    if (panels.size() == 0)
      return null;

    for (final EntityPanel panel : panels) {
      final EntityPanel ret = getActivePanel(panel.getDetailPanels());
      if (ret != null)
        return ret;
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

  private static EntityApplicationPanel constructApplicationPanel(
          final Class<? extends EntityApplicationPanel> applicationPanelClass) throws UserException {
    try {
      return applicationPanelClass.getConstructor().newInstance();
    }
    catch (InvocationTargetException te) {
      if (te.getTargetException() instanceof UserException)
        throw (UserException) te.getTargetException();

      throw new UserException(te.getTargetException());
    }
    catch (NoSuchMethodException e) {
      throw new UserException(e);
    }
    catch (IllegalAccessException e) {
      throw new UserException(e);
    }
    catch (InstantiationException e) {
      throw new UserException(e);
    }
  }

  private static EntityApplicationModel constructApplicationModel(
          final Class<? extends EntityApplicationModel> applicationModelClass, final User user) throws UserException {
    try {
      return applicationModelClass.getConstructor(User.class).newInstance(user);
    }
    catch (InvocationTargetException te) {
      final Throwable target = te.getTargetException();
      if (target instanceof UserException)
        throw (UserException) target;
      else if (target instanceof RuntimeException)
        throw (RuntimeException) target;
      else
        throw new UserException(target);
    }
    catch (NoSuchMethodException e) {
      throw new UserException(e);
    }
    catch (IllegalAccessException e) {
      throw new UserException(e);
    }
    catch (InstantiationException e) {
      throw new UserException(e);
    }
  }

  private static JDialog showInitializationDialog(final JFrame owner, final EntityApplicationPanel applicationPanel,
                                                  final Icon icon, final String initializationMessage) {
    final String message = initializationMessage == null ? "Initializing Application" : initializationMessage;
    final JDialog initializationDialog = new JDialog(owner, message, false);
    initializationDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    panel.add(applicationPanel.initializeStartupProgressPane(icon));
    initializationDialog.getContentPane().add(panel, BorderLayout.CENTER);
    initializationDialog.pack();
    UiUtil.centerWindow(initializationDialog);
    initializationDialog.setVisible(true);

    return initializationDialog;
  }

  private static String getUserInfo(final User user, final String dbSid) {
    return getUsername(user.getUsername().toUpperCase()) + (dbSid != null ? "@" + dbSid.toUpperCase() : "");
  }

  private static User getUser(final String frameCaption, final User defaultUser,
                              final String applicationIdentifier, final ImageIcon applicationIcon)
          throws UserCancelException, UserException {
    final User user = LoginPanel.showLoginPanel(null, defaultUser == null ?
            new User(FrameworkSettings.getDefaultUsername(applicationIdentifier), null) : defaultUser,
            applicationIcon, frameCaption + " - " + Messages.get(Messages.LOGIN), null, null);
    if (user.getUsername() == null || user.getUsername().length() == 0)
      throw new UserException(FrameworkMessages.get(FrameworkMessages.EMPTY_USERNAME));

    return user;
  }

  private static void showEntityPanelDialog(final EntityPanelProvider panelProvider, final IEntityDbProvider dbProvider,
                                            final JPanel owner) throws UserException {
    showEntityPanelDialog(panelProvider, dbProvider, owner, false);
  }

  private static void showEntityPanelDialog(final EntityPanelProvider panelProvider, final IEntityDbProvider dbProvider,
                                            final JPanel owner, final boolean modalDialog) throws UserException {
    final JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, owner);
      EntityPanel entityPanel;
      if (persistEntityPanels && persistentEntityPanels.containsKey(panelProvider)) {
        entityPanel = persistentEntityPanels.get(panelProvider);
        if (entityPanel.isShowing())
          return;
      }
      else {
        entityPanel = panelProvider.createInstance(dbProvider);
        entityPanel.initialize();
        if (persistEntityPanels)
          persistentEntityPanels.put(panelProvider, entityPanel);
      }
      dialog = new JDialog(UiUtil.getParentWindow(owner), panelProvider.getCaption());
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
      dialog.getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
              KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
      dialog.getRootPane().getActionMap().put("close", closeAction);
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(btnClose);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
      dialog.pack();
      dialog.setLocationRelativeTo(owner);
      if (modalDialog)
        dialog.setModal(true);
      dialog.setResizable(true);
    }
    finally {
      UiUtil.setWaitCursor(false, owner);
    }
    dialog.setVisible(true);
  }
}