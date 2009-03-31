/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.UserMonitor;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 12:23:17
 */
public class UserMonitorPanel  extends JPanel implements IPopupProvider {

  private final UserMonitor model;
  private JPopupMenu popupMenu;

  public UserMonitorPanel(final UserMonitor model) {
    this.model = model;
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
