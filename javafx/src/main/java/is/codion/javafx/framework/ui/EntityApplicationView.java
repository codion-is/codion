/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.javafx.framework.model.FXEntityApplicationModel;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * A root application View.
 * @param <M> the type of {@link FXEntityApplicationModel} used by this application view
 */
public abstract class EntityApplicationView<M extends FXEntityApplicationModel>
        extends Application implements ViewTreeNode<EntityView> {

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationView.class);

  private static final String DEFAULT_ICON_FILE_NAME = "codion-logo-black-48x48.png";

  private final String applicationTitle;
  private final String iconFileName;

  private final List<EntityView> entityViews = new ArrayList<>();

  private M model;
  private Stage mainStage;

  /**
   * Instantiates a new {@link EntityApplicationView} instance.
   * @param applicationTitle the title to display in the view header
   */
  public EntityApplicationView(final String applicationTitle) {
    this(applicationTitle, DEFAULT_ICON_FILE_NAME);
  }

  /**
   * Instantiates a new {@link EntityApplicationView} instance.
   * @param applicationTitle the title to display in the view header
   * @param iconFileName the name of a icon file on the classpath to display
   */
  public EntityApplicationView(final String applicationTitle, final String iconFileName) {
    this.applicationTitle = applicationTitle;
    this.iconFileName = iconFileName;
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> onException((Exception) throwable));
  }

  /**
   * @return the application model
   */
  public final M getModel() {
    if (model == null) {
      throw new IllegalStateException("The application model is only available after the application has been started");
    }
    return model;
  }

  /**
   * Adds a {@link EntityView} to this application
   * @param entityView the {@link EntityView} to add
   */
  public final void addEntityView(final EntityView entityView) {
    entityViews.add(entityView);
    entityView.setParentView(this);
  }

  /**
   * @return null, since {@link EntityApplicationView} does not have a parent view
   */
  @Override
  public final ViewTreeNode<EntityView> getParentView() {
    return null;
  }

  /**
   * @return null, since {@link EntityApplicationView} does not have a previous sibling view
   */
  @Override
  public final EntityView getPreviousSiblingView() {
    return null;
  }

  /**
   * @return null, since {@link EntityApplicationView} does not have a next sibling view
   */
  @Override
  public final EntityView getNextSiblingView() {
    return null;
  }

  /**
   * @return the {@link EntityView}s associated with this application
   * @see #addEntityView(EntityView)
   */
  @Override
  public final List<EntityView> getChildViews() {
    return entityViews;
  }

  /**
   * Starts this application
   * @param stage the State on which to set this application
   */
  @Override
  public final void start(final Stage stage) {
    try {
      this.mainStage = stage;
      final User user = getApplicationUser();
      final EntityConnectionProvider connectionProvider = initializeConnectionProvider(user, getApplicationIdentifier());
      connectionProvider.getConnection();//throws exception if the server is not reachable or credentials are incorrect
      this.model = initializeApplicationModel(connectionProvider);
      stage.setTitle(applicationTitle);
      stage.getIcons().add(new Image(EntityApplicationView.class.getResourceAsStream(iconFileName)));
      initializeEntityViews();
      final Scene applicationScene = initializeApplicationScene(stage);
      stage.setOnCloseRequest(event -> savePreferences());
      stage.setScene(applicationScene);

      stage.show();
    }
    catch (final Exception e) {
      onException(e);
      stage.close();
    }
  }

  /**
   * Initializes the connection provider to use in this application
   * @param user the user on which to base the connection
   * @param clientTypeId a String identifying the client type
   * @return a {@link EntityConnectionProvider} based on the given user and client type
   */
  protected EntityConnectionProvider initializeConnectionProvider(final User user, final String clientTypeId) {
    return EntityConnectionProvider.connectionProvider().setDomainClassName(EntityConnectionProvider.CLIENT_DOMAIN_CLASS.getOrThrow())
            .setClientTypeId(clientTypeId).setUser(user);
  }

  /**
   * @return the default user when logging into this application
   */
  protected User getDefaultUser() {
    final String defaultUserName = EntityApplicationModel.USERNAME_PREFIX.get() + System.getProperty("user.name");

    return User.user(defaultUserName);
  }

  /**
   * @return a String identifying this application, the class name by default
   */
  protected String getApplicationIdentifier() {
    return getClass().getName();
  }

  /**
   * @return the main menu for this application
   */
  protected MenuBar createMainMenu() {
    final MenuBar menuBar = new MenuBar();
    final Menu file = new Menu(FrameworkMessages.get(FrameworkMessages.FILE));
    final MenuItem exit = new MenuItem(FrameworkMessages.get(FrameworkMessages.EXIT));
    exit.setOnAction(event -> mainStage.close());
    file.getItems().add(exit);
    menuBar.getMenus().add(file);

    return menuBar;
  }

  /**
   * Displays a login panel in case authentication is required, otherwise returns the default user.
   * @return the user to use when logging into this application.
   * @see EntityApplicationModel#AUTHENTICATION_REQUIRED
   * @see #getDefaultUser()
   */
  protected final User getApplicationUser() {
    if (EntityApplicationModel.AUTHENTICATION_REQUIRED.get()) {
      return showLoginPanel(getDefaultUser());
    }

    return getDefaultUser();
  }

  /**
   * Displays a login panel
   * @param defaultUser the default user to display
   * @return the user retrieved from the login panel
   * @throws CancelException in case the login action is cancelled
   */
  protected final User showLoginPanel(final User defaultUser) {
    return FXUiUtil.showLoginDialog(applicationTitle, defaultUser,
            new ImageView(new Image(EntityApplicationView.class.getResourceAsStream(iconFileName))));
  }

  /**
   * Called on application exit, override to save user preferences on program exit,
   * remember to call super.savePreferences() when overriding
   * @see EntityApplicationModel#savePreferences()
   */
  protected void savePreferences() {
    entityViews.forEach(EntityView::savePreferences);
    getModel().savePreferences();
  }

  /**
   * Initialized all entity views and adds them via {@link #addEntityView(EntityView)}
   */
  protected abstract void initializeEntityViews();

  /**
   * Creates the application scene from the available {@link EntityView}s.
   * @param primaryStage the primary stage
   * @return the application scene
   */
  protected Scene initializeApplicationScene(final Stage primaryStage) {
    if (entityViews.isEmpty()) {
      throw new IllegalStateException("No entity views have been added");
    }
    final TabPane tabPane = new TabPane();
    for (final EntityView entityView : entityViews) {
      entityView.initializePanel();
      tabPane.getTabs().add(new Tab(entityView.getCaption(), entityView));
    }

    return new Scene(tabPane);
  }

  /**
   * Initializes the application model
   * @param connectionProvider the connection provider
   * @return the application model
   */
  protected abstract M initializeApplicationModel(EntityConnectionProvider connectionProvider);

  private static void onException(final Exception e) {
    if (e instanceof CancelException) {
      return;
    }
    LOG.error(e.getMessage(), e);
    FXUiUtil.showExceptionDialog(e);
  }
}
