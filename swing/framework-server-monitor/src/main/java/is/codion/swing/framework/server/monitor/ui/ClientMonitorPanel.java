/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;
import is.codion.swing.framework.server.monitor.ClientMonitor;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

import static is.codion.swing.common.ui.control.Control.control;

/**
 * A ClientMonitorPanel
 */
public final class ClientMonitorPanel extends JPanel {

  private ClientMonitor model;
  private final JList<RemoteClient> clientList = new JList<>();

  /**
   * Instantiates a new ClientMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public ClientMonitorPanel() throws RemoteException {
    initializeUI();
  }

  public void setModel(ClientMonitor model) {
    this.model = model;
    if (model != null) {
      clientList.setModel(model.getRemoteClientListModel());
    }
  }

  public void refresh() throws RemoteException {
    if (model != null) {
      model.refresh();
    }
  }

  private void initializeUI() {
    clientList.setComponentPopupMenu(initializePopupMenu());

    JPanel clientInstanceBase = new JPanel(Layouts.borderLayout());
    JScrollPane clientInstanceScroller = new JScrollPane(clientList);
    clientInstanceScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientInstanceBase.add(clientInstanceScroller, BorderLayout.CENTER);
    clientInstanceBase.add(Components.button(control(this::refresh))
            .caption("Refresh")
            .build(), BorderLayout.SOUTH);

    JPanel clientInstancePanel = new JPanel(Layouts.borderLayout());
    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);
    splitPane.setLeftComponent(clientInstanceBase);
    splitPane.setRightComponent(clientInstancePanel);

    clientList.getSelectionModel().addListSelectionListener(e -> {
      clientInstancePanel.removeAll();
      try {
        RemoteClient remoteClient = clientList.getSelectedValue();
        if (model != null && remoteClient != null) {
          ClientInstanceMonitorPanel clientMonitor = new ClientInstanceMonitorPanel(new ClientInstanceMonitor(model.getServer(), remoteClient));
          clientInstancePanel.add(clientMonitor, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
      }
      catch (RemoteException ex) {
        throw new RuntimeException(ex);
      }
    });
    setLayout(Layouts.borderLayout());
    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu initializePopupMenu() {
    return Controls.builder()
            .control(Control.builder(this::disconnect)
                    .caption("Disconnect"))
            .build()
            .createPopupMenu();
  }

  private void disconnect() throws RemoteException {
    for (RemoteClient remoteClient : clientList.getSelectedValuesList()) {
      model.getServer().disconnect(remoteClient.getClientId());
      model.getRemoteClientListModel().removeElement(remoteClient);
    }
  }
}
