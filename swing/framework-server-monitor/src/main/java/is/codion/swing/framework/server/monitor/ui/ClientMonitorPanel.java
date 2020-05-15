/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.ControlProvider;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;
import is.codion.swing.framework.server.monitor.ClientMonitor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
  private final ClientInstanceMonitorPanel clientInstancePanel;
  private final JList<ClientInstanceMonitor> clientInstanceList = new JList<>();

  /**
   * Instantiates a new ClientMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public ClientMonitorPanel() throws RemoteException {
    this.clientInstancePanel = new ClientInstanceMonitorPanel();
    initializeUI();
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

  private void initializeUI() {
    setLayout(Layouts.borderLayout());
    clientInstanceList.getSelectionModel().addListSelectionListener(e -> {
      try {
        final ClientInstanceMonitor clientMonitor = clientInstanceList.getSelectedValue();
        if (clientMonitor != null) {
          clientInstancePanel.setModel(clientMonitor);
          repaint();
        }
      }
      catch (final RemoteException ex) {
        throw new RuntimeException(ex);
      }
    });
    clientInstanceList.setComponentPopupMenu(initializePopupMenu());

    final JPanel clientInstanceBase = new JPanel(Layouts.borderLayout());
    final JScrollPane clientInstanceScroller = new JScrollPane(clientInstanceList);
    clientInstanceScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientInstanceBase.add(clientInstanceScroller, BorderLayout.CENTER);
    clientInstanceBase.add(new JButton(Controls.control(this::refresh, "Refresh")), BorderLayout.SOUTH);

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);
    splitPane.setLeftComponent(clientInstanceBase);
    splitPane.setRightComponent(clientInstancePanel);

    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu initializePopupMenu() {
    final ControlList controls = Controls.controlList();
    controls.add(Controls.control(() -> {
      for (final ClientInstanceMonitor clientMonitor : clientInstanceList.getSelectedValuesList()) {
        clientMonitor.disconnect();
        model.getClientInstanceListModel().removeElement(clientMonitor);
      }
    }, "Disconnect"));

    return ControlProvider.createPopupMenu(controls);
  }
}
