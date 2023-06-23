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
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.CheckBoxBuilder;
import is.codion.swing.common.ui.component.panel.BorderLayoutPanelBuilder;
import is.codion.swing.common.ui.component.panel.PanelBuilder;
import is.codion.swing.common.ui.component.scrollpane.ScrollPaneBuilder;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.awt.event.KeyEvent.VK_ESCAPE;
import static javax.swing.BorderFactory.createEmptyBorder;

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
    printButton = ButtonBuilder.builder(Control.builder(detailsArea::print)
                    .name(Messages.print())
                    .description(MESSAGES.getString("print_error_report"))
                    .mnemonic(MESSAGES.getString("print_error_report_mnemonic").charAt(0)))
            .build();
    saveButton = ButtonBuilder.builder(Control.builder(this::saveDetails)
                    .name(MESSAGES.getString("save"))
                    .description(MESSAGES.getString("save_error_log"))
                    .mnemonic(MESSAGES.getString("save_mnemonic").charAt(0)))
            .build();
    copyButton = ButtonBuilder.builder(Control.builder(() -> Utilities.setClipboard(detailsArea.getText()))
                    .name(Messages.copy())
                    .description(MESSAGES.getString("copy_to_clipboard"))
                    .mnemonic(MESSAGES.getString("copy_mnemonic").charAt(0)))
            .build();
    centerPanel = createCenterPanel();
    detailPanel = PanelBuilder.builder(FlexibleGridLayout.builder()
                    .rowsColumns(2, 2)
                    .fixedRowHeight(exceptionField.getPreferredSize().height)
                    .build())
            .add(exceptionField)
            .add(ScrollPaneBuilder.builder(messageArea)
                    .horizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
                    .verticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
                    .build())
            .build();
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
    add(BorderLayoutPanelBuilder.builder(borderLayout())
            .border(createEmptyBorder(BORDER_SIZE, BORDER_SIZE, BORDER_SIZE, BORDER_SIZE))
            .northComponent(createNorthPanel())
            .centerComponent(centerPanel)
            .southComponent(createButtonPanel())
            .build(), BorderLayout.CENTER);
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
    return BorderLayoutPanelBuilder.builder(borderLayout())
            .centerComponent(descriptionLabel)
            .build();
  }

  private JPanel createCenterPanel() {
    return BorderLayoutPanelBuilder.builder(new BorderLayout())
            .centerComponent(ScrollPaneBuilder.builder(detailsArea)
                    .preferredSize(new Dimension(SCROLL_PANE_WIDTH, SCROLL_PANE_HEIGHT))
                    .build())
            .build();
  }

  private JPanel createButtonPanel() {
    Control closeControl = Control.builder(closeEvent::onEvent)
            .name(MESSAGES.getString("close"))
            .description(MESSAGES.getString("close_dialog"))
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
    westPanel.add(CheckBoxBuilder.builder(showDetailsState)
            .text(MESSAGES.getString("details"))
            .toolTipText(MESSAGES.getString("show_details"))
            .build());
    JPanel centerButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    centerButtonPanel.add(copyButton);
    centerButtonPanel.add(printButton);
    centerButtonPanel.add(saveButton);
    centerButtonPanel.add(ButtonBuilder.builder(closeControl).build());

    return BorderLayoutPanelBuilder.builder(new BorderLayout())
            .westComponent(westPanel)
            .centerComponent(centerButtonPanel)
            .build();
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
