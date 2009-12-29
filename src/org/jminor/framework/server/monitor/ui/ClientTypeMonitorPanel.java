/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.ui.ExceptionDialog;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.ClientTypeMonitor;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.rmi.RemoteException;

/**
 * User: Björn Darri
 * Date: 11.12.2007
 * Time: 12:58:29
 */
public class ClientTypeMonitorPanel extends JPanel {

  private final ClientTypeMonitor model;

  private JComboBox cmbMaintenanceCheck;

  public ClientTypeMonitorPanel(final ClientTypeMonitor model) throws RemoteException {
    this.model = model;
    initUI();
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    final JPanel actionBase = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    actionBase.add(new JLabel("Reaper interval (s)", JLabel.RIGHT));
    actionBase.add(initCheckIntervalComponent());
    actionBase.setBorder(BorderFactory.createTitledBorder("Remote connection controls"));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "disconnectTimedOut",
            "Disconnect idle", null, "Disconnect those that have exceeded the allowed idle time")));
    actionBase.add(ControlProvider.createButton(ControlFactory.methodControl(model, "disconnectAll",
            "Disconnect all", null, "Disconnect all")));
    add(actionBase, BorderLayout.NORTH);
    //todo client list
  }

  private JComponent initCheckIntervalComponent() throws RemoteException {
    cmbMaintenanceCheck = new JComboBox(new Integer[] {1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,120,180,340,6000,10000});
    cmbMaintenanceCheck.setSelectedItem(model.getServer().getCheckMaintenanceInterval());
    cmbMaintenanceCheck.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        try {
          model.getServer().setCheckMaintenanceInterval((Integer) cmbMaintenanceCheck.getSelectedItem());
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

  private ControlSet getPopupCommands() {
    final ControlSet controlSet = new ControlSet();
    controlSet.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return controlSet;
  }
}
