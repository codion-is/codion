/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.format.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.component.text.SearchHighlighter.searchHighlighter;
import static is.codion.swing.common.ui.control.Control.control;
import static is.codion.swing.common.ui.dialog.Dialogs.fileSelectionDialog;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = LocaleDateTimePattern.builder()
          .delimiterDash().yearFourDigits().hoursMinutesSeconds()
          .build().createFormatter();
  private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

  private final ClientInstanceMonitor model;
  private final JTextArea logTextArea;
  private final SearchHighlighter searchHighlighter;
  private final JTextField searchField;

  private final JTextField creationDateField = textField()
          .editable(false)
          .build();

  /**
   * Instantiates a new ClientInstanceMonitorPanel
   * @param model the model
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitorPanel(ClientInstanceMonitor model) throws RemoteException {
    this.model = requireNonNull(model);
    this.logTextArea = createLogTextArea();
    this.searchHighlighter = searchHighlighter(logTextArea);
    this.searchField = searchHighlighter.createSearchField();
    KeyEvents.builder(KeyEvent.VK_F)
            .modifiers(InputEvent.CTRL_DOWN_MASK)
            .action(Control.control(searchField::requestFocusInWindow))
            .enable(logTextArea);
    initializeUI();
    updateView();
  }

  public void updateView() throws RemoteException {
    creationDateField.setText(DATE_TIME_FORMATTER.format(model.remoteClient().creationTime()));
    model.refreshLog();
  }

  private void initializeUI() {
    JPanel creationDatePanel = flowLayoutPanel(FlowLayout.LEFT)
            .add(new JLabel("Creation time"))
            .add(creationDateField)
            .build();

    JPanel settingsPanel = flowLayoutPanel(FlowLayout.LEFT)
            .add(checkBox(model.loggingEnabled())
                    .text("Logging enabled")
                    .build())
            .add(button(control(this::updateView))
                    .text("Refresh log")
                    .build())
            .build();

    JPanel northPanel = borderLayoutPanel()
            .border(createTitledBorder("Connection info"))
            .centerComponent(creationDatePanel)
            .eastComponent(settingsPanel)
            .build();

    JPanel textLogPanel = borderLayoutPanel()
            .centerComponent(new JScrollPane(logTextArea))
            .southComponent(searchField)
            .build();

    JTabbedPane centerPane = tabbedPane()
            .tab("Text", textLogPanel)
            .tab("Tree", new JScrollPane(createLogTree()))
            .build();

    setLayout(borderLayout());
    add(northPanel, BorderLayout.NORTH);
    add(centerPane, BorderLayout.CENTER);
  }

  private JTree createLogTree() {
    JTree treeLog = new JTree(model.logTreeModel());
    treeLog.setRootVisible(false);

    return treeLog;
  }

  private JTextArea createLogTextArea() {
    JTextArea textArea = textArea()
            .document(model.logDocument())
            .editable(false)
            .wrapStyleWord(true)
            .build();
    Font font = textArea.getFont();
    textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
    State lineWrapState = State.state();
    lineWrapState.addDataListener(textArea::setLineWrap);
    textArea.setComponentPopupMenu(menu(Controls.builder()
            .control(Control.builder(() -> saveLogToFile(textArea))
                    .name("Save to file..."))
            .separator()
            .control(ToggleControl.builder(lineWrapState)
                    .name("Line wrap"))
            .build())
            .createPopupMenu());

    return textArea;
  }

  private void saveLogToFile(JTextArea logArea) throws IOException {
    if (creationDateField.getText().isEmpty()) {
      throw new IllegalStateException("No client selected");
    }

    User user = model.remoteClient().user();
    LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);
    String filename = user.username() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";

    Files.write(fileSelectionDialog()
            .owner(this)
            .selectFileToSave(filename).toPath(), logArea.getText().getBytes());
  }
}
