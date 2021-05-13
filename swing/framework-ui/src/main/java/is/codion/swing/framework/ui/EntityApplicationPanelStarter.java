/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.user.User;
import is.codion.swing.common.ui.Windows;

import javax.swing.ImageIcon;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.net.URL;

import static is.codion.swing.common.ui.icons.Icons.icons;
import static java.util.Objects.requireNonNull;

final class EntityApplicationPanelStarter implements EntityApplicationPanel.Starter {

  private final EntityApplicationPanel<?> applicationPanel;

  private String applicationName;
  private ImageIcon applicationIcon = icons().logoTransparent();
  private boolean includeMainMenu = true;
  private boolean maximizeFrame = false;
  private boolean displayFrame = true;
  private boolean displayProgressDialog = true;
  private Dimension frameSize = Windows.getScreenSizeRatio(0.5);
  private User defaultLoginUser;
  private User silentLoginUser;

  EntityApplicationPanelStarter(final EntityApplicationPanel<?> applicationPanel) {
    this.applicationPanel = applicationPanel;
  }

  @Override
  public EntityApplicationPanel.Starter applicationName(final String applicationName) {
    this.applicationName = requireNonNull(applicationName);
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter applicationIconName(final String applicationIconName) {
    this.applicationIcon = loadIcon(requireNonNull(applicationIconName));
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter applicationIcon(final ImageIcon applicationIcon) {
    this.applicationIcon = requireNonNull(applicationIcon);
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter includeMainMenu(final boolean includeMainMenu) {
    this.includeMainMenu = includeMainMenu;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter maximizeFrame(final boolean maximizeFrame) {
    this.maximizeFrame = maximizeFrame;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter displayFrame(final boolean displayFrame) {
    this.displayFrame = displayFrame;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter displayProgressDialog(final boolean displayProgressDialog) {
    this.displayProgressDialog = displayProgressDialog;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter frameSize(final Dimension frameSize) {
    this.frameSize = requireNonNull(frameSize);
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter defaultLoginUser(final User defaultLoginUser) {
    this.defaultLoginUser = defaultLoginUser;
    return this;
  }

  @Override
  public EntityApplicationPanel.Starter silentLoginUser(final User silentLoginUser) {
    this.silentLoginUser = silentLoginUser;
    return this;
  }

  @Override
  public void start() {
    applicationPanel.startApplication(applicationName, applicationIcon,
            maximizeFrame, frameSize, defaultLoginUser, displayFrame,
            silentLoginUser, includeMainMenu, displayProgressDialog);
  }

  private ImageIcon loadIcon(final String resourceName) {
    final URL url = applicationPanel.getClass().getResource(resourceName);
    requireNonNull(url, "Resource: " + resourceName + " for " + applicationPanel.getClass());

    return new ImageIcon(Toolkit.getDefaultToolkit().getImage(url));
  }
}
