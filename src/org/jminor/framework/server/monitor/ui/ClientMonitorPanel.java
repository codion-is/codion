/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.framework.server.monitor.ClientMonitor;
import org.jminor.framework.server.monitor.ClientTypeMonitor;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;

/**
 * User: Bjorn Darri<br>
 * Date: 10.12.2007<br>
 * Time: 17:32:53<br>
 */
public class ClientMonitorPanel extends JPanel {

  private final ClientMonitor model;

  private ClientTypeMonitorPanel clientTypeMonitorPanel = new ClientTypeMonitorPanel();
  private JComboBox cmbMaintenanceCheck;

  public ClientMonitorPanel(final ClientMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  private void initUI() throws RemoteException {
    final JList clientTypeList = new JList(model.getClientTypeListModel());

    clientTypeList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientTypeList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(final ListSelectionEvent e) {
        clientTypeMonitorPanel.setModel((ClientTypeMonitor) clientTypeList.getSelectedValue());
      }
    });

    final JPanel clientTypeBase = new JPanel(new BorderLayout(5, 5));
    final JScrollPane clientTypeScroller = new JScrollPane(clientTypeList);
    clientTypeScroller.setBorder(BorderFactory.createTitledBorder("Client types"));
    clientTypeBase.add(clientTypeScroller, BorderLayout.CENTER);
    clientTypeBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "refresh", "Refresh")), BorderLayout.SOUTH);

    final JPanel actionBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    actionBase.add(new JLabel("Reaper interval (s)", JLabel.RIGHT));
    actionBase.add(initCheckIntervalComponent());
    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "disconnectTimedOut",
            "Disconnect idle", null, "Disconnect those that have exceeded the allowed idle time")));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "disconnectAll",
            "Disconnect all", null, "Disconnect all")));

    setLayout(new BorderLayout());
    add(clientTypeBase, BorderLayout.WEST);
    add(actionBase, BorderLayout.NORTH);
    add(clientTypeMonitorPanel, BorderLayout.CENTER);
  }

  private JComponent initCheckIntervalComponent() throws RemoteException {
    cmbMaintenanceCheck = new JComboBox(new Integer[] {1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,120,180,340,6000,10000});
    cmbMaintenanceCheck.setSelectedItem(model.getCheckMaintenanceInterval());
    cmbMaintenanceCheck.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
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
