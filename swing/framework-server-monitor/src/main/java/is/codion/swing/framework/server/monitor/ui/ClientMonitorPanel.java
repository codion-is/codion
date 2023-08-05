/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;
import is.codion.swing.framework.server.monitor.ClientMonitor;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.rmi.RemoteException;

import static is.codion.swing.common.ui.Utilities.linkBoundedRangeModels;
import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER;
import static javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER;

/**
 * A ClientMonitorPanel
 */
public final class ClientMonitorPanel extends JPanel {

  private final ClientMonitor model;
  private final FilteredTable<RemoteClient, Integer> clientInstanceTable;
  private final JScrollPane filterScrollPane;
  private final JScrollPane clientInstanceScroller;
  private final State advancedFilterState = State.state();

  /**
   * Instantiates a new ClientMonitorPanel
   * @param model the model
   */
  public ClientMonitorPanel(ClientMonitor model) {
    this.model = model;
    clientInstanceTable = FilteredTable.builder(model.clientInstanceTableModel())
            .popupMenu(createPopupMenu())
            .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
            .build();
    clientInstanceScroller = scrollPane(clientInstanceTable)
            .border(BorderFactory.createTitledBorder("Clients"))
            .build();
    filterScrollPane = createLinkedScrollPane(clientInstanceScroller, clientInstanceTable.filterPanel());
    advancedFilterState.addDataListener(this::toggleAdvancedFilters);
    initializeUI();
  }

  public ClientMonitor model() {
    return model;
  }

  public void refresh() {
    model.refresh();
  }

  private void initializeUI() {
    JPanel clientInstanceBase = borderLayoutPanel()
            .northComponent(filterScrollPane)
            .centerComponent(clientInstanceScroller)
            .southComponent(borderLayoutPanel()
                    .southComponent(borderLayoutPanel()
                            .centerComponent(clientInstanceTable.searchField())
                            .eastComponent(flexibleGridLayoutPanel(1, 2)
                                    .add(checkBox(advancedFilterState)
                                            .text("Advanced filters")
                                            .build())
                                    .add(button(control(this::refresh))
                                            .text("Refresh")
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    JPanel clientInstancePanel = borderLayoutPanel().build();
    JSplitPane splitPane = splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .oneTouchExpandable(true)
            .continuousLayout(true)
            .leftComponent(clientInstanceBase)
            .rightComponent(clientInstancePanel)
            .build();

    model.clientInstanceTableModel().selectionModel().addSelectedItemListener(remoteClient -> {
      clientInstancePanel.removeAll();
      try {
        if (model != null && remoteClient != null) {
          ClientInstanceMonitorPanel clientMonitor = new ClientInstanceMonitorPanel(new ClientInstanceMonitor(model.server(), remoteClient));
          clientInstancePanel.add(clientMonitor, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
      }
      catch (RemoteException ex) {
        throw new RuntimeException(ex);
      }
    });
    setLayout(borderLayout());
    add(splitPane, BorderLayout.CENTER);
  }

  private JPopupMenu createPopupMenu() {
    return menu(Controls.builder()
            .control(Control.builder(this::disconnect)
                    .name("Disconnect")
                    .enabledObserver(model.clientInstanceTableModel().selectionModel().selectionNotEmptyObserver()))
            .build())
            .createPopupMenu();
  }

  private void disconnect() throws RemoteException {
    for (RemoteClient remoteClient : model.clientInstanceTableModel().selectionModel().getSelectedItems()) {
      model.server().disconnect(remoteClient.clientId());
      model.clientInstanceTableModel().removeItem(remoteClient);
    }
  }

  private void toggleAdvancedFilters(Boolean advanced) {
    clientInstanceTable.filterPanel().advancedViewState().set(advanced);
    revalidate();
  }

  private static JScrollPane createLinkedScrollPane(JScrollPane parentScrollPane, JPanel panelToScroll) {
    return Components.scrollPane(panelToScroll)
            .horizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
            .verticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
            .onBuild(scrollPane -> linkBoundedRangeModels(
                    parentScrollPane.getHorizontalScrollBar().getModel(),
                    scrollPane.getHorizontalScrollBar().getModel()))
            .build();
  }
}
