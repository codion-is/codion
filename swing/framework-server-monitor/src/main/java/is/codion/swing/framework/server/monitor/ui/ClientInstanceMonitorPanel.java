/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.DateFormats;
import is.codion.common.MethodLogger;
import is.codion.common.rmi.server.ClientLog;
import is.codion.common.user.User;
import is.codion.swing.common.ui.control.ControlProvider;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP);
  private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

  private final JTextField creationDateField = new JTextField();
  private final JCheckBox loggingEnabledCheckBox = new JCheckBox("Logging enabled");
  private final JTextArea logArea = new JTextArea();
  private final JTree treeLog = new JTree();

  private ClientInstanceMonitor model;

  /**
   * Instantiates a new ClientInstanceMonitorPanel
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitorPanel() throws RemoteException {
    initializeUI();
    updateView();
  }

  public void setModel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = model;
    loggingEnabledCheckBox.setModel(model.getLoggingEnabledButtonModel());
    treeLog.setModel(model.getLogTreeModel());
    updateView();
  }

  public void updateView() throws RemoteException {
    final StringBuilder log = new StringBuilder();
    if (model != null) {
      final ClientLog serverLog = model.getLog();
      if (serverLog != null) {
        for (final MethodLogger.Entry entry : serverLog.getEntries()) {
          entry.append(log);
        }
      }
      else {
        log.append("Disconnected!");
      }
      final LocalDateTime creationDate = model.getCreationDate();
      creationDateField.setText(creationDate == null ? "unknown" : DATE_TIME_FORMATTER.format(creationDate));
      model.refreshLogTreeModel();
    }
    else {
      creationDateField.setText("");
    }
    logArea.setText(log.toString());
  }

  private void initializeUI() {
    setLayout(Layouts.borderLayout());
    creationDateField.setEditable(false);
    final JPanel infoPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    infoPanel.add(new JLabel("Creation date"));
    infoPanel.add(creationDateField);
    final JPanel infoBase = new JPanel(Layouts.borderLayout());
    infoBase.setBorder(BorderFactory.createTitledBorder("Connection info"));
    infoBase.add(infoPanel, BorderLayout.CENTER);
    final JPanel settingsPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    settingsPanel.add(loggingEnabledCheckBox);
    settingsPanel.add(new JButton(Controls.control(this::updateView, "Refresh log")));
    infoBase.add(settingsPanel, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    logArea.setLineWrap(false);
    logArea.setEditable(false);
    logArea.setComponentPopupMenu(ControlProvider.createPopupMenu(
            Controls.controlList(Controls.control(this::saveLogToFile, "Save to file..."))));
    final JScrollPane scrollPane = new JScrollPane(logArea);

    treeLog.setRootVisible(false);
    final JScrollPane treeScrollPane = new JScrollPane(treeLog);

    final JTabbedPane logPane = new JTabbedPane(SwingConstants.TOP);
    logPane.add(scrollPane, "Text");
    logPane.add(treeScrollPane, "Tree");

    add(logPane, BorderLayout.CENTER);
  }

  private void saveLogToFile() throws IOException {
    if (creationDateField.getText().isEmpty()) {
      throw new IllegalStateException("No client selected");
    }

    final User user = model.getRemoteClient().getUser();
    final LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);
    final String filename = user.getUsername() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";

    Files.write(Dialogs.selectFileToSave(this, null, filename).toPath(), logArea.getText().getBytes());
  }
}
