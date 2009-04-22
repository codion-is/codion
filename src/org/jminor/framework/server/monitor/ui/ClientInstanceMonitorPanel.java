/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server.monitor.ui;

import org.jminor.common.db.DbLog;
import org.jminor.common.db.LogEntry;
import org.jminor.common.model.formats.FullDateFormat;
import org.jminor.common.ui.IPopupProvider;
import org.jminor.common.ui.control.ControlFactory;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * User: Björn Darri
 * Date: 4.12.2007
 * Time: 22:49:42
 */
public class ClientInstanceMonitorPanel extends JPanel implements IPopupProvider {

  private final ClientInstanceMonitor model;
  private final JTextField txtCreationDate = new JTextField();
  private final JTextArea txtLog = new JTextArea();
  private JPopupMenu popupMenu;

  public ClientInstanceMonitorPanel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = model;
    initUI();
    updateView();
    model.evtRefreshed.addListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        try {
          updateView();
        }
        catch (RemoteException ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  public void disconnectSelected() throws RemoteException {
    throw new RuntimeException("ClientInstanceMonitorPanel.disconnectSelected() has not been implemented");
  }

  public JPopupMenu getPopupMenu() {
    if (popupMenu == null)
      popupMenu = ControlProvider.createPopupMenu(getPopupCommands());

    return popupMenu;
  }

  public void updateView() throws RemoteException {
    final StringBuffer log = new StringBuffer();
    final DbLog dbLog = model.getLog();
    if (dbLog != null)
      for (final LogEntry logEntry : sortAndRemoveNullEntries(dbLog.log))
        log.append(logEntry.toString());
    else
      log.append("Disconnected!");

    txtLog.setText(log.toString());
  }

  private List<LogEntry> sortAndRemoveNullEntries(List<LogEntry> log) {
    Collections.sort(log);
    final ListIterator<LogEntry> iterator = log.listIterator();
    while (iterator.hasNext())
      if (iterator.next().entryTime == 0)
        iterator.remove();

    return log;
  }

  private void initUI() throws RemoteException {
    setLayout(new BorderLayout(5,5));
    txtCreationDate.setText(FullDateFormat.get().format(new Date(model.getCreationDate())));
    txtCreationDate.setEditable(false);
    final JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5,5));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(txtCreationDate);
    final JPanel infoBase = new JPanel(new BorderLayout(5,5));
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    infoBase.add(ControlProvider.createButton(
            ControlFactory.methodControl(this, "updateView", "Refresh log")), BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    txtLog.setLineWrap(false);
    final JScrollPane scrollPane = new JScrollPane(txtLog);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Connection log"));

    add(scrollPane, BorderLayout.CENTER);
  }

  private ControlSet getPopupCommands() {
    final ControlSet ret = new ControlSet();
    ret.add(ControlFactory.methodControl(this, "disconnectSelected", "Disconnect selected"));

    return ret;
  }
}
