/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.HostMonitor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 18:12:28
 */
public class HostMonitorPanel extends JPanel implements IPopupProvider {

  private final HostMonitor model;

  private JPopupMenu popupMenu;

  public HostMonitorPanel(final HostMonitor model) {
    this.model = model;
    initUI();
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  private void initUI() {
    setLayout(new BorderLayout(5,5));
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    infoPanel.setBorder(BorderFactory.createTitledBorder("Host information"));
    final JTextField txtHost = new JTextField(model.getHostName());
    txtHost.setColumns(16);
    txtHost.setEditable(false);
    infoPanel.add(new JLabel("Name"));
    infoPanel.add(txtHost);
    add(infoPanel, BorderLayout.NORTH);
    final JTabbedPane serverPane = new JTabbedPane();
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
