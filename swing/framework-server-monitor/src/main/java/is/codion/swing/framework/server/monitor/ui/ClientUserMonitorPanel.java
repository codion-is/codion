/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.server.monitor.ClientUserMonitor;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.rmi.RemoteException;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.dialog.Dialogs.exceptionDialog;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A ClientUserMonitorPanel
 */
public final class ClientUserMonitorPanel extends JPanel {

  private static final int SPINNER_COLUMNS = 3;
  private static final Integer[] MAINTENANCE_INTERVAL_VALUES = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 30, 40, 50, 60, 120, 180, 340, 6000, 10000};

  private final ClientUserMonitor model;

  private final ClientMonitorPanel clientTypeMonitorPanel;

  /**
   * Instantiates a new ClientUserMonitorPanel
   * @param model the ClientUserMonitor to base this panel on
   * @throws RemoteException in case of an exception
   */
  public ClientUserMonitorPanel(ClientUserMonitor model) throws RemoteException {
    this.model = requireNonNull(model);
    this.clientTypeMonitorPanel = new ClientMonitorPanel(model.clientMonitor());
    initializeUI();
  }

  public void disconnectAll() throws RemoteException {
    if (showConfirmDialog(this, "Are you sure you want to disconnect all clients?", "Disconnect all",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      model.disconnectAll();
    }
  }

  private void initializeUI() throws RemoteException {
    setLayout(new BorderLayout());
    add(tabbedPane()
            .tab("Current", createCurrentConnectionsPanel())
            .tab("History", createConnectionHistoryPanel())
            .build(), BorderLayout.CENTER);
  }

  private JPanel createCurrentConnectionsPanel() throws RemoteException {
    JPanel actionBase = flowLayoutPanel(FlowLayout.LEFT)
            .border(createTitledBorder("Remote connection controls"))
            .add(new JLabel("Reaper interval (s)", SwingConstants.RIGHT))
            .add(createMaintenanceIntervalComponent())
            .add(new JLabel("Idle connection timeout (s)"))
            .add(integerSpinner(new SpinnerNumberModel(), model.idleConnectionTimeout())
                    .columns(4)
                    .build())
            .add(button(control(model::disconnectTimedOut))
                    .text("Disconnect idle")
                    .toolTipText("Disconnect those that have exceeded the allowed idle time")
                    .build())
            .add(button(control(this::disconnectAll))
                    .text("Disconnect all")
                    .toolTipText("Disconnect all clients")
                    .build())
            .build();

    return borderLayoutPanel()
            .northComponent(actionBase)
            .centerComponent(clientTypeMonitorPanel)
            .build();
  }

  private JPanel createConnectionHistoryPanel() {
    JPanel configPanel = flowLayoutPanel(FlowLayout.LEFT)
            .add(new JLabel("Update interval (s)"))
            .add(integerSpinner(model.updateInterval())
                    .minimum(1)
                    .columns(SPINNER_COLUMNS)
                    .editable(false)
                    .build())
            .build();

    JPanel configBase = borderLayoutPanel()
            .centerComponent(configPanel)
            .eastComponent(button(control(model::resetHistory))
                    .text("Reset")
                    .build())
            .build();

    FilteredTable<?, ?> userHistoryTable = FilteredTable.builder(model.userHistoryTableModel())
            .popupMenuControls(table -> Controls.builder()
                    .controls(Controls.builder()
                            .name("Columns")
                            .control(table.createToggleColumnsControls())
                            .control(table.createResetColumnsControl())
                            .control(table.createAutoResizeModeControl())
                            .build())
                    .build())
            .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
            .build();

    return borderLayoutPanel()
            .centerComponent(new JScrollPane(userHistoryTable))
            .southComponent(configBase)
            .build();
  }

  private JComponent createMaintenanceIntervalComponent() throws RemoteException {
    return comboBox(new DefaultComboBoxModel<>(MAINTENANCE_INTERVAL_VALUES))
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
    exceptionDialog()
            .owner(this)
            .show(exception);
  }
}
