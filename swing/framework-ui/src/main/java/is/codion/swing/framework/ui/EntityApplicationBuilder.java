/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.common.version.Version;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Builds a {@link EntityApplicationPanel} and starts the application.
 * @param <M> the application model type
 * @param <P> the application panel type
 * @see EntityApplicationBuilder#entityApplicationBuilder(Class, Class)
 * @see #start()
 */
public interface EntityApplicationBuilder<M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> {

  /**
   * @param applicationName the application name
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> applicationName(String applicationName);

  /**
   * @param applicationIcon the application icon
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> applicationIcon(ImageIcon applicationIcon);

  /**
   * @param applicationVersion the application version
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> applicationVersion(Version applicationVersion);

  /**
   * Sets the default look and feel classname, used in case no look and feel settings are found in user preferences.
   * Note that for an external Look and Feels to be enabled, it must be registered via
   * {@link is.codion.swing.common.ui.laf.LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)}
   * before starting the application.
   * @param defaultLookAndFeelClassName the default look and feel classname
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> defaultLookAndFeelClassName(String defaultLookAndFeelClassName);

  /**
   * Sets the look and feel classname, overrides any look and feel settings found in user preferences.
   * Note that for an external Look and Feels to be enabled, it must be registered via
   * {@link is.codion.swing.common.ui.laf.LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)}
   * before starting the application.
   * @param lookAndFeelClassName the look and feel classname
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> lookAndFeelClassName(String lookAndFeelClassName);

  /**
   * @param connectionProviderFactory the connection provider factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> connectionProviderFactory(ConnectionProviderFactory connectionProviderFactory);

  /**
   * @param applicationModelFactory the application model factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> applicationModelFactory(Function<EntityConnectionProvider, M> applicationModelFactory);

  /**
   * @param applicationPanelFactory the application panel factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> applicationPanelFactory(Function<M, P> applicationPanelFactory);

  /**
   * @param loginProvider provides a way for a user to login
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> loginProvider(LoginProvider loginProvider);

  /**
   * @param defaultLoginUser the default user credentials to display in the login dialog
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> defaultLoginUser(User defaultLoginUser);

  /**
   * @param automaticLoginUser if specified the application is started automatically with the given user,
   * instead of displaying a login dialog
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> automaticLoginUser(User automaticLoginUser);

  /**
   * @param saveDefaultUsername true if the username should be saved in user preferences after a successful login
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> saveDefaultUsername(boolean saveDefaultUsername);

  /**
   * Note that this does not apply when a custom {@link LoginProvider} has been specified.
   * @param loginPanelSouthComponentSupplier supplies the component to add to the
   * {@link java.awt.BorderLayout#SOUTH} position of the default login panel
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> loginPanelSouthComponentSupplier(Supplier<JComponent> loginPanelSouthComponentSupplier);

  /**
   * Runs before the application is started, but after Look and Feel initialization.
   * Throw {@link is.codion.common.model.CancelException} in order to cancel the application startup.
   * @param beforeApplicationStarted run before the application is started
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> beforeApplicationStarted(Runnable beforeApplicationStarted);

  /**
   * @param onApplicationStarted called after a successful application start
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> onApplicationStarted(EventDataListener<P> onApplicationStarted);

  /**
   * @param frameSupplier the frame supplier
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> frameSupplier(Supplier<JFrame> frameSupplier);

  /**
   * @param frameTitleFactory the frame title factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> frameTitleFactory(Function<M, String> frameTitleFactory);

  /**
   * @param includeMainMenu if true then a main menu is included
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> includeMainMenu(boolean includeMainMenu);

  /**
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> maximizeFrame(boolean maximizeFrame);

  /**
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> displayFrame(boolean displayFrame);

  /**
   * Specifies whether or not to set the default uncaught exception handler when starting the application, true by default.
   * @param setUncaughtExceptionHandler if true the default uncaught exception handler is set on application start
   * @return this Builder instance
   * @see Thread#setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler)
   */
  EntityApplicationBuilder<M, P> setUncaughtExceptionHandler(boolean setUncaughtExceptionHandler);

  /**
   * @param displayProgressDialog if true then a progress dialog is displayed while the application is being initialized
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> displayStartupDialog(boolean displayProgressDialog);

  /**
   * @param frameSize the frame size when not maximized
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> frameSize(Dimension frameSize);

  /**
   * If this is set to false, the {@link #connectionProviderFactory(ConnectionProviderFactory)}
   * {@link User} argument will be null.
   * @param loginRequired true if a user login is required for this application, false if the user is supplied differently
   * @return this Builder instance
   */
  EntityApplicationBuilder<M, P> loginRequired(boolean loginRequired);

  /**
   * Starts the application on the Event Dispatch Thread.
   */
  void start();

  /**
   * @param <M> the application model type
   * @param <P> the application panel type
   * @param applicationModelClass the application model class
   * @param applicationPanelClass the application panel class
   * @return a {@link EntityApplicationBuilder}
   */
  static <M extends SwingEntityApplicationModel, P extends EntityApplicationPanel<M>> EntityApplicationBuilder<M, P> entityApplicationBuilder(
          Class<M> applicationModelClass, Class<P> applicationPanelClass) {
    return new DefaultEntityApplicationBuilder<>(applicationModelClass, applicationPanelClass);
  }

  /**
   * Provides a way for a user to login.
   */
  interface LoginProvider {

    /**
     * Performs the login and returns the User, may not return null.
     * @return the user, not null
     * @throws RuntimeException in case the login failed
     * @throws is.codion.common.model.CancelException in case the login is cancelled
     */
    User login();
  }

  /**
   * A factory for a {@link EntityConnectionProvider} instance.
   */
  interface ConnectionProviderFactory {

    /**
     * Creates a new {@link EntityConnectionProvider} instance.
     * @param user the user, may be null in case login is not required {@link EntityApplicationBuilder#loginRequired(boolean)}.
     * @param clientTypeId the client type id
     * @param clientVersion the client version
     * @return a new {@link EntityConnectionProvider} instance.
     */
    default EntityConnectionProvider create(User user, String clientTypeId, Version clientVersion) {
      return EntityConnectionProvider.builder()
              .domainClassName(EntityConnectionProvider.CLIENT_DOMAIN_CLASS.getOrThrow())
              .clientTypeId(clientTypeId)
              .clientVersion(clientVersion)
              .user(user)
              .build();
    }
  }
}
