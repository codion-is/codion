/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;
import org.jminor.framework.server.monitor.ClientMonitor;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;

/**
 * A ClientMonitorPanel
 */
public final class ClientMonitorPanel extends JPanel {

  private ClientMonitor model;
  private final ClientInstanceMonitorPanel clientInstancePanel;
  private final JList<ClientInstanceMonitor> clientInstanceList = new JList<>();

  /**
   * Instantiates a new ClientMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public ClientMonitorPanel() throws RemoteException {
    this.clientInstancePanel = new ClientInstanceMonitorPanel();
    initializeUI();
  }

  public void setModel(final ClientMonitor model) {
    this.model = model;
    if (model != null) {
      clientInstanceList.setModel(model.getClientInstanceListModel());
    }
  }

  public void refresh() throws RemoteException {
    if (model != null) {
      model.refresh();
    }
  }

  private void initializeUI() {
    setLayout(UiUtil.createBorderLayout());
    clientInstanceList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    clientInstanceList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(final ListSelectionEvent e) {
        try {
          final ClientInstanceMonitor clientMonitor = clientInstanceList.getSelectedValue();
          if (clientMonitor != null) {
            clientInstancePanel.setModel(clientMonitor);
            repaint();
          }
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
    clientInstanceList.setComponentPopupMenu(initializePopupMenu());

    final JPanel clientInstanceBase = new JPanel(UiUtil.createBorderLayout());
    final JScrollPane clientInstanceScroller = new JScrollPane(clientInstanceList);
    clientInstanceScroller.setBorder(BorderFactory.createTitledBorder("Clients"));
    clientInstanceBase.add(clientInstanceScroller, BorderLayout.CENTER);
    clientInstanceBase.add(ControlProvider.createButton(Controls.methodControl(this, "refresh", "Refresh")), BorderLayout.SOUTH);

    final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPane.setOneTouchExpandable(true);
    splitPane.setLeftComponent(clientInstanceBase);
    splitPane.setRightComponent(clientInstancePanel);

    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu initializePopupMenu() {
    final ControlSet controls = new ControlSet();
    controls.add(new AbstractAction("Disconnect") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final ClientInstanceMonitor clientMonitor = clientInstanceList.getSelectedValue();
        if (clientMonitor != null) {
          try {
            clientMonitor.disconnect();
            model.getClientInstanceListModel().removeElement(clientMonitor);
          }
          catch (RemoteException ex) {
            throw new RuntimeException(ex);
          }
        }
      }
    });

    return ControlProvider.createPopupMenu(controls);
  }
}
