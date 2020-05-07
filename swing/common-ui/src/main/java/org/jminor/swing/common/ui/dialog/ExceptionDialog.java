/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.dialog;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.common.ui.KeyEvents.OnKeyRelease;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.control.ToggleControl;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout.FixColumnWidths;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout.FixRowHeights;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ResourceBundle;

import static org.jminor.swing.common.ui.layout.Layouts.*;

/**
 * A JDialog for displaying information on exceptions.
 */
final class ExceptionDialog extends JDialog {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ExceptionDialog.class.getName());

  private static final int DESCRIPTION_LABEL_WIDTH = 250;
  private static final int MESSAGE_LABEL_WIDTH = 50;
  private static final int SCROLL_PANE_WIDTH = 500;
  private static final int SCROLL_PANE_HEIGHT = 200;
  private static final int MAX_MESSAGE_LENGTH = 100;
  private static final int BORDER_SIZE = 5;
  private static final int ICON_TEXT_GAP = 10;
  private static final int NORTH_PANEL_DIMENSIONS = 2;
  private static final int TAB_SIZE = 4;

  //ui components
  private final Window parentWindow;
  private JTextField exceptionField;
  private JTextArea messageArea;
  private JPanel detailPanel;
  private JPanel centerPanel;
  private JTextArea detailsArea;
  private JLabel descriptionLabel;
  private JButton printButton;
  private JButton saveButton;
  private JButton copyButton;

  private final State showDetailsState = States.state();

  /**
   * Instantiates a new ExceptionDialog with the given window as parent
   * @param parentWindow the dialog parent
   */
  ExceptionDialog(final Window parentWindow) {
    super(parentWindow);
    this.parentWindow = parentWindow;
    bindEvents();
    initializeUI();
  }

  private void initializeUI() {
    final JPanel basePanel = new JPanel(borderLayout());
    basePanel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
    basePanel.add(createNorthPanel(), BorderLayout.NORTH);
    centerPanel = createCenterPanel();
    basePanel.add(centerPanel, BorderLayout.CENTER);
    basePanel.add(createButtonPanel(), BorderLayout.SOUTH);

    getContentPane().setLayout(borderLayout());
    getContentPane().add(basePanel, BorderLayout.CENTER);
  }

  private void bindEvents() {
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    showDetailsState.addDataListener(this::initializeDetailView);
  }

  private void initializeDetailView(final boolean show) {
    printButton.setVisible(show);
    saveButton.setVisible(show);
    copyButton.setVisible(show);
    detailPanel.setVisible(show);
    centerPanel.setVisible(show);
    pack();
    detailPanel.revalidate();
    if (parentWindow != null && parentWindow.isVisible()) {
      positionOverFrame();
    }
    else {
      Windows.centerWindow(this);
    }
  }

  private JPanel createNorthPanel() {
    final FlexibleGridLayout layout =
            flexibleGridLayout(NORTH_PANEL_DIMENSIONS, NORTH_PANEL_DIMENSIONS, FixRowHeights.YES, FixColumnWidths.NO);
    layout.setFixedRowHeight(new JTextField().getPreferredSize().height);
    detailPanel = new JPanel(layout);
    descriptionLabel = new JLabel(UIManager.getIcon("OptionPane.errorIcon"), SwingConstants.CENTER);
    descriptionLabel.setMaximumSize(new Dimension(DESCRIPTION_LABEL_WIDTH, descriptionLabel.getMaximumSize().height));
    descriptionLabel.setIconTextGap(ICON_TEXT_GAP);
    final JLabel exceptionLabel = new JLabel(
            Messages.get(Messages.EXCEPTION) + ": ", SwingConstants.LEFT);
    exceptionField = new JTextField();
    exceptionField.setEnabled(false);
    final JLabel messageLabel = new JLabel(MESSAGES.getString("message") + ": ", SwingConstants.LEFT);
    Components.setPreferredWidth(messageLabel, MESSAGE_LABEL_WIDTH);
    messageArea = new JTextArea();
    messageArea.setEnabled(false);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setBackground(exceptionField.getBackground());
    messageArea.setBorder(exceptionField.getBorder());
    final JScrollPane messageScroller = new JScrollPane(messageArea,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    //detail
    detailPanel.add(exceptionLabel);
    detailPanel.add(exceptionField);
    detailPanel.add(messageLabel);
    detailPanel.add(messageScroller);

    final JPanel northPanel = new JPanel(borderLayout());
    final JPanel northNorthPanel = new JPanel(flowLayout(FlowLayout.LEFT));
    northNorthPanel.add(descriptionLabel);
    northPanel.add(northNorthPanel, BorderLayout.NORTH);
    northPanel.add(detailPanel, BorderLayout.CENTER);

    return northPanel;
  }

  private JPanel createCenterPanel() {
    detailsArea = new JTextArea();
    detailsArea.setTabSize(TAB_SIZE);
    detailsArea.setEditable(false);
    detailsArea.setLineWrap(true);
    detailsArea.setWrapStyleWord(true);

    final JScrollPane scrollPane = new JScrollPane(detailsArea);
    scrollPane.setPreferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT));
    final JPanel center = new JPanel(new BorderLayout());
    center.add(scrollPane, BorderLayout.CENTER);

    return center;
  }

  private JPanel createButtonPanel() {
    final ToggleControl detailsControl = Controls.toggleControl(showDetailsState);
    detailsControl.setName(MESSAGES.getString("details"));
    detailsControl.setDescription(MESSAGES.getString("show_details"));
    final Control printControl = Controls.control(() -> detailsArea.print(), Messages.get(Messages.PRINT));
    printControl.setDescription(MESSAGES.getString("print_error_report"));
    printControl.setMnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0));
    final Control closeControl = Controls.control(this::dispose, Messages.get(Messages.CLOSE));
    closeControl.setDescription(MESSAGES.getString("close_dialog"));
    closeControl.setMnemonic(MESSAGES.getString("close_mnemonic").charAt(0));
    KeyEvents.addKeyEvent(getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, OnKeyRelease.NO, closeControl);
    KeyEvents.addKeyEvent(getRootPane(), KeyEvent.VK_ENTER, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, OnKeyRelease.NO, closeControl);
    final Control saveControl = Controls.control(() ->
                    Files.write(Dialogs.selectFileToSave(detailsArea, null, "error.txt").toPath(),
                            Arrays.asList(detailsArea.getText().split("\\r?\\n"))),
            MESSAGES.getString("save"));
    saveControl.setDescription(MESSAGES.getString("save_error_log"));
    saveControl.setMnemonic(MESSAGES.getString("save_mnemonic").charAt(0));
    final Control copyControl = Controls.control(() -> Components.setClipboard(detailsArea.getText()), Messages.get(Messages.COPY));
    copyControl.setDescription(MESSAGES.getString("copy_to_clipboard"));
    copyControl.setMnemonic(MESSAGES.getString("copy_mnemonic").charAt(0));

    final JButton closeButton = new JButton(closeControl);
    printButton = new JButton(printControl);
    saveButton = new JButton(saveControl);
    copyButton = new JButton(copyControl);

    final JPanel baseButtonPanel = new JPanel(new BorderLayout());
    final JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    final JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    rightButtonPanel.add(copyButton);
    rightButtonPanel.add(printButton);
    rightButtonPanel.add(saveButton);
    rightButtonPanel.add(closeButton);
    leftButtonPanel.add(ControlProvider.createCheckBox(detailsControl));

    baseButtonPanel.add(leftButtonPanel, BorderLayout.WEST);
    baseButtonPanel.add(rightButtonPanel, BorderLayout.CENTER);

    return baseButtonPanel;
  }

  private void positionOverFrame() {
    final Point p = getOwner().getLocation();
    final Dimension d = getOwner().getSize();

    p.x += (d.width - getWidth()) >> 1;
    p.y += (d.height - getHeight()) >> 1;

    if (p.x < 0) {
      p.x = 0;
    }

    if (p.y < 0) {
      p.y = 0;
    }

    setLocation(p);
  }

  ExceptionDialog showForThrowable(final String title, final String message, final Throwable throwable, final Modal modal) {
    setModal(modal == Modal.YES);
    setTitle(title);

    final String name = translateExceptionClass(throwable.getClass());
    descriptionLabel.setText(message == null ? name : truncateMessage(message));
    descriptionLabel.setToolTipText(message);

    exceptionField.setText(name);
    messageArea.setText(throwable.getMessage());

    final StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));

    detailsArea.setText(null);
    detailsArea.append(stringWriter.toString());
    detailsArea.append("\n");
    detailsArea.append("--------------------------------------------Properties--------------------------------------------\n\n");

    final String propsString = Util.getSystemProperties();
    detailsArea.append(propsString);

    detailsArea.setCaretPosition(0);
    initializeDetailView(false);
    setVisible(true);

    return this;
  }

  private static String truncateMessage(final String message) {
    if (message.length() > MAX_MESSAGE_LENGTH) {
      return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    return message;
  }

  private static String translateExceptionClass(final Class<? extends Throwable> exceptionClass) {
    if (exceptionClass.equals(DatabaseException.class)) {
      return MESSAGES.getString("database_exception");
    }

    return exceptionClass.getSimpleName();
  }
}
