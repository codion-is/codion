/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor.ui;

import is.codion.common.formats.LocaleDateTimePattern;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.textfield.SearchHighlighter;
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
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.requireNonNull;

/**
 * A ClientInstanceMonitorPanel
 */
public final class ClientInstanceMonitorPanel extends JPanel {

  private static final DateTimeFormatter DATE_TIME_FORMATTER = LocaleDateTimePattern.builder()
          .delimiterDash().yearFourDigits().hoursMinutesSeconds()
          .build().getFormatter();
  private static final DateTimeFormatter DATE_TIME_FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");

  private final ClientInstanceMonitor model;
  private final JTextArea logTextArea;
  private final SearchHighlighter searchHighlighter;

  private final JTextField creationDateField = ComponentBuilders.textField()
          .editable(false)
          .build();

  /**
   * Instantiates a new ClientInstanceMonitorPanel
   * @param model the model
   * @throws RemoteException in case of an exception
   */
  public ClientInstanceMonitorPanel(final ClientInstanceMonitor model) throws RemoteException {
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
    final JPanel creationDatePanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    creationDatePanel.add(new JLabel("Creation date"));
    creationDatePanel.add(creationDateField);

    final JPanel settingsPanel = new JPanel(Layouts.flowLayout(FlowLayout.LEFT));
    settingsPanel.add(ComponentBuilders.checkBox()
            .caption("Logging enabled")
            .linkedValue(model.getLoggingEnabledValue())
            .build());
    settingsPanel.add(Control.builder(this::updateView)
            .caption("Refresh log")
            .build().createButton());

    final JPanel northPanel = new JPanel(Layouts.borderLayout());
    northPanel.setBorder(BorderFactory.createTitledBorder("Connection info"));
    northPanel.add(creationDatePanel, BorderLayout.CENTER);
    northPanel.add(settingsPanel, BorderLayout.EAST);

    final JPanel searchPanel = new JPanel(Layouts.borderLayout());
    searchPanel.add(new JLabel("Search"), BorderLayout.WEST);
    searchPanel.add(searchHighlighter.createSearchField(), BorderLayout.CENTER);

    final JPanel textLogPanel = new JPanel(Layouts.borderLayout());
    textLogPanel.add(new JScrollPane(logTextArea), BorderLayout.CENTER);
    textLogPanel.add(searchPanel, BorderLayout.SOUTH);

    final JTabbedPane centerPane = new JTabbedPane(SwingConstants.TOP);
    centerPane.add(textLogPanel, "Text");
    centerPane.add(new JScrollPane(createLogTree()), "Tree");

    setLayout(Layouts.borderLayout());
    add(northPanel, BorderLayout.NORTH);
    add(centerPane, BorderLayout.CENTER);
  }

  private JTree createLogTree() {
    final JTree treeLog = new JTree(model.getLogTreeModel());
    treeLog.setRootVisible(false);

    return treeLog;
  }

  private JTextArea createLogTextArea() {
    final JTextArea textArea = new JTextArea(model.getLogDocument());
    textArea.setEditable(false);
    textArea.setLineWrap(false);
    textArea.setWrapStyleWord(true);
    final State lineWrapState = State.state();
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

  private void saveLogToFile(final JTextArea logArea) throws IOException {
    if (creationDateField.getText().isEmpty()) {
      throw new IllegalStateException("No client selected");
    }

    final User user = model.getRemoteClient().getUser();
    final LocalDateTime creationDate = LocalDateTime.parse(creationDateField.getText(), DATE_TIME_FORMATTER);
    final String filename = user.getUsername() + "@" + DATE_TIME_FILENAME_FORMATTER.format(creationDate) + ".log";

    Files.write(Dialogs.fileSelectionDialogBuilder()
            .owner(this)
            .selectFileToSave(filename).toPath(), logArea.getText().getBytes());
  }
}
