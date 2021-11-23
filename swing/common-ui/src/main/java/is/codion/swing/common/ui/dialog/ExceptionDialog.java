/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.i18n.Messages;
import is.codion.common.properties.PropertyStore;
import is.codion.common.state.State;
import is.codion.common.value.PropertyValue;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.ComponentBuilders;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
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

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;

/**
 * A JDialog for displaying information on exceptions.
 */
final class ExceptionDialog extends JDialog {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ExceptionDialog.class.getName());

  /**
   * Specifies whether an ExceptionDialog should display system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES = Configuration.booleanValue("codion.swing.common.ui.ExceptionDialog.displaySystemProperties", true);

  private static final int DESCRIPTION_LABEL_WIDTH = 250;
  private static final int MESSAGE_LABEL_WIDTH = 50;
  private static final int SCROLL_PANE_WIDTH = 500;
  private static final int SCROLL_PANE_HEIGHT = 200;
  private static final int MAX_MESSAGE_LENGTH = 100;
  private static final int BORDER_SIZE = 5;
  private static final int ICON_TEXT_GAP = 10;
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

  private final State showDetailsState = State.state();

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
    final FlexibleGridLayout layout = FlexibleGridLayout.builder()
            .rowsColumns(2, 2)
            .fixRowHeights(true)
            .build();
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
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
    final JCheckBox detailsCheckBox = ComponentBuilders.checkBox()
            .caption(MESSAGES.getString("details"))
            .toolTipText(MESSAGES.getString("show_details"))
            .linkedValue(showDetailsState)
            .build();
    final Control printControl = Control.builder(() -> detailsArea.print())
            .caption(Messages.get(Messages.PRINT))
            .description(MESSAGES.getString("print_error_report"))
            .mnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0))
            .build();
    final Control closeControl = Control.builder(this::dispose)
            .caption(MESSAGES.getString("close"))
            .description(MESSAGES.getString("close_dialog"))
            .mnemonic(MESSAGES.getString("close_mnemonic").charAt(0))
            .build();
    final Control saveControl = Control.builder(() -> Files.write(new DefaultFileSelectionDialogBuilder()
                    .owner(detailsArea)
                    .selectFileToSave("error.txt")
                    .toPath(),
            Arrays.asList(detailsArea.getText().split("\\r?\\n"))))
            .caption(MESSAGES.getString("save"))
            .description(MESSAGES.getString("save_error_log"))
            .mnemonic(MESSAGES.getString("save_mnemonic").charAt(0))
            .build();
    final Control copyControl = Control.builder(() -> Components.setClipboard(detailsArea.getText()))
            .caption(Messages.get(Messages.COPY))
            .description(MESSAGES.getString("copy_to_clipboard"))
            .mnemonic(MESSAGES.getString("copy_mnemonic").charAt(0))
            .build();

    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(closeControl)
            .enable(getRootPane());
    KeyEvents.builder(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(closeControl)
            .enable(getRootPane());

    final JButton closeButton = closeControl.createButton();
    printButton = printControl.createButton();
    saveButton = saveControl.createButton();
    copyButton = copyControl.createButton();

    final JPanel baseButtonPanel = new JPanel(new BorderLayout());
    final JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    final JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    rightButtonPanel.add(copyButton);
    rightButtonPanel.add(printButton);
    rightButtonPanel.add(saveButton);
    rightButtonPanel.add(closeButton);
    leftButtonPanel.add(detailsCheckBox);

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

  ExceptionDialog showForThrowable(final Throwable throwable, final String title, final String message, final boolean modal) {
    setModal(modal);
    setTitle(title);

    final String name = throwable.getClass().getSimpleName();
    descriptionLabel.setText(message == null ? name : truncateMessage(message));
    descriptionLabel.setToolTipText(message);

    exceptionField.setText(name);
    messageArea.setText(throwable.getMessage());

    final StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));

    detailsArea.setText(null);
    detailsArea.append(stringWriter.toString());

    if (DISPLAY_SYSTEM_PROPERTIES.get()) {
      detailsArea.append("\n");
      detailsArea.append("--------------------------------------------Properties--------------------------------------------\n\n");
      detailsArea.append(PropertyStore.getSystemProperties());
    }

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
}
