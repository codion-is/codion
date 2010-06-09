/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;
import org.jminor.framework.server.monitor.ClientMonitor;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 11.12.2007<br>
 * Time: 12:58:29<br>
 */
public class ClientMonitorPanel extends JPanel {

  private ClientMonitor model;
  private final ClientInstanceMonitorPanel clientInstancePanel;
  private final JList clientInstanceList = new JList();

  public ClientMonitorPanel() throws RemoteException {
    this.clientInstancePanel = new ClientInstanceMonitorPanel();
    initUI();
  }

  public void setModel(final ClientMonitor model) {
    this.model = model;
    if (model != null) {
      clientInstanceList.setModel(model.getClientInstanceListModel());
    }
  }

  public void refresh() throws RemoteException {
    if (model != null) {
      model.refresh();
    }
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

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setLeftComponent(clientInstanceBase);
    splitPane.setRightComponent(clientInstancePanel);

    add(splitPane, BorderLayout.CENTER);
  }
}
