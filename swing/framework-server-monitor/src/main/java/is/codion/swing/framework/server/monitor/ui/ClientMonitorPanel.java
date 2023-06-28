/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;
import is.codion.swing.framework.server.monitor.ClientMonitor;

import javax.swing.BorderFactory;
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

  private final ClientMonitor model;
  private final FilteredTable<RemoteClient, Integer> clientInstanceTable;

  /**
   * Instantiates a new ClientMonitorPanel
   * @param model the model
   * @throws RemoteException in case of an exception
   */
  public ClientMonitorPanel(ClientMonitor model) throws RemoteException {
    this.model = model;
    this.clientInstanceTable = FilteredTable.builder(model.clientInstanceTableModel())
            .popupMenu(createPopupMenu())
            .build();
    initializeUI();
  }

  public ClientMonitor model() {
    return model;
  }

  public void refresh() throws RemoteException {
    model.refresh();
  }

  private void initializeUI() {
    JScrollPane clientInstanceScroller = scrollPane(clientInstanceTable)
            .border(BorderFactory.createTitledBorder("Clients"))
            .build();
    JPanel clientInstanceBase = borderLayoutPanel()
            .centerComponent(clientInstanceScroller)
            .southComponent(button(control(this::refresh))
                    .text("Refresh")
                    .build())
            .build();

    JPanel clientInstancePanel = borderLayoutPanel().build();
    JSplitPane splitPane = splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .oneTouchExpandable(true)
            .continuousLayout(true)
            .leftComponent(clientInstanceBase)
            .rightComponent(clientInstancePanel)
            .build();

    model.clientInstanceTableModel().selectionModel().addSelectedItemListener(remoteClient -> {
      clientInstancePanel.removeAll();
      try {
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
                    .name("Disconnect")
                    .enabledState(model.clientInstanceTableModel().selectionModel().selectionNotEmptyObserver()))
            .build())
            .createPopupMenu();
  }

  private void disconnect() throws RemoteException {
    for (RemoteClient remoteClient : model.clientInstanceTableModel().selectionModel().getSelectedItems()) {
      model.server().disconnect(remoteClient.clientId());
      model.clientInstanceTableModel().removeItem(remoteClient);
    }
  }
}
