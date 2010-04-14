/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;
import org.jminor.framework.server.monitor.ClientTypeMonitor;

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
 * Time: 12:58:29
 */
public class ClientTypeMonitorPanel extends JPanel {

  private ClientTypeMonitor model;
  private final ClientInstanceMonitorPanel clientInstancePanel;
  private final JList clientInstanceList = new JList();

  public ClientTypeMonitorPanel() throws RemoteException {
    this.clientInstancePanel = new ClientInstanceMonitorPanel();
    initUI();
  }

  public void setModel(final ClientTypeMonitor model) {
    this.model = model;
    if (model != null)
      clientInstanceList.setModel(model.getClientInstanceListModel());
  }

  public void refresh() throws RemoteException {
    model.refresh();
  }

  private void initUI() {
    setLayout(new BorderLayout(5, 5));
    clientInstanceList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientInstanceList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        try {
          final ClientInstanceMonitor clientMonitor = (ClientInstanceMonitor) clientInstanceList.getSelectedValue();
          if (clientMonitor != null) {
            clientInstancePanel.setModel(clientMonitor);
            repaint();
          }
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });

    final JPanel clientInstanceBase = new JPanel(new BorderLayout(5, 5));
    final JScrollPane clientInstanceScroller = new JScrollPane(clientInstanceList);
    clientInstanceScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientInstanceBase.add(clientInstanceScroller, BorderLayout.CENTER);
    clientInstanceBase.add(ControlProvider.createButton(ControlFactory.methodControl(this, "refresh", "Refresh")), BorderLayout.SOUTH);

    add(clientInstanceBase, BorderLayout.WEST);
    add(clientInstancePanel, BorderLayout.CENTER);
  }
}
