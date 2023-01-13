/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientMonitor;
import is.codion.swing.framework.server.monitor.ClientUserMonitor;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

import static is.codion.swing.common.ui.control.Control.control;

/**
 * A ClientUserMonitorPanel
 */
public final class ClientUserMonitorPanel extends JPanel {

  private static final int SPINNER_COLUMNS = 3;
  private static final Integer[] MAINTENANCE_INTERVAL_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000};

  private final ClientUserMonitor model;

  private final ClientMonitorPanel clientTypeMonitorPanel = new ClientMonitorPanel();

  /**
   * Instantiates a new ClientUserMonitorPanel
   * @param model the ClientUserMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public ClientUserMonitorPanel(ClientUserMonitor model) throws RemoteException {
    this.model = model;
    initializeUI();
  }

  public void disconnectAll() throws RemoteException {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to disconnect all clients?", "Disconnect all",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      model.disconnectAll();
    }
  }

  private void initializeUI() throws RemoteException {
    setLayout(new BorderLayout());
    add(Components.tabbedPane()
            .tab("Current", createCurrentConnectionsPanel())
            .tab("History", createConnectionHistoryPanel())
            .build(), BorderLayout.CENTER);
  }

  private JSplitPane createCurrentConnectionsPanel() throws RemoteException {
    JList<ClientMonitor> clientTypeList = new JList<>(model.clientTypeListModel());

    clientTypeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientTypeList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel(clientTypeList.getSelectedValue()));

    JList<ClientMonitor> userList = new JList<>(model.userListModel());

    userList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel(userList.getSelectedValue()));

    JScrollPane clientTypeScroller = Components.scrollPane(clientTypeList)
            .border(BorderFactory.createTitledBorder("Client types"))
            .build();
    JScrollPane userScroller = Components.scrollPane(userList)
            .border(BorderFactory.createTitledBorder("Users"))
            .build();
    JPanel clientUserBase = Components.panel(Layouts.gridLayout(2, 1))
            .add(clientTypeScroller)
            .add(userScroller)
            .build();

    JPanel clientTypeBase = Components.panel(Layouts.borderLayout())
            .add(clientUserBase, BorderLayout.CENTER)
            .add(Components.button(control(model::refresh))
                    .caption("Refresh")
                    .build(), BorderLayout.SOUTH)
            .build();

    JPanel actionBase = Components.panel(Layouts.flowLayout(FlowLayout.LEFT))
            .border(BorderFactory.createTitledBorder("Remote connection controls"))
            .add(new JLabel("Reaper interval (s)", SwingConstants.RIGHT))
            .add(createMaintenanceIntervalComponent())
            .add(new JLabel("Idle connection timeout (s)"))
            .add(Components.integerSpinner(new SpinnerNumberModel(), model.idleConnectionTimeoutValue())
                    .columns(7)
                    .build())
            .add(Components.button(control(model::disconnectTimedOut))
                    .caption("Disconnect idle")
                    .toolTipText("Disconnect those that have exceeded the allowed idle time")
                    .build())
            .add(Components.button(control(this::disconnectAll))
                    .caption("Disconnect all")
                    .toolTipText("Disconnect all clients")
                    .build())
            .build();

    JPanel currentConnectionsPanel = Components.panel(Layouts.borderLayout())
            .add(actionBase, BorderLayout.NORTH)
            .add(clientTypeMonitorPanel, BorderLayout.CENTER)
            .build();

    return Components.splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .oneTouchExpandable(true)
            .continuousLayout(true)
            .leftComponent(clientTypeBase)
            .rightComponent(currentConnectionsPanel)
            .build();
  }

  private JPanel createConnectionHistoryPanel() {
    JPanel configPanel = Components.panel(Layouts.flowLayout(FlowLayout.LEFT))
            .add(new JLabel("Update interval (s)"))
            .add(Components.integerSpinner(model.updateIntervalValue())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build())
            .build();

    JPanel configBase = Components.panel(Layouts.borderLayout())
            .add(configPanel, BorderLayout.CENTER)
            .add(Components.button(control(model::resetHistory))
                    .caption("Reset")
                    .build(), BorderLayout.EAST)
            .build();

    FilteredTable<?, ?, ?> userHistoryTable = FilteredTable.filteredTable(model.userHistoryTableModel());

    return Components.panel(Layouts.borderLayout())
            .add(new JScrollPane(userHistoryTable), BorderLayout.CENTER)
            .add(configBase, BorderLayout.SOUTH)
            .build();
  }

  private JComponent createMaintenanceIntervalComponent() throws RemoteException {
    return Components.comboBox(new DefaultComboBoxModel<>(MAINTENANCE_INTERVAL_VALUES))
            .initialValue(model.getMaintenanceInterval())
            .itemListener(e -> {
              try {
                model.setMaintenanceInterval((Integer) ((JComboBox<Integer>) e.getSource()).getSelectedItem());
              }
              catch (RemoteException ex) {
                onException(ex);
              }
            }).build();
  }

  private void onException(Exception exception) {
    Dialogs.exceptionDialog()
            .owner(this)
            .show(exception);
  }
}
