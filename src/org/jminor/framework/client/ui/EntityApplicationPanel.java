/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.apache.log4j.Logger;
import org.jminor.common.db.Database;
import org.jminor.common.db.User;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.common.ui.BorderlessTabbedPaneUI;
import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.IExceptionHandler;
import org.jminor.common.ui.LoginPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.ToggleBeanPropertyLink;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.IEntityDbProvider;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public abstract class EntityApplicationPanel extends JPanel implements IExceptionHandler {

  private static final Logger log = Util.getLogger(EntityApplicationPanel.class);

  public static final String TIPS_AND_TRICKS_FILE = "TipsAndTricks.txt";

  protected final List<EntityPanel> mainApplicationPanels = new ArrayList<EntityPanel>();

  protected EntityApplicationModel model;
  protected JTabbedPane applicationTabPane;

  protected Control ctrSetLoggingLevel;
  protected ToggleBeanPropertyLink ctrSelectDetail;
  protected ToggleBeanPropertyLink ctrCascadeRefresh;

  private final Event evtSelectedEntityPanelChanged = new Event("EntityApplicationPanel.evtSelectedEntityPanelChanged");
  private final Event evtAlwaysOnTopChanged = new Event("EntityApplicationPanel.evtAlwaysOnTopChanged");

  private static EntityApplicationPanel applicationPanel;

  static {
    ToolTipManager.sharedInstance().setInitialDelay(500);
  }

  /** Constructs a new EntityApplicationPanel. */
  public EntityApplicationPanel() {
    if (applicationPanel != null)
      throw new IllegalStateException("EntityApplicationPanel has already been initialized, only one per application please");
    initializeSettings();
    applicationPanel = this;
  }

  public static EntityApplicationPanel getApplicationPanel() {
    return applicationPanel;
  }

  public EntityApplicationPanel(final EntityApplicationModel model) throws UserCancelException {
    setModel(model);
  }

  /** {@inheritDoc} */
  public void handleException(final Throwable e) {
    log.error(this, e);
    FrameworkUiUtil.handleException(e, null, this);
  }

  /**
   * @param model Value to set for property 'model'.
   * @throws org.jminor.common.model.UserCancelException if the user cancels
   * during the login procedure
   */
  public void setModel(final EntityApplicationModel model) throws UserCancelException {
    this.model = model;
  }

  /**
   * @return Value for property 'model'.
   */
  public EntityApplicationModel getModel() {
    return this.model;
  }

  public void initialize() throws UserException {
    if (model == null)
      throw new UserException("Cannot initialize panel without application model");

    setupControls();
    initializeUI();
    initializeActiveEntityPanel();
    bindEvents();
  }

  /**
   * @return Value for property 'alwaysOnTop'.
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

  public void setLookAndFeel() throws IllegalAccessException, UnsupportedLookAndFeelException,
          InstantiationException, ClassNotFoundException {
    org.jminor.common.ui.UiUtil.setLookAndFeel(UiUtil.getParentWindow(this));
  }

  public void setLoggingLevel() {
    FrameworkUiUtil.setLoggingLevel(this);
  }

  /**
   * @return Value for property 'fileControlSet'.
   */
  public ControlSet getFileControlSet() {
    final ControlSet file = new ControlSet(FrameworkMessages.get(FrameworkMessages.FILE));
    file.add(ControlFactory.methodControl(this, "exit", FrameworkMessages.get(FrameworkMessages.EXIT),
            null, FrameworkMessages.get(FrameworkMessages.EXIT_TIP)));

    return file;
  }

  public void viewApplicationTree() {
    UiUtil.showInDialog(UiUtil.getParentWindow(this), initializeApplicationTree(), false,
            FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE), false, true, null);
  }

  public static void exit(final EntityApplicationPanel panel) throws UserCancelException {
    panel.exit();
  }

  public void exit() throws UserCancelException {
    if (FrameworkSettings.get().confirmExit) {
      if (JOptionPane.showConfirmDialog(this, FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT),
              FrameworkMessages.get(FrameworkMessages.CONFIRM_EXIT_TITLE), JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION)
        throw new UserCancelException();
    }
    try {
      model.getDbConnectionProvider().getEntityDb().logout();
    }
    catch (Exception e) {
      log.debug("Unable to properly log out, no connection");
    }
    System.exit(0);
  }

  /**
   * @return Value for property 'settingsControlSet'.
   */
  public ControlSet getSettingsControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.SETTINGS));
    ret.add(ctrSelectDetail);
    ret.add(ctrCascadeRefresh);
    ret.addSeparator();
    ret.add(ctrSetLoggingLevel);

    return ret;
  }

  /**
   * @return Value for property 'toolsControlSet'.
   * @throws org.jminor.common.model.UserException in case of an exception
   */
  public ControlSet getToolsControlSet() throws UserException {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.TOOLS));
    ret.add(getSettingsControlSet());

    return ret;
  }

  /**
   * @return Value for property 'viewControlSet'.
   */
  public ControlSet getViewControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.VIEW));
    final Control ctrRefreshAll = ControlFactory.methodControl(model, "forceRefreshAll",
            FrameworkMessages.get(FrameworkMessages.REFRESH_ALL));
    ret.add(ctrRefreshAll);
    ret.addSeparator();
    ret.add(ControlFactory.methodControl(this, "viewApplicationTree", FrameworkMessages.get(FrameworkMessages.APPLICATION_TREE),
            null, null));
    ret.addSeparator();
    final ToggleBeanPropertyLink ctrAlwaysOnTop = ControlFactory.toggleControl(this,
            "alwaysOnTop", FrameworkMessages.get(FrameworkMessages.ALWAYS_ON_TOP), evtAlwaysOnTopChanged);
    ret.add(ctrAlwaysOnTop);
    ret.addSeparator();
    ret.add(initLookAndFeelControl());

    return ret;
  }

  /**
   * @return Value for property 'helpControlSet'.
   */
  public ControlSet getHelpControlSet() {
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.HELP), 'H');
    final Control ctrHelp = ControlFactory.methodControl(this, "showHelp",
            FrameworkMessages.get(FrameworkMessages.HELP) + "...", null, null);
    ret.add(ctrHelp);
    ret.addSeparator();
    final Control ctrAbout = ControlFactory.methodControl(this, "showAbout",
            FrameworkMessages.get(FrameworkMessages.ABOUT) + "...", null, null);
    ret.add(ctrAbout);

    return ret;
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

  protected Control initLookAndFeelControl() {
    final Control ret =
            ControlFactory.methodControl(this, "setLookAndFeel", FrameworkMessages.get(FrameworkMessages.SET_LOOK_AND_FEEL));
    ret.setMnemonic('L');

    return ret;
  }

  protected void setupControls() {
    final ImageIcon selectionFiltersDetailIcon = Images.loadImage(Images.ICON_SELECTION_FILTERS_DETAIL);
    ctrSelectDetail = ControlFactory.toggleControl(model, "selectionFiltersDetail",
            FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER), model.evtSelectionFiltersDetailChanged);
    ctrSelectDetail.setDescription(FrameworkMessages.get(FrameworkMessages.SELECTION_FILTER_DESC));
    ctrSelectDetail.setIcon(selectionFiltersDetailIcon);

    final ImageIcon cascadeRefreshIcon = Images.loadImage(Images.ICON_CASCADE_REFRESH);
    ctrCascadeRefresh = ControlFactory.toggleControl(model, "cascadeRefresh",
            FrameworkMessages.get(FrameworkMessages.CASCADE_REFRESH), model.evtCascadeRefreshChanged);
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

  protected void selectEntityPanel(final Class<? extends EntityPanel> entityPanelClass) {
    if (applicationTabPane != null)
      applicationTabPane.setSelectedComponent(getEntityPanel(entityPanelClass));
  }

  protected void initializeSettings() {}

  protected void bindEvents() {
    evtSelectedEntityPanelChanged.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        initializeActiveEntityPanel();
      }
    });
  }

  /**
   * @return Value for property 'rootEntityPanelInfo'.
   */
  protected abstract List<EntityPanel.EntityPanelInfo> getRootEntityPanelInfo();

  /**
   * @return Value for property 'supportEntityPanelInfo'.
   */
  protected List<EntityPanel.EntityPanelInfo> getSupportEntityPanelInfo() {
    return new ArrayList<EntityPanel.EntityPanelInfo>(0);
  }

  /**
   * @return Value for property 'additionalMenuControlSet'.
   */
  protected ControlSet[] getAdditionalMenuControlSet() {
    return new ControlSet[0];
  }

  /**
   * @return Value for property 'supportModelControlSet'.
   */
  protected ControlSet getSupportModelControlSet() {
    final List<EntityPanel.EntityPanelInfo> supportAppInfos = getSupportEntityPanelInfo();
    if (supportAppInfos == null || supportAppInfos.size() == 0)
      return null;

    Collections.sort(supportAppInfos);
    final ControlSet ret = new ControlSet(FrameworkMessages.get(FrameworkMessages.SUPPORT_TABLES), 'T');
    for (final EntityPanel.EntityPanelInfo appInfo : supportAppInfos) {
      final Control ctr = new Control(appInfo.getCaption()) {
        public void actionPerformed(ActionEvent e) {
          showEntityPanel(appInfo);
        }
      };
      ret.add(ctr);
    }

    return ret;
  }

  protected void showEntityPanel(final EntityPanel.EntityPanelInfo appInfo) {
    try {
      showEntityPanelDialog(appInfo, model.getDbConnectionProvider(), this);
    }
    catch (UserException ux) {
      throw ux.getRuntimeException();
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * @return Value for property 'northToolBar'.
   */
  protected JToolBar getNorthToolBar() {
    return null;
  }

  protected void initializeUI() throws UserException {
    setLayout(new BorderLayout());
    final List<EntityPanel.EntityPanelInfo> entityPanels = getRootEntityPanelInfo();
    if (entityPanels.size() > 1) {
      applicationTabPane = new JTabbedPane(FrameworkSettings.get().tabPlacement);
      applicationTabPane.setFocusable(false);
      applicationTabPane.setUI(new BorderlessTabbedPaneUI());
      applicationTabPane.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          evtSelectedEntityPanelChanged.fire();
        }
      });
    }

    for (final EntityPanel.EntityPanelInfo info : entityPanels) {
      try {
        final EntityModel entityModel = model.getMainApplicationModel(info.getEntityModelClass());
        final EntityPanel entityPanel = info.getInstance(entityModel);
        mainApplicationPanels.add(entityPanel);
        if (entityPanels.size() > 1) {
          final String caption = (info.getCaption() == null || info.getCaption().length() == 0)
                  ? entityModel.getModelCaption() : info.getCaption();
          applicationTabPane.addTab(caption, entityPanel);
        }
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        if (e instanceof UserException)
          throw (UserException) e;
        else if (e.getCause() instanceof UserException)
          throw (UserException) e.getCause();

        e.printStackTrace();
        throw new UserException(e);
      }
    }
    if (mainApplicationPanels.size() == 1)
      add(mainApplicationPanels.get(0), BorderLayout.CENTER);
    else
      add(applicationTabPane, BorderLayout.CENTER);

    final JPanel southPanel = initializeSouthPanel();
    if (southPanel != null)
      add(southPanel, BorderLayout.SOUTH);
  }

  public static EntityApplicationPanel startApplication(final String frameCaption,
                                                        final Class<? extends EntityApplicationPanel> panelClass,
                                                        final Class<? extends EntityApplicationModel> modelClass,
                                                        final String iconName, final boolean maximize, final Dimension size) {
    return startApplication(frameCaption, panelClass, modelClass, iconName, maximize, size, null);
  }

  public static EntityApplicationPanel startApplication(final String frameCaption,
                                                        final Class<? extends EntityApplicationPanel> panelClass,
                                                        final Class<? extends EntityApplicationModel> modelClass,
                                                        final String iconName, final boolean maximize,
                                                        final Dimension size, final User defaultUser) {
    return startApplication(frameCaption, panelClass, modelClass, iconName, maximize, size, defaultUser, true);
  }

  public static EntityApplicationPanel startApplication(final String frameCaption,
                                                        final Class<? extends EntityApplicationPanel> panelClass,
                                                        final Class<? extends EntityApplicationModel> applicationModelClass,
                                                        final String iconName, final boolean maximize,
                                                        final Dimension size, final User defaultUser,
                                                        final boolean northToolBar) {
    return startApplication(frameCaption, panelClass, applicationModelClass, iconName, maximize, size, defaultUser, northToolBar, true);
  }

  public static EntityApplicationPanel startApplication(final String frameCaption,
                                                        final Class<? extends EntityApplicationPanel> panelClass,
                                                        final Class<? extends EntityApplicationModel> applicationModelClass,
                                                        final String iconName, final boolean maximize,
                                                        final Dimension size, final User defaultUser,
                                                        final boolean northToolBar, final boolean showFrame) {
    log.info(frameCaption + " starting");
    EntityApplicationPanel applicationPanel = null;
    JDialog initializationDialog = null;
    boolean retry = true;
    while (retry) {
      try {
        final ImageIcon applicationIcon = iconName != null ? Images.getImageIcon(panelClass, iconName) :
                Images.loadImage("jminor_logo32.gif");
        applicationPanel = constructApplicationPanel(panelClass);
        if (FrameworkSettings.get().printMemoryUsageInterval > 0)
          Util.printMemoryUsage(FrameworkSettings.get().printMemoryUsageInterval);

        initializationDialog = showInitializationDialog(null, applicationPanel, applicationIcon, frameCaption);
        final User user = LoginPanel.showLoginPanel(null, defaultUser == null ?
                new User(FrameworkSettings.getDefaultUsername(), null) : defaultUser,
                applicationIcon, frameCaption + " - " + Messages.get(Messages.LOGIN), null, null);
        if (user.getPassword() == null || user.getPassword().length() == 0)
          throw new UserException(FrameworkMessages.get(FrameworkMessages.EMPTY_PASSWORD));

        final long now = System.currentTimeMillis();

        applicationPanel.setModel(initializeApplicationModel(applicationModelClass, user));
        initializeApplicationPanel(applicationPanel);

        final Properties properties =
                applicationPanel.getModel().getDbConnectionProvider().getEntityDb().getUser().getProperties();
        final String frameTitle = frameCaption + " - " + getUserInfo(user,
                properties != null ? properties.getProperty(Database.DATABASE_SID_PROPERTY) : null);
        if (showFrame)
          applicationPanel.prepareFrame(applicationIcon, frameTitle, maximize, northToolBar, true, size).setVisible(true);

        initializationDialog.dispose();

        log.info(frameTitle + ", application started successfully " + "(" + (System.currentTimeMillis() - now) + " ms)");

        retry = false;//successful startup
      }
      catch (Exception ue) {
        if (ue instanceof UserCancelException)
          System.exit(0);

        if (initializationDialog != null)
          initializationDialog.dispose();

        if (applicationPanel != null)
          FrameworkUiUtil.handleException(ue, null, applicationPanel);
        else
          ExceptionDialog.showExceptionDialog(null, Messages.get(Messages.EXCEPTION), ue);

        retry = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, FrameworkMessages.get(FrameworkMessages.RETRY),
                FrameworkMessages.get(FrameworkMessages.RETRY_TITLE),
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (!retry)
          System.exit(0);
        else
          EntityApplicationPanel.applicationPanel = null;
      }
    }

    return applicationPanel;
  }

  public static String getUsername(String username) {
    if (username.indexOf("OPS") == 0)
      username = username.substring(4, username.length());

    return username;
  }

  protected JPanel initializeSouthPanel() {
    return null;
  }

  protected static JDialog showInitializationDialog(final Frame owner, final EntityApplicationPanel panel,
                                                    final Icon icon, final String msg) {
    final String message = msg == null ? "Initializing Application" : msg;
    final JDialog initDlg = new JDialog(owner, message, false);
    initDlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    p.add(panel.getInitProgressPane(icon));
    initDlg.getContentPane().add(p, BorderLayout.CENTER);
    initDlg.pack();
    UiUtil.centerWindow(initDlg);
    initDlg.setVisible(true);

    return initDlg;
  }

  protected JPanel getInitProgressPane(final Icon icon) {
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

  protected static EntityApplicationPanel constructApplicationPanel(
          final Class<? extends EntityApplicationPanel> applicationPanelClass) throws UserException {
    try {
      try {
        return applicationPanelClass.getConstructor().newInstance();
      }
      catch (InvocationTargetException te) {
        throw (Exception) te.getTargetException();
      }
    }
    catch (Exception e) {
      throw new UserException(e);
    }
  }

  protected static EntityApplicationModel initializeApplicationModel(
          final Class<? extends EntityApplicationModel> applicationModelClass, final User user) throws UserException {
    try {
      return applicationModelClass.getConstructor(User.class).newInstance(user);
    }
    catch (NoSuchMethodException ix) {
      throw new RuntimeException(ix);
    }
    catch (InstantiationException ix) {
      throw new RuntimeException(ix);
    }
    catch (IllegalAccessException ix) {
      throw new RuntimeException(ix);
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
  }

  private void initializeActiveEntityPanel() {
    if (mainApplicationPanels.size() > 1)
      ((EntityPanel) applicationTabPane.getSelectedComponent()).initialize();
    else
      ((EntityPanel) getComponent(0)).initialize();
  }

  private static String getUserInfo(final User user, final String dbSid) {
    return getUsername(user.getUsername().toUpperCase()) + (dbSid != null ? "@" + dbSid.toUpperCase() : "");
  }

  private static void initializeApplicationPanel(final EntityApplicationPanel applicationPanel) throws UserException {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      public void uncaughtException(Thread t, Throwable e) {
        FrameworkUiUtil.handleException(e, null, applicationPanel);
      }
    });

    applicationPanel.initialize();
  }

  private JScrollPane initializeApplicationTree() {
    final JTree tree = new JTree(getModel().getApplicationTreeModel());
    tree.setShowsRootHandles(true);
    tree.setToggleClickCount(1);
    tree.setRootVisible(false);
    UiUtil.expandAll(tree, new TreePath(tree.getModel().getRoot()), true);

    return new JScrollPane(tree,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
  }

  /**
   * Initializes a JFrame according to the given parameters
   * @param applicationIcon the frame icon
   * @param title the title string for the JFrame
   * @param maximize if true then the JFrame is maximized, overrides the prefSeizeAsRatioOfScreen parameter
   * @param northToolBar true if a toolbar should be included
   * @param showMenuBar true if a menubar should be created
   * @param size if the JFrame is not maximed then it's preferredSize is set to this value
   * @return an initialized, but non-visible JFrame
   * @throws org.jminor.common.model.UserException in case of a user exception
   */
  private JFrame prepareFrame(final ImageIcon applicationIcon, final String title, final boolean maximize, final boolean northToolBar,
                              final boolean showMenuBar, final Dimension size) throws UserException {

    final JFrame frame = UiUtil.createFrame(applicationIcon != null ? applicationIcon.getImage() : null);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        try {
          exit();
        }
        catch(UserCancelException uc) {/**/}
      }
    });

    frame.setTitle(title);
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(this, BorderLayout.CENTER);
    if (showMenuBar) {
      frame.setJMenuBar(createMenuBar());
    }
    if (northToolBar) {
      final JToolBar toolbar = getNorthToolBar();
      if (toolbar != null)
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
    if (maximize)
      frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

    return frame;
  }

  private JMenuBar createMenuBar() throws UserException {
    final List<ControlSet> menuControlSets = new ArrayList<ControlSet>();
    menuControlSets.add(getFileControlSet());
    menuControlSets.add(getViewControlSet());
    menuControlSets.add(getToolsControlSet());
    final ControlSet supportModelcontrolSet = getSupportModelControlSet();
    if (supportModelcontrolSet != null)
      menuControlSets.add(supportModelcontrolSet);
    final ControlSet[] additionalMenus = getAdditionalMenuControlSet();
    if (additionalMenus != null && additionalMenus.length > 0)
      menuControlSets.addAll(Arrays.asList(additionalMenus));

    menuControlSets.add(getHelpControlSet());

    return ControlProvider.createMenuBar(menuControlSets.toArray(new ControlSet[menuControlSets.size()]));
  }

  private static void showEntityPanelDialog(final EntityPanel.EntityPanelInfo appInfo, final IEntityDbProvider dbProvider,
                                            final JPanel owner) throws UserException {
    showEntityPanelDialog(appInfo, dbProvider, owner, false);
  }

  private static void showEntityPanelDialog(final EntityPanel.EntityPanelInfo appInfo, final IEntityDbProvider dbProvider,
                                            final JPanel owner, final boolean modalDialog) throws UserException {
    final JDialog dialog;
    try {
      UiUtil.setWaitCursor(true, owner);
      final EntityPanel entityPanel = appInfo.getInstance(dbProvider);
      entityPanel.initialize();
      dialog = new JDialog(UiUtil.getParentWindow(owner), appInfo.getCaption());
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
