/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;
import org.jminor.framework.server.monitor.UserInstanceMonitor;

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
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 11:31:45
 */
public class UserInstanceMonitorPanel extends JPanel {

  private final UserInstanceMonitor model;
  private final ClientInstanceMonitorPanel clientInstancePanel;

  public UserInstanceMonitorPanel(final UserInstanceMonitor model) throws RemoteException {
    this.model = model;
    this.clientInstancePanel = new ClientInstanceMonitorPanel();
    initUI();
  }

  private void initUI() {
    setLayout(new BorderLayout());
    final JList clientList = new JList(model.getClientListModel());
    clientList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        try {
          clientInstancePanel.setModel((ClientInstanceMonitor) clientList.getSelectedValue());
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });

    final JPanel clientBase = new JPanel(new BorderLayout());
    final JScrollPane clientScroller = new JScrollPane(clientList);
    clientScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientBase.add(clientScroller, BorderLayout.CENTER);
    clientBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "refresh", "Refresh")), BorderLayout.SOUTH);
    add(clientBase, BorderLayout.WEST);
    add(clientInstancePanel, BorderLayout.CENTER);
  }
}
