/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.LoginDialogBuilder.LoginValidator;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
import static is.codion.common.model.UserPreferences.getUserPreference;
import static is.codion.swing.common.ui.Utilities.getParentWindow;
import static java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager;
import static java.lang.Integer.parseInt;
import static java.util.Objects.requireNonNull;

final class DefaultEntityApplicationBuilder<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> implements EntityApplicationBuilder<M, P> {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultEntityApplicationBuilder.class);

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(EntityApplicationPanel.class.getName());

  private static final String CODION_APPLICATION_VERSION = "codion.application.version";
  private static final int DEFAULT_LOGO_SIZE = 68;
  private static final String DASH = " - ";

  private final Class<M> applicationModelClass;
  private final Class<P> applicationPanelClass;

  private final String applicationDefaultUsernameProperty;
  private final String applicationLookAndFeelProperty;
  private final String applicationFontSizeProperty;

  private String applicationName = "";
  private ConnectionProviderFactory connectionProviderFactory = new DefaultConnectionProviderFactory();
  private Function<EntityConnectionProvider, M> applicationModelFactory = new DefaultApplicationModelFactory();
  private Function<M, P> applicationPanelFactory = new DefaultApplicationPanelFactory();
  private Function<M, String> frameTitleFactory = new DefaultFrameTitleFactory();

  private LoginProvider loginProvider = new DefaultDialogLoginProvider();
  private Supplier<JFrame> frameSupplier = JFrame::new;
  private boolean displayStartupDialog = EntityApplicationPanel.SHOW_STARTUP_DIALOG.get();
  private ImageIcon applicationIcon = FrameworkIcons.instance().logo(DEFAULT_LOGO_SIZE);
  private Version applicationVersion;
  private boolean saveDefaultUsername = EntityApplicationModel.SAVE_DEFAULT_USERNAME.get();
  private Supplier<JComponent> loginPanelSouthComponentSupplier = () -> null;
  private EventDataListener<P> onApplicationStarted;

  private String defaultLookAndFeelClassName = Utilities.systemLookAndFeelClassName();
  private String lookAndFeelClassName;
  private boolean maximizeFrame = false;
  private boolean displayFrame = true;
  private boolean includeMainMenu = true;
  private Dimension frameSize;
  private boolean loginRequired = EntityApplicationModel.AUTHENTICATION_REQUIRED.get();
  private User defaultLoginUser;
  private User automaticLoginUser;

  DefaultEntityApplicationBuilder(Class<M> applicationModelClass, Class<P> applicationPanelClass) {
    this.applicationModelClass = requireNonNull(applicationModelClass);
    this.applicationPanelClass = requireNonNull(applicationPanelClass);
    this.applicationDefaultUsernameProperty = EntityApplicationPanel.DEFAULT_USERNAME_PROPERTY + "#" + applicationPanelClass.getSimpleName();
    this.applicationLookAndFeelProperty = EntityApplicationPanel.LOOK_AND_FEEL_PROPERTY + "#" + applicationPanelClass.getSimpleName();
    this.applicationFontSizeProperty = EntityApplicationPanel.FONT_SIZE_PROPERTY + "#" + applicationPanelClass.getSimpleName();
    this.defaultLoginUser = User.user(getUserPreference(applicationDefaultUsernameProperty,
            EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name")));
  }

  @Override
  public EntityApplicationBuilder<M, P> applicationIcon(ImageIcon applicationIcon) {
    this.applicationIcon = requireNonNull(applicationIcon);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> defaultLookAndFeelClassName(String defaultLookAndFeelClassName) {
    this.defaultLookAndFeelClassName = requireNonNull(defaultLookAndFeelClassName);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> lookAndFeelClassName(String lookAndFeelClassName) {
    this.lookAndFeelClassName = requireNonNull(lookAndFeelClassName);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> applicationName(String applicationName) {
    this.applicationName = requireNonNull(applicationName);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> applicationVersion(Version applicationVersion) {
    this.applicationVersion = requireNonNull(applicationVersion);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> applicationModelFactory(Function<EntityConnectionProvider, M> applicationModelFactory) {
    this.applicationModelFactory = requireNonNull(applicationModelFactory);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> applicationPanelFactory(Function<M, P> applicationPanelFactory) {
    this.applicationPanelFactory = requireNonNull(applicationPanelFactory);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> defaultLoginUser(User defaultLoginUser) {
    this.defaultLoginUser = requireNonNull(defaultLoginUser);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> automaticLoginUser(User automaticLoginUser) {
    this.automaticLoginUser = automaticLoginUser;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> loginProvider(LoginProvider loginProvider) {
    this.loginProvider = requireNonNull(loginProvider);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> saveDefaultUsername(boolean saveDefaultUsername) {
    this.saveDefaultUsername = saveDefaultUsername;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> frameSupplier(Supplier<JFrame> frameSupplier) {
    this.frameSupplier = requireNonNull(frameSupplier);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> frameTitleFactory(Function<M, String> frameTitleFactory) {
    this.frameTitleFactory = requireNonNull(frameTitleFactory);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> includeMainMenu(boolean includeMainMenu) {
    this.includeMainMenu = includeMainMenu;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> maximizeFrame(boolean maximizeFrame) {
    this.maximizeFrame = maximizeFrame;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> displayFrame(boolean displayFrame) {
    this.displayFrame = displayFrame;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> displayStartupDialog(boolean displayStartupDialog) {
    this.displayStartupDialog = displayStartupDialog;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> frameSize(Dimension frameSize) {
    this.frameSize = frameSize;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> loginRequired(boolean loginRequired) {
    this.loginRequired = loginRequired;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> loginPanelSouthComponentSupplier(Supplier<JComponent> loginPanelSouthComponentSupplier) {
    this.loginPanelSouthComponentSupplier = requireNonNull(loginPanelSouthComponentSupplier);
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> onApplicationStarted(EventDataListener<P> onApplicationStarted) {
    this.onApplicationStarted = onApplicationStarted;
    return this;
  }

  @Override
  public EntityApplicationBuilder<M, P> connectionProviderFactory(ConnectionProviderFactory connectionProviderFactory) {
    this.connectionProviderFactory = requireNonNull(connectionProviderFactory);
    return this;
  }

  @Override
  public void start() {
    SwingUtilities.invokeLater(this::startApplication);
  }

  private void startApplication() {
    LOG.debug("{} application starting", applicationName);
    Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> displayExceptionAndExit(exception));
    setVersionProperty();
    LookAndFeelProvider.findLookAndFeelProvider(lookAndFeelClassName()).ifPresent(LookAndFeelProvider::enable);
    int fontSizePercentage = fontSizePercentage();
    if (fontSizePercentage != 100) {
      Utilities.setFontSizePercentage(fontSizePercentage);
      FrameworkIcons.ICON_SIZE.set(Math.round(FrameworkIcons.ICON_SIZE.get() * (fontSizePercentage / 100f)));
    }

    EntityConnectionProvider connectionProvider = connectionProvider(user());
    long initializationStarted = System.currentTimeMillis();
    if (displayStartupDialog) {
      int borderSize = Layouts.HORIZONTAL_VERTICAL_GAP.get();
      Dialogs.progressWorkerDialog(() -> applicationModel(connectionProvider))
              .title(applicationName)
              .icon(applicationIcon)
              .border(BorderFactory.createEmptyBorder(borderSize, borderSize, borderSize, borderSize))
              .westPanel(createStartupIconPanel())
              .onResult(applicationModel -> startApplication(applicationModel, initializationStarted))
              .onException(DefaultEntityApplicationBuilder::displayExceptionAndExit)
              .execute();
    }
    else {
      startApplication(applicationModel(connectionProvider), initializationStarted);
    }
  }

  private String lookAndFeelClassName() {
    if (lookAndFeelClassName != null) {
      return lookAndFeelClassName;
    }

    return getUserPreference(applicationLookAndFeelProperty, defaultLookAndFeelClassName);
  }

  private int fontSizePercentage() {
    return parseInt(getUserPreference(applicationFontSizeProperty, "100"));
  }

  /**
   * Sets the application version as a system property, so that it appears automatically in exception dialogs.
   */
  private void setVersionProperty() {
    if (applicationVersion != null) {
      System.setProperty(CODION_APPLICATION_VERSION, applicationVersion.toString());
    }
  }

  private void startApplication(M applicationModel, long initializationStarted) {
    P applicationPanel = applicationPanel(applicationModel);
    applicationPanel.initializePanel();

    JFrame applicationFrame = applicationFrame(applicationPanel);
    applicationModel.connectionValidObserver().addDataListener(connectionValid ->
            SwingUtilities.invokeLater(() -> applicationFrame.setTitle(frameTitle(applicationModel))));

    Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> displayException(exception, applicationFrame));
    LOG.info(applicationFrame.getTitle() + ", application started successfully: " + (System.currentTimeMillis() - initializationStarted) + " ms");
    if (displayFrame) {
      applicationFrame.setVisible(true);
    }
    if (onApplicationStarted != null) {
      onApplicationStarted.onEvent(applicationPanel);
    }
  }

  private User user() {
    if (automaticLoginUser != null) {
      return automaticLoginUser;
    }
    if (!loginRequired) {
      return null;
    }

    User user = loginProvider.login();
    if (saveDefaultUsername) {
      UserPreferences.setUserPreference(applicationDefaultUsernameProperty, user.username());
    }

    return user;
  }

  private EntityConnectionProvider connectionProvider(User user) {
    if (loginProvider instanceof DefaultEntityApplicationBuilder.DefaultDialogLoginProvider &&
            ((DefaultDialogLoginProvider) loginProvider).loginValidator.connectionProvider != null) {
      return ((DefaultDialogLoginProvider) loginProvider).loginValidator.connectionProvider;
    }

    return initializeConnectionProvider(user, applicationPanelClass.getName(), applicationVersion);
  }

  private M applicationModel(EntityConnectionProvider connectionProvider) {
    return applicationModelFactory.apply(connectionProvider);
  }

  private P applicationPanel(M applicationModel) {
    return applicationPanelFactory.apply(applicationModel);
  }

  private JFrame applicationFrame(P applicationPanel) {
    JFrame frame = frameSupplier.get();
    frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    frame.setIconImage(applicationIcon.getImage());
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        try {
          applicationPanel.exit();
        }
        catch (CancelException ignored) {/*ignored*/}
      }
    });
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(applicationPanel, BorderLayout.CENTER);
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
    frame.setTitle(frameTitle(applicationPanel.applicationModel()));
    if (includeMainMenu) {
      JMenuBar menuBar = applicationPanel.createMenuBar();
      if (menuBar != null) {
        frame.setJMenuBar(menuBar);
      }
    }
    frame.setAlwaysOnTop(applicationPanel.alwaysOnTopState().get());

    return frame;
  }

  private JPanel createStartupIconPanel() {
    JPanel panel = new JPanel(new BorderLayout());
    if (applicationIcon != null) {
      panel.add(new JLabel(applicationIcon), BorderLayout.CENTER);
    }

    return panel;
  }

  private String frameTitle(M applicationModel) {
    String title = frameTitleFactory.apply(applicationModel);

    return applicationModel.connectionValidObserver().get() ? title : title + DASH + RESOURCE_BUNDLE.getString("not_connected");
  }

  private EntityConnectionProvider initializeConnectionProvider(User user, String clientTypeId, Version clientVersion) {
    return connectionProviderFactory.create(user, clientTypeId, clientVersion);
  }

  private static void displayExceptionAndExit(Throwable exception) {
    if (exception instanceof CancelException) {
      System.exit(0);
    }
    else {
      displayException(exception, null);
      System.exit(1);
    }
  }

  private static void displayException(Throwable exception, JFrame applicationFrame) {
    Window focusOwnerParentWindow = getParentWindow(getCurrentKeyboardFocusManager().getFocusOwner());
    Dialogs.displayExceptionDialog(exception, focusOwnerParentWindow == null ? applicationFrame : focusOwnerParentWindow);
  }

  private final class DefaultDialogLoginProvider implements LoginProvider {

    private final DefaultLoginValidator loginValidator = new DefaultLoginValidator();

    @Override
    public User login() {
      return Dialogs.loginDialog()
              .defaultUser(defaultLoginUser)
              .validator(loginValidator)
              .title(loginDialogTitle())
              .icon(applicationIcon)
              .southComponent(loginPanelSouthComponentSupplier.get())
              .show();
    }

    private String loginDialogTitle() {
      StringBuilder builder = new StringBuilder(applicationName);
      if (builder.length() > 0 && applicationVersion != null) {
        builder.append(DASH).append(applicationVersion);
      }
      if (builder.length() > 0) {
        builder.append(DASH);
      }

      return builder.append(Messages.login()).toString();
    }
  }

  private final class DefaultLoginValidator implements LoginValidator {

    private EntityConnectionProvider connectionProvider;

    @Override
    public void validate(User user) throws Exception {
      connectionProvider = initializeConnectionProvider(user, applicationPanelClass.getName(), applicationVersion);
      try {
        connectionProvider.connection();//throws exception if the server is not reachable
      }
      catch (Exception e) {
        connectionProvider.close();
        connectionProvider = null;
        throw e;
      }
    }
  }

  private class DefaultApplicationModelFactory implements Function<EntityConnectionProvider, M> {

    @Override
    public M apply(EntityConnectionProvider connectionProvider) {
      try {
        return applicationModelClass.getConstructor(EntityConnectionProvider.class).newInstance(connectionProvider);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class DefaultApplicationPanelFactory implements Function<M, P> {

    @Override
    public P apply(M model) {
      try {
        return applicationPanelClass.getConstructor(model.getClass()).newInstance(model);
      }
      catch (RuntimeException e) {
        throw e;
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class DefaultFrameTitleFactory implements Function<M, String> {

    @Override
    public String apply(M applicationModel) {
      StringBuilder builder = new StringBuilder(applicationName);
      if (applicationVersion != null) {
        if (builder.length() > 0) {
          builder.append(DASH);
        }
        builder.append(applicationVersion);
      }
      if (builder.length() > 0) {
        builder.append(DASH);
      }
      builder.append(userInfo(applicationModel.connectionProvider()));

      return builder.toString();
    }

    private String userInfo(EntityConnectionProvider connectionProvider) {
      String description = connectionProvider.description();

      return removeUsernamePrefix(connectionProvider.user().username().toUpperCase()) + (description != null ? "@" + description.toUpperCase() : "");
    }

    private String removeUsernamePrefix(String username) {
      String usernamePrefix = EntityApplicationModel.USERNAME_PREFIX.get();
      if (!nullOrEmpty(usernamePrefix) && username.toUpperCase().startsWith(usernamePrefix.toUpperCase())) {
        return username.substring(usernamePrefix.length());
      }

      return username;
    }
  }

  private static final class DefaultConnectionProviderFactory implements ConnectionProviderFactory {}
}
