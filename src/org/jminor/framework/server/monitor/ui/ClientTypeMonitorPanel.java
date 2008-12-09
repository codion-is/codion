/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.ControlProvider;
import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.ClientTypeMonitor;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
public class ClientTypeMonitorPanel extends JPanel implements IPopupProvider {

  private final ClientTypeMonitor model;

  private JComboBox cmbMaintainanceCheck;
  private JPopupMenu popupMenu;

  public ClientTypeMonitorPanel(final ClientTypeMonitor model) throws RemoteException {
    this.model = model;
    initUI();
    updateView();
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  public void updateView() throws RemoteException {
    cmbMaintainanceCheck.setSelectedItem(model.getServer().getCheckMaintenanceInterval());
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
  }

  private JComponent initCheckIntervalComponent() {
    cmbMaintainanceCheck = new JComboBox(new Integer[] {1,2,3,4,5,6,7,8,9,10,20,30,40,50,60,120,180,340,6000,10000});
    cmbMaintainanceCheck.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        try {
          model.getServer().setCheckMaintenanceInterval((Integer) cmbMaintainanceCheck.getSelectedItem());
        }
        catch (RemoteException e1) {
          handleException(e1);
        }
      }
    });

    return cmbMaintainanceCheck;
  }

  private void handleException(final Exception e1) {
    UiUtil.handleException(e1, this);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(model, "refresh", "Refresh"));

    return ret;
  }
}
