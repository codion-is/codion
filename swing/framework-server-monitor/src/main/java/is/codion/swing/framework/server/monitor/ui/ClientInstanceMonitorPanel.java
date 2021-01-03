/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.DateFormats;
import is.codion.common.user.User;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.value.TextValues;
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
import javax.swing.text.BadLocationException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.KeyEvents.KeyTrigger.ON_KEY_PRESSED;
import static is.codion.swing.common.ui.KeyEvents.addKeyEvent;
import static is.codion.swing.common.ui.control.Controls.*;
import static java.util.Objects.requireNonNull;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateFormats.FULL_TIMESTAMP);
  private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

  private final ClientInstanceMonitor model;

  private final JTextField creationDateField = new JTextField();
  private final JCheckBox loggingEnabledCheckBox = new JCheckBox("Logging enabled");
  private final JTextArea logArea = new JTextArea();
  private final JTree treeLog = new JTree();
  private final JTextField searchField = new JTextField(12);

  /**
   * Instantiates a new ClientInstanceMonitorPanel
   * @param model the model
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitorPanel(final ClientInstanceMonitor model) throws RemoteException {
    this.model = requireNonNull(model);
    loggingEnabledCheckBox.setModel(model.getLoggingEnabledButtonModel());
    logArea.setDocument(model.getLogDocument());
    logArea.setHighlighter(model.getLogHighlighter());
    treeLog.setModel(model.getLogTreeModel());
    model.getSearchStringValue().link(TextValues.textValue(searchField));
    model.getCurrentSearchTextPosition().addDataListener(currentSearchPosition -> {
      if (currentSearchPosition != null) {
        try {
          logArea.scrollRectToVisible(logArea.modelToView(currentSearchPosition));
        }
        catch (final BadLocationException e) {
          throw new RuntimeException(e);
        }
      }
    });
    initializeUI();
    updateView();
  }

  public void updateView() throws RemoteException {
    creationDateField.setText(model == null ? "" : DATE_TIME_FORMATTER.format(model.getCreationDate()));
    model.refreshLog();
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
    settingsPanel.add(new JButton(control(this::updateView, "Refresh log")));
    infoBase.add(settingsPanel, BorderLayout.EAST);
    add(infoBase, BorderLayout.NORTH);

    logArea.setLineWrap(false);
    logArea.setEditable(false);
    logArea.setComponentPopupMenu(popupMenu(controlList(control(this::saveLogToFile, "Save to file..."))));

    addKeyEvent(searchField, KeyEvent.VK_DOWN, 0, 0, ON_KEY_PRESSED, control(this::scrollToNextSearchPosition));
    addKeyEvent(searchField, KeyEvent.VK_UP, 0, 0, ON_KEY_PRESSED, control(this::scrollToPreviousSearchPosition));

    final JPanel searchPanel = new JPanel(Layouts.borderLayout());
    searchPanel.add(new JLabel("Search"), BorderLayout.WEST);
    searchPanel.add(searchField, BorderLayout.CENTER);

    final JPanel textLogPanel = new JPanel(Layouts.borderLayout());
    textLogPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
    textLogPanel.add(searchPanel, BorderLayout.SOUTH);

    treeLog.setRootVisible(false);
    final JScrollPane treeScrollPane = new JScrollPane(treeLog);

    final JTabbedPane logPane = new JTabbedPane(SwingConstants.TOP);
    logPane.add(textLogPanel, "Text");
    logPane.add(treeScrollPane, "Tree");

    add(logPane, BorderLayout.CENTER);
  }

  private void scrollToNextSearchPosition() {
    model.nextSearchPosition();
  }

  private void scrollToPreviousSearchPosition() {
    model.previousSearchPosition();
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
