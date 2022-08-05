/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.SearchHighlighter;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.server.monitor.ClientInstanceMonitor;

import javax.swing.BorderFactory;
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
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static is.codion.swing.common.ui.control.Control.control;
import static java.util.Objects.requireNonNull;

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

  private final JTextField creationDateField = Components.textField()
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
    this.searchHighlighter = SearchHighlighter.searchHighlighter(logTextArea);
    initializeUI();
    updateView();
  }

  public void updateView() throws RemoteException {
    creationDateField.setText(DATE_TIME_FORMATTER.format(model.getCreationDate()));
    model.refreshLog();
  }

  private void initializeUI() {
    JPanel creationDatePanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    creationDatePanel.add(new JLabel("Creation date"));
    creationDatePanel.add(creationDateField);

    JPanel settingsPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    Components.checkBox(model.getLoggingEnabledState())
            .caption("Logging enabled")
            .build(settingsPanel::add);
    Components.button(control(this::updateView))
            .caption("Refresh log")
            .build(settingsPanel::add);

    JPanel northPanel = new JPanel(Layouts.borderLayout());
    northPanel.setBorder(BorderFactory.createTitledBorder("Connection info"));
    northPanel.add(creationDatePanel, BorderLayout.CENTER);
    northPanel.add(settingsPanel, BorderLayout.EAST);

    JPanel searchPanel = new JPanel(Layouts.borderLayout());
    searchPanel.add(new JLabel("Search"), BorderLayout.WEST);
    searchPanel.add(searchHighlighter.createSearchField(), BorderLayout.CENTER);

    JPanel textLogPanel = new JPanel(Layouts.borderLayout());
    textLogPanel.add(new JScrollPane(logTextArea), BorderLayout.CENTER);
    textLogPanel.add(searchPanel, BorderLayout.SOUTH);

    JTabbedPane centerPane = new JTabbedPane(SwingConstants.TOP);
    centerPane.add(textLogPanel, "Text");
    centerPane.add(new JScrollPane(createLogTree()), "Tree");

    setLayout(Layouts.borderLayout());
    add(northPanel, BorderLayout.NORTH);
    add(centerPane, BorderLayout.CENTER);
  }

  private JTree createLogTree() {
    JTree treeLog = new JTree(model.getLogTreeModel());
    treeLog.setRootVisible(false);

    return treeLog;
  }

  private JTextArea createLogTextArea() {
    JTextArea textArea = new JTextArea(model.getLogDocument());
    Font font = textArea.getFont();
    textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));
    textArea.setEditable(false);
    textArea.setLineWrap(false);
    textArea.setWrapStyleWord(true);
    State lineWrapState = State.state();
    lineWrapState.addDataListener(textArea::setLineWrap);
    textArea.setComponentPopupMenu(Controls.builder()
            .control(Control.builder(() -> saveLogToFile(textArea))
                    .caption("Save to file..."))
            .separator()
            .control(ToggleControl.builder(lineWrapState)
                    .caption("Line wrap"))
            .build().createPopupMenu());

    return textArea;
  }

  private void saveLogToFile(JTextArea logArea) throws IOException {
    if (creationDateField.getText().isEmpty()) {
      throw new IllegalStateException("No client selected");
    }

    User user = model.getRemoteClient().user();
    LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);
    String filename = user.getUsername() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";

    Files.write(Dialogs.fileSelectionDialog()
            .owner(this)
            .selectFileToSave(filename).toPath(), logArea.getText().getBytes());
  }
}
