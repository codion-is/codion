/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientMonitor;
import is.codion.swing.framework.server.monitor.ClientUserMonitor;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
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
    JTabbedPane baseTabPane = new JTabbedPane();
    baseTabPane.addTab("Current", createCurrentConnectionsPanel());
    baseTabPane.addTab("History", createConnectionHistoryPanel());

    add(baseTabPane, BorderLayout.CENTER);
  }

  private JSplitPane createCurrentConnectionsPanel() throws RemoteException {
    JList<ClientMonitor> clientTypeList = new JList<>(model.clientTypeListModel());

    clientTypeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientTypeList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel(clientTypeList.getSelectedValue()));

    JList<ClientMonitor> userList = new JList<>(model.userListModel());

    userList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel(userList.getSelectedValue()));

    JPanel clientTypeBase = new JPanel(Layouts.borderLayout());
    JScrollPane clientTypeScroller = new JScrollPane(clientTypeList);
    JScrollPane userScroller = new JScrollPane(userList);
    clientTypeScroller.setBorder(BorderFactory.createTitledBorder("Client types"));
    userScroller.setBorder(BorderFactory.createTitledBorder("Users"));
    JPanel clientUserBase = new JPanel(Layouts.gridLayout(2, 1));
    clientUserBase.add(clientTypeScroller);
    clientUserBase.add(userScroller);

    clientTypeBase.add(clientUserBase, BorderLayout.CENTER);
    clientTypeBase.add(Components.button(control(model::refresh))
            .caption("Refresh")
            .build(), BorderLayout.SOUTH);

    JPanel actionBase = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    actionBase.add(new JLabel("Reaper interval (s)", SwingConstants.RIGHT));
    actionBase.add(createMaintenanceIntervalComponent());

    actionBase.add(new JLabel("Idle connection timeout (s)"));
    actionBase.add(Components.integerSpinner(new SpinnerNumberModel(), model.idleConnectionTimeoutValue())
            .columns(7)
            .build());

    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(Components.button(control(model::disconnectTimedOut))
            .caption("Disconnect idle")
            .toolTipText("Disconnect those that have exceeded the allowed idle time")
            .build());
    actionBase.add(Components.button(control(this::disconnectAll))
            .caption("Disconnect all")
            .toolTipText("Disconnect all clients")
            .build());

    setLayout(new BorderLayout());

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);
    splitPane.setContinuousLayout(true);

    splitPane.setLeftComponent(clientTypeBase);

    JPanel currentConnectionsPanel = new JPanel(Layouts.borderLayout());
    currentConnectionsPanel.add(actionBase, BorderLayout.NORTH);
    currentConnectionsPanel.add(clientTypeMonitorPanel, BorderLayout.CENTER);

    splitPane.setRightComponent(currentConnectionsPanel);

    return splitPane;
  }

  private JPanel createConnectionHistoryPanel() {
    JPanel configPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    configPanel.add(new JLabel("Update interval (s)"));
    configPanel.add(Components.integerSpinner(model.updateIntervalValue())
            .minimum(1)
            .columns(SPINNER_COLUMNS)
            .editable(false)
            .build());

    JPanel configBase = new JPanel(Layouts.borderLayout());
    configBase.add(configPanel, BorderLayout.CENTER);
    configBase.add(Components.button(control(model::resetHistory))
            .caption("Reset")
            .build(), BorderLayout.EAST);

    FilteredTable<?, ?, ?> userHistoryTable = new FilteredTable<>(model.userHistoryTableModel());

    JPanel connectionHistoryPanel = new JPanel(Layouts.borderLayout());
    connectionHistoryPanel.add(new JScrollPane(userHistoryTable), BorderLayout.CENTER);
    connectionHistoryPanel.add(configBase, BorderLayout.SOUTH);

    return connectionHistoryPanel;
  }

  private JComponent createMaintenanceIntervalComponent() throws RemoteException {
    JComboBox<Integer> maintenanceBox = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000});
    maintenanceBox.setSelectedItem(model.getMaintenanceInterval());
    maintenanceBox.addItemListener(e -> {
      try {
        model.setMaintenanceInterval((Integer) maintenanceBox.getSelectedItem());
      }
      catch (RemoteException ex) {
        onException(ex);
      }
    });

    return maintenanceBox;
  }

  private void onException(Exception exception) {
    Dialogs.exceptionDialog()
            .owner(this)
            .show(exception);
  }
}
