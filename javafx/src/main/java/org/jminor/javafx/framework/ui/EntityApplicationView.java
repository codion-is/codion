/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.ExceptionUtil;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityApplicationModel;

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

public abstract class EntityApplicationView<Model extends EntityApplicationModel> extends Application implements ViewTreeNode {

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationView.class);

  private static final String DEFAULT_ICON_FILE_NAME = "jminor_logo32.gif";

  private final String applicationTitle;
  private final String iconFileName;

  private final List<EntityView> entityViews = new ArrayList<>();

  private Model model;
  private Stage mainStage;

  public EntityApplicationView(final String applicationTitle) {
    this(applicationTitle, DEFAULT_ICON_FILE_NAME);
  }

  public EntityApplicationView(final String applicationTitle, final String iconFileName) {
    this.applicationTitle = applicationTitle;
    this.iconFileName = iconFileName;
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> handleException((Exception) throwable));
  }

  public final Model getModel() {
    return model;
  }

  public final void addEntityView(final EntityView entityView) {
    entityViews.add(entityView);
    entityView.setParentView(this);
  }

  @Override
  public final ViewTreeNode getParentView() {
    return null;
  }

  @Override
  public final ViewTreeNode getPreviousSiblingView() {
    return null;
  }

  @Override
  public final ViewTreeNode getNextSiblingView() {
    return null;
  }

  @Override
  public final List<? extends ViewTreeNode> getChildViews() {
    return entityViews;
  }

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
      initializeEntitieViews();
      final Scene applicationScene = initializeApplicationScene(stage);
      stage.setOnCloseRequest(event -> savePreferences());
      stage.setScene(applicationScene);

      stage.show();
    }
    catch (final Exception e) {
      handleException(e);
      stage.close();
    }
  }

  protected EntityConnectionProvider initializeConnectionProvider(final User user, final String clientTypeID) {
    return EntityConnectionProviders.connectionProvider(user, clientTypeID);
  }

  protected User getDefaultUser() {
    final String defaultUserName = Configuration.getValue(Configuration.USERNAME_PREFIX) + System.getProperty("user.name");

    return new User(defaultUserName, "");
  }

  protected String getApplicationIdentifier() {
    return getClass().getName();
  }

  protected MenuBar createMainMenu() {
    final MenuBar menuBar = new MenuBar();
    final Menu file = new Menu(FrameworkMessages.get(FrameworkMessages.FILE));
    final MenuItem exit = new MenuItem(FrameworkMessages.get(FrameworkMessages.EXIT));
    exit.setOnAction(event -> mainStage.close());
    file.getItems().add(exit);
    menuBar.getMenus().add(file);

    return menuBar;
  }

  protected final User getApplicationUser() {
    if (Configuration.getBooleanValue(Configuration.AUTHENTICATION_REQUIRED)) {
      return showLoginPanel(getDefaultUser());
    }

    return getDefaultUser();
  }

  protected final User showLoginPanel(final User defaultUser) {
    return FXUiUtil.showLoginDialog(applicationTitle, defaultUser,
            new ImageView(new Image(EntityApplicationView.class.getResourceAsStream(iconFileName))));
  }

  /**
   * Called on application exit, override to save user preferences on program exit,
   * remember to call super.savePreferences() when overriding
   */
  protected void savePreferences() {
    entityViews.forEach(EntityView::savePreferences);
  }

  protected abstract void initializeEntitieViews();

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

  protected abstract Model initializeApplicationModel(final EntityConnectionProvider connectionProvider);

  private void handleException(final Exception e) {
    if (e instanceof CancelException) {
      return;
    }
    FXUiUtil.showExceptionDialog(ExceptionUtil.unwrapAndLog(e, RuntimeException.class, LOG));
  }
}
