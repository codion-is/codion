/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.tools.generator;

import is.codion.common.db.database.Database;
import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.icon.Logos;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.tools.generator.DefinitionRow;
import is.codion.swing.framework.model.tools.generator.DomainGeneratorModel;
import is.codion.swing.framework.model.tools.metadata.MetaDataSchema;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.InputEvent;
import java.util.Arrays;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.component.Components.*;
import static is.codion.swing.common.ui.dialog.Dialogs.lookAndFeelSelectionDialog;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.defaultLookAndFeelName;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.findLookAndFeelProvider;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_ENTER;
import static java.util.Objects.requireNonNull;

public final class DomainGeneratorPanel extends JPanel {

  private static final double RESIZE_WEIGHT = 0.2;

  private final DomainGeneratorModel model;

  /**
   * Instantiates a new DomainGeneratorPanel.
   * @param model the domain generator model to base this panel on
   */
  DomainGeneratorPanel(DomainGeneratorModel model) {
    this.model = requireNonNull(model);
    Control populateSchemaControl = Control.builder(this::populateSchema)
            .name("Populate")
            .enabled(model.schemaModel().selectionModel().selectionNotEmpty())
            .build();
    FilteredTable<MetaDataSchema, Integer> schemaTable =
            FilteredTable.builder(model.schemaModel())
                    .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
                    .doubleClickAction(populateSchemaControl)
                    .keyEvent(KeyEvents.builder(VK_ENTER)
                            .modifiers(InputEvent.CTRL_DOWN_MASK)
                            .action(populateSchemaControl))
                    .popupMenuControls(table -> Controls.builder()
                            .control(populateSchemaControl)
                            .control(createToggleColumnsControls(table))
                            .controls(table.createAutoResizeModeControl())
                            .build())
                    .build();

    FilteredTable<DefinitionRow, Integer> domainTable =
            FilteredTable.builder(model.definitionModel())
                    .autoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
                    .popupMenuControl(FilteredTable::createAutoResizeModeControl)
                    .build();

    JSplitPane schemaTableSplitPane = splitPane()
            .orientation(JSplitPane.VERTICAL_SPLIT)
            .resizeWeight(RESIZE_WEIGHT)
            .topComponent(new JScrollPane(schemaTable))
            .bottomComponent(new JScrollPane(domainTable))
            .build();

    JTextArea textArea = textArea()
            .rowsColumns(40, 60)
            .editable(false)
            .build();

    Font font = textArea.getFont();
    textArea.setFont(new Font(Font.MONOSPACED, font.getStyle(), font.getSize()));

    JPanel textAreaCopyPanel = borderLayoutPanel()
            .centerComponent(new JScrollPane(textArea))
            .southComponent(flowLayoutPanel(FlowLayout.RIGHT)
                    .add(button(Control.builder(() -> Utilities.setClipboard(textArea.getText()))
                            .name(Messages.copy()))
                            .build())
                    .build())
            .build();

    JSplitPane splitPane = splitPane()
            .resizeWeight(RESIZE_WEIGHT)
            .leftComponent(schemaTableSplitPane)
            .rightComponent(textAreaCopyPanel)
            .build();

    setLayout(borderLayout());
    add(splitPane, BorderLayout.CENTER);

    model.domainSourceObserver().addDataListener(textArea::setText);
  }

  public void showFrame() {
    Windows.frame(this)
            .title("Codion Database Explorer")
            .icon(Logos.logoTransparent())
            .menuBar(menu(createMainMenuControls()).createMenuBar())
            .defaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
            .onClosing(windowEvent -> model.close())
            .centerFrame(true)
            .show();
  }

  private void populateSchema() {
    JLabel schemaLabel = new JLabel("Testing", SwingConstants.CENTER);
    JPanel northPanel = borderLayoutPanel()
            .centerComponent(schemaLabel)
            .build();
    Consumer<String> schemaNotifier = schema -> SwingUtilities.invokeLater(() -> schemaLabel.setText(schema));
    Dialogs.progressWorkerDialog(() -> model.populateSelected(schemaNotifier))
            .owner(this)
            .title("Populating")
            .northPanel(northPanel)
            .onResult(model.schemaModel()::refresh)
            .execute();
  }

  private Controls createMainMenuControls() {
    return Controls.builder()
            .controls(Controls.builder()
                    .name("File")
                    .mnemonic('F')
                    .control(Control.builder(() -> System.exit(0))
                            .name("Exit")
                            .mnemonic('X')))
            .controls(Controls.builder()
                    .name("View")
                    .mnemonic('V')
                    .control(lookAndFeelSelectionDialog()
                            .owner(this)
                            .userPreferencePropertyName(DomainGeneratorPanel.class.getName())
                            .createControl()))
            .build();
  }

  private static Control createToggleColumnsControls(FilteredTable<MetaDataSchema, Integer> table) {
    Controls toggleColumnsControls = table.createToggleColumnsControls();
    toggleColumnsControls.setName("Columns...");

    return toggleColumnsControls;
  }

  /**
   * Runs a DomainGeneratorPanel instance in a frame
   * @param arguments no arguments required
   */
  public static void main(String[] arguments) {
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    findLookAndFeelProvider(defaultLookAndFeelName(DomainGeneratorPanel.class.getName()))
            .ifPresent(LookAndFeelProvider::enable);
    try {
      Database database = Database.instance();
      DomainGeneratorModel explorerModel = DomainGeneratorModel.domainGeneratorModel(database,
              Dialogs.loginDialog()
                      .icon(Logos.logoTransparent())
                      .validator(user -> database.createConnection(user).close())
                      .show());
      SwingUtilities.invokeLater(() -> new DomainGeneratorPanel(explorerModel).showFrame());
    }
    catch (CancelException ignored) {
      System.exit(0);
    }
    catch (Exception e) {
      Dialogs.displayExceptionDialog(e, null);
      System.exit(1);
    }
  }
}
