/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
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

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

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
      clientList.setModel(model.remoteClientListModel());
    }
  }

  public void refresh() throws RemoteException {
    if (model != null) {
      model.refresh();
    }
  }

  private void initializeUI() {
    clientList.setComponentPopupMenu(createPopupMenu());

    JScrollPane clientInstanceScroller = scrollPane(clientList)
            .border(BorderFactory.createTitledBorder("Clients"))
            .build();
    JPanel clientInstanceBase = borderLayoutPanel(borderLayout())
            .centerComponent(clientInstanceScroller)
            .southComponent(button(control(this::refresh))
                    .text("Refresh")
                    .build())
            .build();

    JPanel clientInstancePanel = panel(borderLayout()).build();
    JSplitPane splitPane = splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .oneTouchExpandable(true)
            .continuousLayout(true)
            .leftComponent(clientInstanceBase)
            .rightComponent(clientInstancePanel)
            .build();

    clientList.getSelectionModel().addListSelectionListener(e -> {
      clientInstancePanel.removeAll();
      try {
        RemoteClient remoteClient = clientList.getSelectedValue();
        if (model != null && remoteClient != null) {
          ClientInstanceMonitorPanel clientMonitor = new ClientInstanceMonitorPanel(new ClientInstanceMonitor(model.server(), remoteClient));
          clientInstancePanel.add(clientMonitor, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
      }
      catch (RemoteException ex) {
        throw new RuntimeException(ex);
      }
    });
    setLayout(borderLayout());
    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu createPopupMenu() {
    return menu(Controls.builder()
            .control(Control.builder(this::disconnect)
                    .name("Disconnect"))
            .build())
            .createPopupMenu();
  }

  private void disconnect() throws RemoteException {
    for (RemoteClient remoteClient : clientList.getSelectedValuesList()) {
      model.server().disconnect(remoteClient.clientId());
      model.remoteClientListModel().removeElement(remoteClient);
    }
  }
}
