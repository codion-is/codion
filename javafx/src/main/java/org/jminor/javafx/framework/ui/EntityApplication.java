/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.db.exception.DatabaseException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public abstract class EntityApplication extends Application {

  private final String applicationTitle;

  public EntityApplication(final String applicationTitle) {
    this.applicationTitle = applicationTitle;
    Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
      handleException(throwable);
    });
  }

  @Override
  public final void start(final Stage stage) throws Exception {
    stage.setTitle(applicationTitle);
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
