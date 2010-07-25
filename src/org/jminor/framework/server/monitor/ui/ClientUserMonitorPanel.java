/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.IntBeanSpinnerValueLink;
import org.jminor.framework.server.monitor.ClientMonitor;
import org.jminor.framework.server.monitor.ClientUserMonitor;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 10.12.2007<br>
 * Time: 17:32:53<br>
 */
public final class ClientUserMonitorPanel extends JPanel {

  private final ClientUserMonitor model;

  private final ClientMonitorPanel clientTypeMonitorPanel = new ClientMonitorPanel();
  private JComboBox cmbMaintenanceCheck;

  public ClientUserMonitorPanel(final ClientUserMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  public void disconnectAll() throws RemoteException {
    if (JOptionPane.showConfirmDialog(this, "Are you sure you want to disconnect all clients?", "Disconnect all",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      System.out.println("disco");
      model.disconnectAll();
    }
  }

  private void initUI() throws RemoteException {
    final JList clientTypeList = new JList(model.getClientTypeListModel());

    clientTypeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientTypeList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        clientTypeMonitorPanel.setModel((ClientMonitor) clientTypeList.getSelectedValue());
      }
    });

    final JList userList = new JList(model.getUserListModel());

    userList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    userList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        clientTypeMonitorPanel.setModel((ClientMonitor) userList.getSelectedValue());
      }
    });

    final JPanel clientTypeBase = new JPanel(new BorderLayout(5, 5));
    final JScrollPane clientTypeScroller = new JScrollPane(clientTypeList);
    final JScrollPane userScroller = new JScrollPane(userList);
    clientTypeScroller.setBorder(BorderFactory.createTitledBorder("Client types"));
    userScroller.setBorder(BorderFactory.createTitledBorder("Users"));
    final JPanel clientUserBase = new JPanel(new GridLayout(2, 1, 5, 5));
    clientUserBase.add(clientTypeScroller);
    clientUserBase.add(userScroller);

    clientTypeBase.add(clientUserBase, BorderLayout.CENTER);
    clientTypeBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "refresh", "Refresh")), BorderLayout.SOUTH);

    final JPanel actionBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    actionBase.add(new JLabel("Reaper interval (s)", JLabel.RIGHT));
    actionBase.add(initCheckIntervalComponent());

    actionBase.add(new JLabel("Connection timeout (s)"));
    final JSpinner spnConnectionTimeout = new JSpinner(
            new IntBeanSpinnerValueLink(model, "connectionTimeout", model.eventConnectionTimeoutChanged()).getSpinnerModel());
    ((JSpinner.DefaultEditor) spnConnectionTimeout.getEditor()).getTextField().setEditable(false);
    ((JSpinner.DefaultEditor) spnConnectionTimeout.getEditor()).getTextField().setColumns(7);
    actionBase.add(spnConnectionTimeout);

    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "disconnectTimedOut",
            "Disconnect idle", null, "Disconnect those that have exceeded the allowed idle time")));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(this, "disconnectAll",
            "Disconnect all", null, "Disconnect all")));

    setLayout(new BorderLayout());

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);

    splitPane.setLeftComponent(clientTypeBase);

    final JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
    rightPanel.add(actionBase, BorderLayout.NORTH);
    rightPanel.add(clientTypeMonitorPanel, BorderLayout.CENTER);

    splitPane.setRightComponent(rightPanel);

    add(splitPane, BorderLayout.CENTER);
  }

  private JComponent initCheckIntervalComponent() throws RemoteException {
    cmbMaintenanceCheck = new JComboBox(new Integer[] {1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,120,180,340,6000,10000});
    cmbMaintenanceCheck.setSelectedItem(model.getCheckMaintenanceInterval());
    cmbMaintenanceCheck.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        try {
          model.setCheckMaintenanceInterval((Integer) cmbMaintenanceCheck.getSelectedItem());
        }
        catch (RemoteException ex) {
          handleException(ex);
        }
      }
    });

    return cmbMaintenanceCheck;
  }

  private void handleException(final Exception exception) {
    ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(this),
            Messages.get(Messages.EXCEPTION), exception.getMessage(), exception);
  }
}
