/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.Configuration;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.i18n.Messages;
import is.codion.common.properties.PropertyStore;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.Components.button;
import static is.codion.swing.common.ui.component.Components.checkBox;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.flowLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;

/**
 * A JDialog for displaying information on exceptions.
 */
final class ExceptionPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(ExceptionPanel.class.getName());

  /**
   * Specifies whether an ExceptionPanel should display system properties in the detail panel<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> DISPLAY_SYSTEM_PROPERTIES =
          Configuration.booleanValue("is.codion.swing.common.ui.dialog.ExceptionPanel.displaySystemProperties", true);

  private static final int DESCRIPTION_LABEL_WIDTH = 320;
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

  ExceptionPanel(Throwable throwable, String message) {
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
    printButton = button(Control.builder(detailsArea::print)
            .name(Messages.print())
            .description(MESSAGES.getString("print_error_report"))
            .mnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0))
            .build()).build();
    saveButton = button(Control.builder(this::saveDetails)
            .name(MESSAGES.getString("save"))
            .description(MESSAGES.getString("save_error_log"))
            .mnemonic(MESSAGES.getString("save_mnemonic").charAt(0))
            .build()).build();
    copyButton = button(Control.builder(() -> Utilities.setClipboard(detailsArea.getText()))
            .name(Messages.copy())
            .description(MESSAGES.getString("copy_to_clipboard"))
            .mnemonic(MESSAGES.getString("copy_mnemonic").charAt(0))
            .build()).build();
    centerPanel = createCenterPanel();
    detailPanel = new JPanel(FlexibleGridLayout.builder()
            .rowsColumns(2, 2)
            .fixedRowHeight(exceptionField.getPreferredSize().height)
            .build());
    showDetailsState.addDataListener(this::initializeDetailView);
    initializeUI();
    setException(throwable, message);
  }

  void addShowDetailsListener(EventDataListener<Boolean> showDetailsListener) {
    showDetailsState.addDataListener(showDetailsListener);
  }

  public EventObserver<?> closeObserver() {
    return closeEvent.observer();
  }

  private void initializeUI() {
    setLayout(borderLayout());
    JPanel panel = new JPanel(borderLayout());
    panel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE));
    panel.add(createNorthPanel(), BorderLayout.NORTH);
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(createButtonPanel(), BorderLayout.SOUTH);
    add(panel, BorderLayout.CENTER);
  }

  private void initializeDetailView(boolean showDetails) {
    printButton.setVisible(showDetails);
    saveButton.setVisible(showDetails);
    copyButton.setVisible(showDetails);
    detailPanel.setVisible(showDetails);
    centerPanel.setVisible(showDetails);
    detailPanel.revalidate();
  }

  private JPanel createNorthPanel() {
    detailPanel.add(exceptionField);
    detailPanel.add(new JScrollPane(messageArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));

    JPanel northPanel = new JPanel(flowLayout(FlowLayout.LEFT));
    JPanel panel = new JPanel(borderLayout());
    panel.add(northPanel, BorderLayout.NORTH);
    panel.add(descriptionLabel, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createCenterPanel() {
    JScrollPane scrollPane = new JScrollPane(detailsArea);
    scrollPane.setPreferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT));

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  private JPanel createButtonPanel() {
    Control closeControl = Control.builder(closeEvent::onEvent)
            .name(MESSAGES.getString("close"))
            .description(MESSAGES.getString("close_dialog"))
            .build();
    ToggleControl detailsControl = ToggleControl.builder(showDetailsState)
            .name(MESSAGES.getString("details"))
            .description(MESSAGES.getString("show_details"))
            .build();
    KeyEvents.builder(VK_ESCAPE)
            .condition(WHEN_IN_FOCUSED_WINDOW)
            .action(closeControl)
            .enable(this);
    KeyEvents.builder(VK_ENTER)
            .condition(WHEN_IN_FOCUSED_WINDOW)
            .action(closeControl)
            .enable(this);

    JPanel westPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    westPanel.add(checkBox(detailsControl).build());
    JPanel centerButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    centerButtonPanel.add(copyButton);
    centerButtonPanel.add(printButton);
    centerButtonPanel.add(saveButton);
    centerButtonPanel.add(button(closeControl).build());
    JPanel panel = new JPanel(new BorderLayout());

    panel.add(westPanel, BorderLayout.WEST);
    panel.add(centerButtonPanel, BorderLayout.CENTER);

    return panel;
  }

  void setException(Throwable throwable, String message) {
    String name = throwable.getClass().getSimpleName();
    descriptionLabel.setText(message == null ? name : truncateMessage(message));
    descriptionLabel.setToolTipText(message);

    exceptionField.setText(name);
    messageArea.setText(throwable.getMessage());

    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));

    detailsArea.setText(null);
    detailsArea.append(stringWriter.toString());

    if (DISPLAY_SYSTEM_PROPERTIES.get()) {
      detailsArea.append("\n");
      detailsArea.append("--------------------------------------------Properties--------------------------------------------\n\n");
      detailsArea.append(PropertyStore.systemProperties());
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

  private static String truncateMessage(String message) {
    if (message.length() > MAX_MESSAGE_LENGTH) {
      return message.substring(0, MAX_MESSAGE_LENGTH) + "...";
    }

    return message;
  }
}
