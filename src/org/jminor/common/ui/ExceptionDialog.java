/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.Events;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.control.ToggleControl;
import org.jminor.common.ui.layout.FlexibleGridLayout;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;

/**
 * A JDialog for displaying information on exceptions.
 * Before you can use the ExceptionDialog email mechanism (Windows only) you need to call at least
 * ExceptionDialog.setErrorReportEmailFrom() and ExceptionDialog.setErrorReportEmailTo(),
 * to prefix a string to the subject field call ExceptionDialog.setErrorReportEmailSubjectPrefix()
 */
public final class ExceptionDialog extends JDialog {

  private static final int DESCRIPTION_LABEL_WIDTH = 250;
  private static final int MESSAGE_LABEL_WIDTH = 50;
  private static final int SCROLL_PANE_WIDTH = 500;
  private static final int SCROLL_PANE_HEIGHT = 200;

  //ui components
  private JTextField exceptionField;
  private JTextArea messageArea;
  private JPanel detailPanel;
  private final Window ownerFrame;
  private JPanel centerPanel;
  private JTextArea detailsArea;
  private JLabel descriptionLabel;
  private JButton btnPrint;
  private JButton btnSave;
  private JButton btnCopy;
  private JButton btnEmail;
  //controls
  private ToggleControl ctrDetails;
  private Control ctrClose;
  private Control ctrPrint;
  private Control ctrSave;
  private Control ctrCopy;
  private Control ctrEmail;

  private final Event evtShowDetailsChanged = Events.event();
  private boolean showDetails = false;

  private static String lastUsedEmailAddress = "";
  private static String errorReportEmailSubjectPrefix = "";

  public ExceptionDialog(final Window owner) {
    super(owner);
    ownerFrame = owner;
    setupControls();
    bindEvents();
    initUI();
  }

  public static void showExceptionDialog(final Window parentFrame, final String title, final Throwable throwable) {
    showExceptionDialog(parentFrame, title, throwable.getMessage(), throwable);
  }

  public static void showExceptionDialog(final Window parentFrame, final String title, final String message,
                                         final Throwable throwable) {
    showExceptionDialog(parentFrame, title, message, throwable, true);
  }

