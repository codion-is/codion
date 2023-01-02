/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.framework.model.EntityApplicationModel;
import is.codion.swing.common.ui.Windows;

import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

final class EntityApplicationPanelStarter implements EntityApplicationPanel.Starter {

  private final EntityApplicationPanel<?> applicationPanel;

  private boolean includeMainMenu = true;
  private boolean maximizeFrame = false;
  private boolean displayFrame = true;
  private boolean displayProgressDialog = true;
  private Dimension frameSize = Windows.screenSizeRatio(0.5);
  private boolean loginRequired = EntityApplicationModel.AUTHENTICATION_REQUIRED.get();
  private User defaultLoginUser;
  private User silentLoginUser;

  EntityApplicationPanelStarter(EntityApplicationPanel<?> applicationPanel) {
    this.applicationPanel = applicationPanel;
  }

  @Override
  public EntityApplicationPanel.Starter includeMainMenu(boolean includeMainMenu) {
    this.includeMainMenu = includeMainMenu;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter maximizeFrame(boolean maximizeFrame) {
    this.maximizeFrame = maximizeFrame;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter displayFrame(boolean displayFrame) {
    this.displayFrame = displayFrame;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter displayProgressDialog(boolean displayProgressDialog) {
    this.displayProgressDialog = displayProgressDialog;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter frameSize(Dimension frameSize) {
    this.frameSize = requireNonNull(frameSize);
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter loginRequired(boolean loginRequired) {
    this.loginRequired = loginRequired;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter defaultLoginUser(User defaultLoginUser) {
    this.defaultLoginUser = defaultLoginUser;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter silentLoginUser(User silentLoginUser) {
    this.silentLoginUser = silentLoginUser;
    return this;
  }

  @Override
  public void start() {
    applicationPanel.startApplication(defaultLoginUser, silentLoginUser, loginRequired, frameSize, maximizeFrame,
            displayFrame, includeMainMenu, displayProgressDialog);
  }
}
