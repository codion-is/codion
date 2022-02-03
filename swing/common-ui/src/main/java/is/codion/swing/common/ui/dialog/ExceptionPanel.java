/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.properties.PropertyStore;
import is.codion.common.state.State;
import is.codion.common.value.PropertyValue;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
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
import java.awt.event.KeyEvent;
import java.io.IOException;
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
final class ExceptionPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ExceptionPanel.class.getName());

  /**
   * Specifies whether an ExceptionDialog should display system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES =
          Configuration.booleanValue("codion.swing.common.ui.ExceptionDialog.displaySystemProperties", true);

  private static final int DESCRIPTION_LABEL_WIDTH = 250;
  private static final int MESSAGE_LABEL_WIDTH = 50;
  private static final int SCROLL_PANE_WIDTH = 500;
  private static final int SCROLL_PANE_HEIGHT = 200;
  private static final int MAX_MESSAGE_LENGTH = 100;
  private static final int BORDER_SIZE = 5;
  private static final int ICON_TEXT_GAP = 10;
  private static final int TAB_SIZE = 4;

  private final JTextField exceptionField;
  private final JTextArea messageArea;
  private final JTextArea detailsArea;
  private final JLabel descriptionLabel;
  private final JButton printButton;
  private final JButton saveButton;
  private final JButton copyButton;
  private final JPanel detailPanel;
  private final JPanel centerPanel;

  private final State showDetailsState = State.state();
  private final Event<?> closeEvent = Event.event();

  ExceptionPanel(final Throwable throwable, final String message) {
    exceptionField = new JTextField();
    exceptionField.setEnabled(false);
    messageArea = new JTextArea();
    messageArea.setEnabled(false);
    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setBackground(exceptionField.getBackground());
    messageArea.setBorder(exceptionField.getBorder());
    detailsArea = new JTextArea();
    detailsArea.setTabSize(TAB_SIZE);
    detailsArea.setEditable(false);
    descriptionLabel = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
    Sizes.setPreferredWidth(descriptionLabel, DESCRIPTION_LABEL_WIDTH);
    descriptionLabel.setIconTextGap(ICON_TEXT_GAP);
    printButton = Control.builder(detailsArea::print)
            .caption(Messages.get(Messages.PRINT))
            .description(MESSAGES.getString("print_error_report"))
            .mnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0))
            .build().createButton();
    saveButton = Control.builder(this::saveDetails)
            .caption(MESSAGES.getString("save"))
            .description(MESSAGES.getString("save_error_log"))
            .mnemonic(MESSAGES.getString("save_mnemonic").charAt(0))
            .build().createButton();
    copyButton = Control.builder(() -> Utilities.setClipboard(detailsArea.getText()))
            .caption(Messages.get(Messages.COPY))
            .description(MESSAGES.getString("copy_to_clipboard"))
            .mnemonic(MESSAGES.getString("copy_mnemonic").charAt(0))
            .build().createButton();
    centerPanel = createCenterPanel();
    detailPanel = new JPanel(FlexibleGridLayout.builder()
            .rowsColumns(2, 2)
            .fixedRowHeight(exceptionField.getPreferredSize().height)
            .build());
    showDetailsState.addDataListener(this::initializeDetailView);
    initializeUI();
    setException(throwable, message);
  }

  void addDetailsListener(final EventDataListener<Boolean> detailsListener) {
    showDetailsState.addDataListener(detailsListener);
  }

  public EventObserver<?> getCloseObserver() {
    return closeEvent.getObserver();
  }

  private void initializeUI() {
    final JPanel basePanel = new JPanel(borderLayout());
    basePanel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
    basePanel.add(createNorthPanel(), BorderLayout.NORTH);
    basePanel.add(centerPanel, BorderLayout.CENTER);
    basePanel.add(createButtonPanel(), BorderLayout.SOUTH);

    setLayout(borderLayout());
    add(basePanel, BorderLayout.CENTER);
  }

  private void initializeDetailView(final boolean show) {
    printButton.setVisible(show);
    saveButton.setVisible(show);
    copyButton.setVisible(show);
    detailPanel.setVisible(show);
    centerPanel.setVisible(show);
    detailPanel.revalidate();
  }

  private JPanel createNorthPanel() {
    final JLabel label = new JLabel(Messages.get(Messages.EXCEPTION) + ": ");
    label.setHorizontalAlignment(SwingConstants.LEFT);
    detailPanel.add(label);
    detailPanel.add(exceptionField);
    final JLabel message = new JLabel(MESSAGES.getString("message") + ": ");
    message.setHorizontalAlignment(SwingConstants.LEFT);
    Sizes.setPreferredWidth(message, MESSAGE_LABEL_WIDTH);
    detailPanel.add(message);
    detailPanel.add(new JScrollPane(messageArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

    final JPanel northPanel = new JPanel(borderLayout());
    final JPanel northNorthPanel = new JPanel(flowLayout(FlowLayout.LEFT));
    northNorthPanel.add(descriptionLabel);
    northPanel.add(northNorthPanel, BorderLayout.NORTH);
    northPanel.add(detailPanel, BorderLayout.CENTER);

    return northPanel;
  }

  private JPanel createCenterPanel() {
    final JScrollPane scrollPane = new JScrollPane(detailsArea);
    scrollPane.setPreferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT));
    final JPanel center = new JPanel(new BorderLayout());
    center.add(scrollPane, BorderLayout.CENTER);

    return center;
  }

  private JPanel createButtonPanel() {
    final Control closeControl = Control.builder(closeEvent::onEvent)
            .caption(MESSAGES.getString("close"))
            .description(MESSAGES.getString("close_dialog"))
            .build();
    final ToggleControl detailsControl = ToggleControl.builder(showDetailsState)
            .caption(MESSAGES.getString("details"))
            .description(MESSAGES.getString("show_details"))
            .build();
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(closeControl)
            .enable(this);
    KeyEvents.builder(KeyEvent.VK_ENTER)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(closeControl)
            .enable(this);

    final JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    rightButtonPanel.add(copyButton);
    rightButtonPanel.add(printButton);
    rightButtonPanel.add(saveButton);
    rightButtonPanel.add(closeControl.createButton());

    final JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    leftButtonPanel.add(detailsControl.createCheckBox());

    final JPanel baseButtonPanel = new JPanel(new BorderLayout());
    baseButtonPanel.add(leftButtonPanel, BorderLayout.WEST);
    baseButtonPanel.add(rightButtonPanel, BorderLayout.CENTER);

    return baseButtonPanel;
  }

  void setException(final Throwable throwable, final String message) {
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
  }

  private void saveDetails() throws IOException {
    Files.write(new DefaultFileSelectionDialogBuilder()
                    .owner(detailsArea)
                    .selectFileToSave("error.txt")
                    .toPath(),
            Arrays.asList(detailsArea.getText().split("\\r?\\n")));
  }

  private static String truncateMessage(final String message) {
    if (message.length() > MAX_MESSAGE_LENGTH) {
      return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    return message;
  }
}
