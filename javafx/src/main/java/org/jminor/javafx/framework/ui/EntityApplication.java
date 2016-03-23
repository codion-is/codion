/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public abstract class EntityApplication extends Application {

  private static final String DEFAULT_ICON_FILE_NAME = "jminor_logo32.gif";

  private final String applicationTitle;
  private final String iconFileName;

  public EntityApplication(final String applicationTitle) {
    this(applicationTitle, DEFAULT_ICON_FILE_NAME);
  }

  public EntityApplication(final String applicationTitle, final String iconFileName) {
    this.applicationTitle = applicationTitle;
    this.iconFileName = iconFileName;
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> handleException(throwable));
  }

  @Override
  public final void start(final Stage stage) throws Exception {
    stage.setTitle(applicationTitle);
    stage.getIcons().add(new Image(EntityApplication.class.getResourceAsStream(iconFileName)));
    stage.setScene(initializeApplicationScene(stage));
    stage.show();
  }

  protected abstract Scene initializeApplicationScene(final Stage primaryStage) throws DatabaseException;

  private void handleException(final Throwable throwable) {
    throwable.printStackTrace();
    final Alert alert = new Alert(Alert.AlertType.ERROR, throwable.getMessage());
    alert.showAndWait();
  }
}
