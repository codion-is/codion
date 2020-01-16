package org.jminor.swing.common.ui.dialog;

import org.jminor.common.Util;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.control.ToggleControl;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.common.ui.layout.Layouts;

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
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.jminor.common.Util.nullOrEmpty;

/**
 * A JDialog for displaying information on exceptions.
 * Before you can use the ExceptionDialog email mechanism (Windows only) you need to call at least
 * ExceptionDialog.setErrorReportEmailFrom() and ExceptionDialog.setErrorReportEmailTo(),
 * to prefix a string to the subject field call ExceptionDialog.setErrorReportEmailSubjectPrefix()
 */
final class ExceptionDialog extends JDialog {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ExceptionDialog.class.getName(), Locale.getDefault());

  private static final int DESCRIPTION_LABEL_WIDTH = 250;
  private static final int MESSAGE_LABEL_WIDTH = 50;
  private static final int SCROLL_PANE_WIDTH = 500;
  private static final int SCROLL_PANE_HEIGHT = 200;
  private static final int MAX_MESSAGE_LENGTH = 100;
  private static final int BORDER_SIZE = 5;
  private static final int ICON_TEXT_GAP = 10;
  private static final int NORTH_PANEL_DIMENSIONS = 2;
  private static final int COMPONENT_GAP = 0;
  private static final int TAB_SIZE = 4;

  //ui components
  private JTextField exceptionField;
  private JTextArea messageArea;
  private JPanel detailPanel;
  private final Window window;
  private JPanel centerPanel;
  private JTextArea detailsArea;
  private JLabel descriptionLabel;
  private JButton printButton;
  private JButton saveButton;
  private JButton copyButton;
  private JButton emailButton;
  //controls
  private ToggleControl detailsControl;
  private Control closeControl;
  private Control printControl;
  private Control saveControl;
  private Control copyControl;
  private Control emailControl;

  private final State showDetailsState = States.state();

  /**
   * Instantiates a new ExceptionDialog with the given window as parent
   * @param window the dialog parent
   */
  ExceptionDialog(final Window window) {
    super(window);
    this.window = window;
    setupControls();
    bindEvents();
    initializeUI();
  }

  private void setupControls() {
    detailsControl = Controls.toggleControl(showDetailsState);
    detailsControl.setName(MESSAGES.getString("details"));
    detailsControl.setDescription(MESSAGES.getString("show_details"));
    printControl = Controls.control(() -> detailsArea.print(), Messages.get(Messages.PRINT));
    printControl.setDescription(MESSAGES.getString("print_error_report"));
    printControl.setMnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0));
    closeControl = Controls.control(this::dispose, Messages.get(Messages.CLOSE));
    closeControl.setDescription(MESSAGES.getString("close_dialog"));
    closeControl.setMnemonic(MESSAGES.getString("close_mnemonic").charAt(0));
    saveControl = Controls.control(() ->
                    Files.write(Dialogs.selectFileToSave(detailsArea, null, null).toPath(),
                            Arrays.asList(detailsArea.getText().split("\\r?\\n"))),
            MESSAGES.getString("save"));
    saveControl.setDescription(MESSAGES.getString("save_error_log"));
    saveControl.setMnemonic(MESSAGES.getString("save_mnemonic").charAt(0));
    copyControl = Controls.control(() -> UiUtil.setClipboard(detailsArea.getText()), Messages.get(Messages.COPY));
    copyControl.setDescription(MESSAGES.getString("copy_to_clipboard"));
    copyControl.setMnemonic(MESSAGES.getString("copy_mnemonic").charAt(0));
    emailControl = Controls.control(this::emailErrorReport, MESSAGES.getString("send"));
    emailControl.setDescription(MESSAGES.getString("send_email"));
    emailControl.setMnemonic(MESSAGES.getString("send_mnemonic").charAt(0));
  }

  private void initializeUI() {
    KeyEvents.addKeyEvent(getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, Controls.control(this::dispose));
    final JPanel basePanel = new JPanel(Layouts.createBorderLayout());
    basePanel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
    basePanel.add(createNorthPanel(), BorderLayout.NORTH);
    centerPanel = createCenterPanel();
    basePanel.add(centerPanel, BorderLayout.CENTER);
    basePanel.add(createButtonPanel(), BorderLayout.SOUTH);

    getContentPane().setLayout(Layouts.createBorderLayout());
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
    emailButton.setVisible(show);
    detailPanel.setVisible(show);
    centerPanel.setVisible(show);
    pack();
    detailPanel.revalidate();
    if (window != null && window.isVisible()) {
      positionOverFrame();
    }
    else {
      UiUtil.centerWindow(this);
    }
  }

  private JPanel createNorthPanel() {
    final FlexibleGridLayout layout = Layouts.createFlexibleGridLayout(NORTH_PANEL_DIMENSIONS, NORTH_PANEL_DIMENSIONS, true, false);
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
    UiUtil.setPreferredWidth(messageLabel, MESSAGE_LABEL_WIDTH);
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

    final JPanel northPanel = new JPanel(Layouts.createBorderLayout());
    final JPanel northNorthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
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
    final JPanel baseButtonPanel = new JPanel(new BorderLayout());
    final JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, COMPONENT_GAP, COMPONENT_GAP));
    final JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, COMPONENT_GAP, COMPONENT_GAP));

    final JButton closeButton = new JButton(closeControl);
    printButton = new JButton(printControl);
    saveButton = new JButton(saveControl);
    copyButton = new JButton(copyControl);
    emailButton = new JButton(emailControl);
    rightButtonPanel.add(emailButton);
    rightButtonPanel.add(copyButton);
    rightButtonPanel.add(printButton);
    rightButtonPanel.add(saveButton);
    rightButtonPanel.add(closeButton);
    leftButtonPanel.add(ControlProvider.createCheckBox(detailsControl));

    baseButtonPanel.add(leftButtonPanel, BorderLayout.WEST);
    baseButtonPanel.add(rightButtonPanel, BorderLayout.CENTER);

    getRootPane().setDefaultButton(closeButton);

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

  void showForThrowable(final String title, final String message, final Throwable throwable, final boolean modal) {
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
    initializeDetailView(false);
    setVisible(true);
  }

  /**
   * Uses "mailto" to create an email containing the current error report to a specified recipient
   */
  private void emailErrorReport() {
    final String address = JOptionPane.showInputDialog(MESSAGES.getString("input_email_address"));
    if (nullOrEmpty(address)) {
      return;
    }
    try {
      final String uriStr = String.format("mailto:%s?subject=%s&body=%s", address,
              URLEncoder.encode(descriptionLabel.getText(), "UTF-8").replace("+", "%20"),
              URLEncoder.encode(detailsArea.getText(), "UTF-8").replace("+", "%20"));
      Desktop.getDesktop().browse(new URI(uriStr));
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
