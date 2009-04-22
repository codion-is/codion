/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.UserInstanceMonitor;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:31:45
 */
public class UserInstanceMonitorPanel extends JPanel implements IPopupProvider {

  private final UserInstanceMonitor model;
  private JPopupMenu popupMenu;

  public UserInstanceMonitorPanel(final UserInstanceMonitor model) {
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
