/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.EntityConnectionProviders;
import org.jminor.javafx.framework.model.EntityApplicationModel;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EntityApplicationView<Model extends EntityApplicationModel> extends Application {

  private static final Logger LOG = LoggerFactory.getLogger(EntityApplicationView.class);

  private static final String DEFAULT_ICON_FILE_NAME = "jminor_logo32.gif";

  private final String applicationTitle;
  private final String iconFileName;

  private Model model;

  public EntityApplicationView(final String applicationTitle) {
    this(applicationTitle, DEFAULT_ICON_FILE_NAME);
  }

  public EntityApplicationView(final String applicationTitle, final String iconFileName) {
    this.applicationTitle = applicationTitle;
    this.iconFileName = iconFileName;
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> handleException(throwable));
  }

  public final Model getModel() {
    return model;
  }

  @Override
  public final void start(final Stage stage) {
    try {
      final User user = getApplicationUser();
      final EntityConnectionProvider connectionProvider = initializeConnectionProvider(user, getApplicationIdentifier());
      connectionProvider.getConnection();//throws exception if the server is not reachable or credentials are incorrect
      this.model = initializeApplicationModel(connectionProvider);
      stage.setTitle(applicationTitle);
      stage.getIcons().add(new Image(EntityApplicationView.class.getResourceAsStream(iconFileName)));
      stage.setScene(initializeApplicationScene(stage));

      stage.show();
    }
    catch (final Exception e) {
      handleException(Util.unwrapAndLog(e, RuntimeException.class, LOG));
      stage.close();
    }
  }

  protected User getApplicationUser() {
    return showLoginPanel();
  }

  protected EntityConnectionProvider initializeConnectionProvider(final User user, final String clientTypeID) {
    return EntityConnectionProviders.connectionProvider(user, clientTypeID);
  }

  protected String getApplicationIdentifier() {
    return getClass().getName();
  }

  protected final User showLoginPanel() {
    final String defaultUserName = Configuration.getValue(Configuration.USERNAME_PREFIX) + System.getProperty("user.name");

    return EntityUiUtil.showLoginDialog(applicationTitle, defaultUserName,
            new ImageView(new Image(EntityApplicationView.class.getResourceAsStream(iconFileName))));
  }

  protected abstract Scene initializeApplicationScene(final Stage primaryStage) throws DatabaseException;

  protected abstract Model initializeApplicationModel(final EntityConnectionProvider connectionProvider);

  private void handleException(final Throwable throwable) {
    if (throwable instanceof CancelException) {
      return;
    }
    EntityUiUtil.showExceptionDialog(throwable);
  }
}
