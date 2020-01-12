/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.server.monitor.ui;

import org.jminor.common.TaskScheduler;
import org.jminor.common.i18n.Messages;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.table.FilteredTable;
import org.jminor.swing.common.ui.value.ValueLinks;
import org.jminor.swing.framework.server.monitor.ClientMonitor;
import org.jminor.swing.framework.server.monitor.ClientUserMonitor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

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
  public ClientUserMonitorPanel(final ClientUserMonitor model) throws RemoteException {
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
    final JTabbedPane baseTabPane = new JTabbedPane();
    baseTabPane.addTab("Current", createCurrentConnectionsPanel());
    baseTabPane.addTab("History", createConnectionHistoryPanel());

    add(baseTabPane, BorderLayout.CENTER);
  }

  private JSplitPane createCurrentConnectionsPanel() throws RemoteException {
    final JList clientTypeList = new JList<>(model.getClientTypeListModel());

    clientTypeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientTypeList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel((ClientMonitor) clientTypeList.getSelectedValue()));

    final JList userList = new JList<>(model.getUserListModel());

    userList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getSelectionModel().addListSelectionListener(e -> clientTypeMonitorPanel.setModel((ClientMonitor) userList.getSelectedValue()));

    final JPanel clientTypeBase = new JPanel(UiUtil.createBorderLayout());
    final JScrollPane clientTypeScroller = new JScrollPane(clientTypeList);
    final JScrollPane userScroller = new JScrollPane(userList);
    clientTypeScroller.setBorder(BorderFactory.createTitledBorder("Client types"));
    userScroller.setBorder(BorderFactory.createTitledBorder("Users"));
    final JPanel clientUserBase = new JPanel(UiUtil.createGridLayout(2, 1));
    clientUserBase.add(clientTypeScroller);
    clientUserBase.add(userScroller);

    clientTypeBase.add(clientUserBase, BorderLayout.CENTER);
    clientTypeBase.add(new JButton(Controls.control(model::refresh, "Refresh")), BorderLayout.SOUTH);

    final JPanel actionBase = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    actionBase.add(new JLabel("Reaper interval (s)", JLabel.RIGHT));
    actionBase.add(initializeMaintenanceIntervalComponent());

    actionBase.add(new JLabel("Connection timeout (s)"));
    final JSpinner connectionTimeoutSpinner = new JSpinner(
            ValueLinks.intSpinnerValueLink(model, "connectionTimeout", model.getConnectionTimeoutObserver()));
    ((JSpinner.DefaultEditor) connectionTimeoutSpinner.getEditor()).getTextField().setColumns(7);
    actionBase.add(connectionTimeoutSpinner);

    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(new JButton(Controls.control(model::disconnectTimedOut,
            "Disconnect idle", null, "Disconnect those that have exceeded the allowed idle time")));
    actionBase.add(new JButton(Controls.control(this::disconnectAll,
            "Disconnect all", null, "Disconnect all clients")));

    setLayout(new BorderLayout());

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);

    splitPane.setLeftComponent(clientTypeBase);

    final JPanel currentConnectionsPanel = new JPanel(UiUtil.createBorderLayout());
    currentConnectionsPanel.add(actionBase, BorderLayout.NORTH);
    currentConnectionsPanel.add(clientTypeMonitorPanel, BorderLayout.CENTER);

    splitPane.setRightComponent(currentConnectionsPanel);

    return splitPane;
  }

  private JPanel createConnectionHistoryPanel() {
    final JPanel configPanel = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    final JSpinner updateIntervalSpinner = new JSpinner(ValueLinks.intSpinnerValueLink(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) updateIntervalSpinner.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    configPanel.add(new JLabel("Update interval (s)"));
    configPanel.add(updateIntervalSpinner);

    final JPanel configBase = new JPanel(UiUtil.createBorderLayout());
    configBase.add(configPanel, BorderLayout.CENTER);
    configBase.add(new JButton(Controls.control(model::resetHistory, "Reset")), BorderLayout.EAST);

    final FilteredTable userHistoryTable = new FilteredTable(model.getUserHistoryTableModel());

    final JPanel connectionHistoryPanel = new JPanel(UiUtil.createBorderLayout());
    connectionHistoryPanel.add(new JScrollPane(userHistoryTable), BorderLayout.CENTER);
    connectionHistoryPanel.add(configBase, BorderLayout.SOUTH);

    return connectionHistoryPanel;
  }

  private JComponent initializeMaintenanceIntervalComponent() throws RemoteException {
    final JComboBox<Integer> maintenanceBox = new JComboBox<>(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000});
    maintenanceBox.setSelectedItem(model.getMaintenanceInterval());
    maintenanceBox.addItemListener(e -> {
      try {
        model.setMaintenanceInterval((Integer) maintenanceBox.getSelectedItem());
      }
      catch (final RemoteException ex) {
        onException(ex);
      }
    });

    return maintenanceBox;
  }

  private void onException(final Exception exception) {
    Dialogs.showExceptionDialog(UiUtil.getParentWindow(this),
            Messages.get(Messages.EXCEPTION), exception.getMessage(), exception);
  }
}