  public static void showExceptionDialog(final Window parentFrame, final String title, final String message,
                                         final Throwable throwable, final boolean modal) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        new ExceptionDialog(parentFrame).showForThrowable(title, message, throwable, modal);
      }
      else {
        SwingUtilities.invokeAndWait(new Runnable() {
          @Override
          public void run() {
            new ExceptionDialog(parentFrame).showForThrowable(title, message, throwable, modal);
          }
        });
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void showForThrowable(final String title, final String message, final Throwable t) {
    showForThrowable(title, message, t, true);
  }

  public void showForThrowable(final String title, final String message, final Throwable throwable, final boolean modal) {
    setModal(modal);
    setTitle(title);

    final String name = translateExceptionClass(throwable.getClass());
    descriptionLabel.setText(message == null ? name : truncateMessage(message));
    descriptionLabel.setToolTipText(message);

    exceptionField.setText(name);
    messageArea.setText(throwable.getMessage());

    final StringWriter sw = new StringWriter();
    throwable.printStackTrace(new PrintWriter(sw));

    detailsArea.setText(null);
    detailsArea.append(sw.toString());
    detailsArea.append("\n");
    detailsArea.append("--------------------------------------------Properties--------------------------------------------\n\n");

    final String propsString = Util.getSystemProperties();
    detailsArea.append(propsString);

    detailsArea.setCaretPosition(0);
    initDetailView(false);
    setVisible(true);
  }

  private String truncateMessage(final String message) {
    if (message.length() > 100) {
      return message.substring(0, 100) + "...";
    }

    return message;
  }

  private String translateExceptionClass(final Class<? extends Throwable> exceptionClass) {
    if (exceptionClass.equals(DatabaseException.class)) {
      return Messages.get(Messages.DATABASE_EXCEPTION);
    }

    return exceptionClass.getSimpleName();
  }

  public void close() {
    setVisible(false);
  }

  /**
   * @return true if show details is active
   */
  public boolean isShowDetails() {
    return showDetails;
  }

  /**
   * @param show true if the exception details should be visible
   */
  public void setShowDetails(final boolean show) {
    showDetails = show;
    evtShowDetailsChanged.fire();
  }

  /**
   * @param errorReportSubjectPrefix a string to prefix to the subject of the error report when emailed
   */
  public static void setErrorReportEmailSubjectPrefix(final String errorReportSubjectPrefix) {
    errorReportEmailSubjectPrefix = errorReportSubjectPrefix;
  }

  public void printErrorReport() {
    try {
      detailsArea.print();
    }
    catch (PrinterException e) {
      throw new RuntimeException(e);
    }
  }

  public void saveErrorReport() throws IOException {
    try {
      Util.writeFile(detailsArea.getText(), UiUtil.chooseFileToSave(detailsArea, null, null));
    }
    catch (CancelException e) {
      //cancelled
    }
  }

  public void copyErrorReport() {
    UiUtil.setClipboard(detailsArea.getText());
  }

  public void emailErrorReport() {
    final String address = JOptionPane.showInputDialog(Messages.get(Messages.INPUT_EMAIL_ADDRESS), lastUsedEmailAddress);
    if (Util.nullOrEmpty(address)) {
      return;
    }
    lastUsedEmailAddress = address;
    try {
      final String uriStr = String.format("mailto:%s?subject=%s&body=%s", address,
              URLEncoder.encode(errorReportEmailSubjectPrefix + descriptionLabel.getText(), "UTF-8").replace("+", "%20"),
              URLEncoder.encode(detailsArea.getText(), "UTF-8").replace("+", "%20"));
      Desktop.getDesktop().browse(new URI(uriStr));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void setupControls() {
    ctrDetails = Controls.toggleControl(this, "showDetails",
            Messages.get(Messages.DETAILS), null);
    ctrDetails.setDescription(Messages.get(Messages.SHOW_DETAILS));
    ctrPrint = Controls.methodControl(this, "printErrorReport",
            Messages.get(Messages.PRINT), null,
            Messages.get(Messages.PRINT_ERROR_REPORT),
            Messages.get(Messages.PRINT_ERROR_REPORT_MNEMONIC).charAt(0));
    ctrClose = Controls.methodControl(this, "close",
            Messages.get(Messages.CLOSE), null,
            Messages.get(Messages.CLOSE_DIALOG),
            Messages.get(Messages.CLOSE_MNEMONIC).charAt(0));
    ctrSave = Controls.methodControl(this, "saveErrorReport",
            Messages.get(Messages.SAVE), null,
            Messages.get(Messages.SAVE_ERROR_LOG),
            Messages.get(Messages.SAVE_MNEMONIC).charAt(0));
    ctrCopy = Controls.methodControl(this,"copyErrorReport",
            Messages.get(Messages.COPY), null,
            Messages.get(Messages.COPY_TO_CLIPBOARD),
            Messages.get(Messages.COPY_MNEMONIC).charAt(0));
    ctrEmail = Controls.methodControl(this,"emailErrorReport",
            Messages.get(Messages.SEND), null,
            Messages.get(Messages.SEND_EMAIL),
            Messages.get(Messages.SEND_MNEMONIC).charAt(0));
  }

  private void initUI() {
    final AbstractAction closeAction = new AbstractAction("close") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        close();
      }
    };
    UiUtil.addKeyEvent(getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, closeAction);
    final JPanel basePanel = new JPanel(UiUtil.createBorderLayout());
    basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    basePanel.add(createNorthPanel(), BorderLayout.NORTH);
    centerPanel = createCenterPanel();
    basePanel.add(centerPanel, BorderLayout.CENTER);
    basePanel.add(createButtonPanel(), BorderLayout.SOUTH);

    getContentPane().setLayout(UiUtil.createBorderLayout());
    getContentPane().add(basePanel, BorderLayout.CENTER);
  }

  private void bindEvents() {
    evtShowDetailsChanged.addListener(new EventAdapter() {
      /** {@inheritDoc} */
      @Override
      public void eventOccurred() {
        initDetailView(isShowDetails());
      }
    });
  }

  private void initDetailView(final boolean show) {
    btnPrint.setVisible(show);
    btnSave.setVisible(show);
    btnCopy.setVisible(show);
    btnEmail.setVisible(show);
    detailPanel.setVisible(show);
    centerPanel.setVisible(show);
    pack();
    detailPanel.revalidate();
    if (ownerFrame != null && ownerFrame.isVisible()) {
      positionOverFrame();
    }
    else {
      UiUtil.centerWindow(this);
    }
  }

  private JPanel createNorthPanel() {
    final FlexibleGridLayout layout = UiUtil.createFlexibleGridLayout(2, 2, true, false);
    layout.setFixedRowHeight(new JTextField().getPreferredSize().height);
    detailPanel = new JPanel(layout);
    descriptionLabel = new JLabel(UIManager.getIcon("OptionPane.errorIcon"), SwingConstants.CENTER);
    descriptionLabel.setMaximumSize(new Dimension(DESCRIPTION_LABEL_WIDTH, descriptionLabel.getMaximumSize().height));
    descriptionLabel.setIconTextGap(10);
    final JLabel exceptionLabel = new JLabel(
            Messages.get(Messages.EXCEPTION) + ": ", SwingConstants.LEFT);
    exceptionField = new JTextField();
    exceptionField.setEnabled(false);
    final JLabel messageLabel = new JLabel(
            Messages.get(Messages.MESSAGE) + ": ", SwingConstants.LEFT);
    messageLabel.setPreferredSize(new Dimension(MESSAGE_LABEL_WIDTH, messageLabel.getPreferredSize().height));
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

    final JPanel northPanel = new JPanel(UiUtil.createBorderLayout());
    final JPanel northNorthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    northNorthPanel.add(descriptionLabel);
    northPanel.add(northNorthPanel, BorderLayout.NORTH);
    northPanel.add(detailPanel, BorderLayout.CENTER);

    return northPanel;
  }

  private JPanel createCenterPanel() {
    detailsArea = new JTextArea();
    detailsArea.setFont(new Font("Dialog", Font.PLAIN, 9));
    detailsArea.setTabSize(4);
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
    final JPanel baseButtonPanel = new JPanel(new BorderLayout());
    final JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    final JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    final JButton btnClose = ControlProvider.createButton(ctrClose);
    btnPrint = ControlProvider.createButton(ctrPrint);
    btnSave = ControlProvider.createButton(ctrSave);
    btnCopy = ControlProvider.createButton(ctrCopy);
    btnEmail = ControlProvider.createButton(ctrEmail);
    rightButtonPanel.add(btnEmail);
    rightButtonPanel.add(btnCopy);
    rightButtonPanel.add(btnPrint);
    rightButtonPanel.add(btnSave);
    rightButtonPanel.add(btnClose);
    leftButtonPanel.add(ControlProvider.createCheckBox(ctrDetails));

    baseButtonPanel.add(leftButtonPanel, BorderLayout.WEST);
    baseButtonPanel.add(rightButtonPanel, BorderLayout.CENTER);

    getRootPane().setDefaultButton(btnClose);

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
}
