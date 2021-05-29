/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
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

  public void setModel(final ClientMonitor model) {
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

    final JPanel clientInstanceBase = new JPanel(Layouts.borderLayout());
    final JScrollPane clientInstanceScroller = new JScrollPane(clientList);
    clientInstanceScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientInstanceBase.add(clientInstanceScroller, BorderLayout.CENTER);
    clientInstanceBase.add(Control.builder(this::refresh)
            .caption("Refresh")
            .build().createButton(), BorderLayout.SOUTH);

    final JPanel clientInstancePanel = new JPanel(Layouts.borderLayout());
    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);
    splitPane.setLeftComponent(clientInstanceBase);
    splitPane.setRightComponent(clientInstancePanel);

    clientList.getSelectionModel().addListSelectionListener(e -> {
      clientInstancePanel.removeAll();
      try {
        final RemoteClient remoteClient = clientList.getSelectedValue();
        if (model != null && remoteClient != null) {
          final ClientInstanceMonitorPanel clientMonitor = new ClientInstanceMonitorPanel(new ClientInstanceMonitor(model.getServer(), remoteClient));
          clientInstancePanel.add(clientMonitor, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
      }
      catch (final RemoteException ex) {
        throw new RuntimeException(ex);
      }
    });
    setLayout(Layouts.borderLayout());
    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu initializePopupMenu() {
    final Controls controls = Controls.controls();
    controls.add(Control.builder(() -> {
      for (final RemoteClient remoteClient : clientList.getSelectedValuesList()) {
        model.getServer().disconnect(remoteClient.getClientId());
        model.getRemoteClientListModel().removeElement(remoteClient);
      }
    }).caption("Disconnect").build());

    return controls.createPopupMenu();
  }
}
