/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.TaskScheduler;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.table.FilteredTablePanel;
import org.jminor.framework.server.monitor.ClientMonitor;
import org.jminor.framework.server.monitor.ClientUserMonitor;

import javax.swing.BorderFactory;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;

/**
 * A ClientUserMonitorPanel
 */
public final class ClientUserMonitorPanel extends JPanel {

  private static final int SPINNER_COLUMNS = 3;

  private final ClientUserMonitor model;

  private final ClientMonitorPanel clientTypeMonitorPanel = new ClientMonitorPanel();
  private JComboBox<Integer> cmbMaintenance;

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
    clientTypeList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        clientTypeMonitorPanel.setModel((ClientMonitor) clientTypeList.getSelectedValue());
      }
    });

    final JList userList = new JList<>(model.getUserListModel());

    userList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        clientTypeMonitorPanel.setModel((ClientMonitor) userList.getSelectedValue());
      }
    });

    final JPanel clientTypeBase = new JPanel(UiUtil.createBorderLayout());
    final JScrollPane clientTypeScroller = new JScrollPane(clientTypeList);
    final JScrollPane userScroller = new JScrollPane(userList);
    clientTypeScroller.setBorder(BorderFactory.createTitledBorder("Client types"));
    userScroller.setBorder(BorderFactory.createTitledBorder("Users"));
    final JPanel clientUserBase = new JPanel(UiUtil.createGridLayout(2, 1));
    clientUserBase.add(clientTypeScroller);
    clientUserBase.add(userScroller);

    clientTypeBase.add(clientUserBase, BorderLayout.CENTER);
    clientTypeBase.add(ControlProvider.createButton(Controls.methodControl(model, "refresh", "Refresh")), BorderLayout.SOUTH);

    final JPanel actionBase = new JPanel(UiUtil.createFlowLayout(FlowLayout.LEFT));
    actionBase.add(new JLabel("Reaper interval (s)", JLabel.RIGHT));
    actionBase.add(initializeMaintenanceIntervalComponent());

    actionBase.add(new JLabel("Connection timeout (s)"));
    final JSpinner spnConnectionTimeout = new JSpinner(
            ValueLinks.intSpinnerValueLink(model, "connectionTimeout", model.getConnectionTimeoutObserver()));
    ((JSpinner.DefaultEditor) spnConnectionTimeout.getEditor()).getTextField().setColumns(7);
    actionBase.add(spnConnectionTimeout);

    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(ControlProvider.createButton(Controls.methodControl(model, "disconnectTimedOut",
            "Disconnect idle", null, "Disconnect those that have exceeded the allowed idle time")));
    actionBase.add(ControlProvider.createButton(Controls.methodControl(this, "disconnectAll",
            "Disconnect all", null, "Disconnect all")));

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
    final JSpinner spnUpdateInterval = new JSpinner(ValueLinks.intSpinnerValueLink(model.getUpdateScheduler(),
            TaskScheduler.INTERVAL_PROPERTY, model.getUpdateScheduler().getIntervalObserver()));

    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnUpdateInterval.getEditor()).getTextField().setColumns(SPINNER_COLUMNS);

    configPanel.add(new JLabel("Update interval (s)"));
    configPanel.add(spnUpdateInterval);

    final JPanel configBase = new JPanel(UiUtil.createBorderLayout());
    configBase.add(configPanel, BorderLayout.CENTER);
    configBase.add(ControlProvider.createButton(
            Controls.methodControl(model, "resetHistory", "Reset")), BorderLayout.EAST);

    final FilteredTablePanel<ClientUserMonitor.UserInfo, Integer> userHistoryTable = new FilteredTablePanel<>(model.getUserHistoryTableModel());

    final JPanel connectionHistoryPanel = new JPanel(UiUtil.createBorderLayout());
    connectionHistoryPanel.add(userHistoryTable, BorderLayout.CENTER);
    connectionHistoryPanel.add(configBase, BorderLayout.SOUTH);

    return connectionHistoryPanel;
  }

  private JComponent initializeMaintenanceIntervalComponent() throws RemoteException {
    cmbMaintenance = new JComboBox<>(new Integer[] {1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,120,180,340,6000,10000});
    cmbMaintenance.setSelectedItem(model.getMaintenanceInterval());
    cmbMaintenance.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(final ItemEvent e) {
        try {
          model.setMaintenanceInterval((Integer) cmbMaintenance.getSelectedItem());
        }
        catch (final RemoteException ex) {
          handleException(ex);
        }
      }
    });

    return cmbMaintenance;
  }

  private void handleException(final Exception exception) {
    ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(this),
            Messages.get(Messages.EXCEPTION), exception.getMessage(), exception);
  }
}
