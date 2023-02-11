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
 * @see EntityApplicationBuilder#entityApplicationBuilder(Class, Class)
 * @see #start()
 */
public interface EntityApplicationBuilder<M extends SwingEntityApplicationModel> {

  /**
   * @param applicationName the application name
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> applicationName(String applicationName);

  /**
   * @param applicationIcon the application icon
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> applicationIcon(ImageIcon applicationIcon);

  /**
   * @param applicationVersion the application version
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> applicationVersion(Version applicationVersion);

  /**
   * Sets the default look and feel classname, used in case no look and feel settings are found in user preferences.
   * @param defaultLookAndFeelClassName the default look and feel classname
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> defaultLookAndFeelClassName(String defaultLookAndFeelClassName);

  /**
   * Sets the look and feel classname, overrides any look and feel settings found in user preferences.
   * Note that for the given look to be enabled it must be made available via
   * {@link is.codion.swing.common.ui.laf.LookAndFeelProvider#addLookAndFeelProvider(LookAndFeelProvider)}
   * before starting the application.
   * @param lookAndFeelClassName the look and feel classname
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> lookAndFeelClassName(String lookAndFeelClassName);

  /**
   * @param connectionProviderFactory the connection provider factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> connectionProviderFactory(ConnectionProviderFactory connectionProviderFactory);

  /**
   * @param modelFactory the application model factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> modelFactory(Function<EntityConnectionProvider, M> modelFactory);

  /**
   * @param panelFactory the application panel factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> panelFactory(Function<M, ? extends EntityApplicationPanel<M>> panelFactory);

  /**
   * @param loginProvider provides a way for a user to login
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> loginProvider(LoginProvider loginProvider);

  /**
   * @param defaultLoginUser the default user credentials to display in the login dialog
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> defaultLoginUser(User defaultLoginUser);

  /**
   * @param automaticLoginUser if specified the application is started automatically with the given user,
   * instead of displaying a login dialog
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> automaticLoginUser(User automaticLoginUser);

  /**
   * @param saveDefaultUsername true if the username should be saved in user preferences after a successful login
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> saveDefaultUsername(boolean saveDefaultUsername);

  /**
   * Note that this does not apply when a custom {@link LoginProvider} has been specified.
   * @param loginPanelSouthComponentSupplier supplies the component to add to the
   * {@link java.awt.BorderLayout#SOUTH} position of the default login panel
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> loginPanelSouthComponentSupplier(Supplier<JComponent> loginPanelSouthComponentSupplier);

  /**
   * @param onApplicationStarted called after a successful application start
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> onApplicationStarted(EventDataListener<EntityApplicationPanel<M>> onApplicationStarted);

  /**
   * @param frameSupplier the frame supplier
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> frameSupplier(Supplier<JFrame> frameSupplier);

  /**
   * @param frameTitleFactory the frame title factory
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> frameTitleFactory(Function<M, String> frameTitleFactory);

  /**
   * @param includeMainMenu if true then a main menu is included
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> includeMainMenu(boolean includeMainMenu);

  /**
   * @param maximizeFrame specifies whether the frame should be maximized or use it's preferred size
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> maximizeFrame(boolean maximizeFrame);

  /**
   * @param displayFrame specifies whether the frame should be displayed or left invisible
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> displayFrame(boolean displayFrame);

  /**
   * @param displayProgressDialog if true then a progress dialog is displayed while the application is being initialized
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> displayStartupDialog(boolean displayProgressDialog);

  /**
   * @param frameSize the frame size when not maximized
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> frameSize(Dimension frameSize);

  /**
   * If this is set to false, the {@link #connectionProviderFactory(ConnectionProviderFactory)}
   * {@link User} argument will be null.
   * @param loginRequired true if a user login is required for this application, false if the user is supplied differently
   * @return this Builder instance
   */
  EntityApplicationBuilder<M> loginRequired(boolean loginRequired);

  /**
   * Starts the application, should be called on the Event Dispatch Thread
   */
  void start();

  /**
   * @param <M> the application model type
   * @param modelClass the application model class
   * @param panelClass the application panel class
   * @return a {@link EntityApplicationBuilder}
   */
  static <M extends SwingEntityApplicationModel> EntityApplicationBuilder<M> entityApplicationBuilder(
          Class<M> modelClass, Class<?extends EntityApplicationPanel<M>> panelClass) {
    return new DefaultEntityApplicationBuilder<>(modelClass, panelClass);
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
    EntityConnectionProvider create(User user, String clientTypeId, Version clientVersion);
  }
}
