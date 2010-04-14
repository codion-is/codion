/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.server.monitor.UserInstanceMonitor;
import org.jminor.framework.server.monitor.UserMonitor;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri
 * Date: 11.12.2007
 * Time: 12:23:17
 */
public class UserMonitorPanel  extends JPanel {

  private final UserMonitor model;

  public UserMonitorPanel(final UserMonitor model) {
    this.model = model;
    initUI();
  }

  private void initUI() {
    setLayout(new BorderLayout());
    final JList userInstanceList = new JList(model.getUserInstanceMonitorsListModel());
    userInstanceList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userInstanceList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        try {
          final UserInstanceMonitor userInstanceMonitor = (UserInstanceMonitor) userInstanceList.getSelectedValue();
          if (userInstanceMonitor != null)
            add(new UserInstanceMonitorPanel(userInstanceMonitor), BorderLayout.CENTER);
          repaint();
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });

    final JPanel usersBase = new JPanel(new BorderLayout());
    final JScrollPane usersScroller = new JScrollPane(userInstanceList);
    usersScroller.setBorder(BorderFactory.createTitledBorder("Users"));
    usersBase.add(usersScroller, BorderLayout.CENTER);
    usersBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "refresh", "Refresh")), BorderLayout.SOUTH);
    add(usersBase, BorderLayout.WEST);
  }
}
