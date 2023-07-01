package is.codion.framework.demos.world.ui;

import is.codion.common.state.State;
import is.codion.framework.demos.world.model.LookupTableModel;
import is.codion.framework.demos.world.model.LookupTableModel.ExportFormat;
import is.codion.swing.common.ui.component.button.ButtonBuilder;
import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static is.codion.framework.demos.world.model.LookupTableModel.ExportFormat.CSV;
import static is.codion.framework.demos.world.model.LookupTableModel.ExportFormat.JSON;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static javax.swing.BorderFactory.createTitledBorder;

final class LookupTablePanel extends EntityTablePanel {

  private final JScrollPane columnSelectionPanel = scrollPane(createColumnSelectionToolBar())
          .verticalUnitIncrement(16)
          .build();
  private final State columnSelectionPaneVisibleState = State.state(true);

  LookupTablePanel(SwingEntityTableModel lookupModel) {
    super(lookupModel);
    columnSelectionPaneVisibleState.addDataListener(this::setColumnSelectionPanelVisible);
    table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    setConditionPanelVisible(true);
    setShowRefreshProgressBar(true);
    setupControls();
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    FrameworkIcons icons = FrameworkIcons.instance();

    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addSeparatorAt(2)
            .addAt(3, Controls.builder()
                    .name("Export")
                    .smallIcon(icons.icon(Foundation.PAGE_EXPORT))
                    .control(Control.builder(this::exportCSV)
                            .name("CSV..."))
                    .control(Control.builder(this::exportJSON)
                            .name("JSON..."))
                    .build())
            .addAt(4, Controls.builder()
                    .name("Import")
                    .smallIcon(icons.icon(Foundation.PAGE_ADD))
                    .control(Control.builder(this::importJSON)
                            .name("JSON..."))
                    .build())
            .addSeparatorAt(7)
            .addAt(8, ToggleControl.builder(columnSelectionPaneVisibleState)
                    .name("Select columns")
                    .build());
  }

  @Override
  protected void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    super.layoutPanel(tablePanel, southPanel);
    add(columnSelectionPanel, BorderLayout.EAST);
  }

  private void exportCSV() {
    export(CSV);
  }

  private void exportJSON() {
    export(JSON);
  }

  private void export(ExportFormat format) {
    File fileToSave = Dialogs.fileSelectionDialog()
            .owner(this)
            .selectFileToSave(format.defaultFileName());
    LookupTableModel lookupTableModel = tableModel();
    Dialogs.progressWorkerDialog(() -> lookupTableModel.export(fileToSave, format))
            .owner(this)
            .title("Exporting data")
            .onResult("Export successful")
            .onException("Export failed")
            .execute();
  }

  private void importJSON() throws IOException {
    File file = Dialogs.fileSelectionDialog()
            .owner(this)
            .fileFilter(new FileNameExtensionFilter("JSON", "json"))
            .selectFile();
    LookupTableModel tableModel = tableModel();
    tableModel.importJSON(file);
  }

  private JToolBar createColumnSelectionToolBar() {
    Controls toggleColumnsControls = table().createToggleColumnsControls();

    return toolBar(Controls.controls()
            .add(createSelectAllColumnsControl(toggleColumnsControls))
            .addSeparator()
            .addAll(toggleColumnsControls))
            .orientation(SwingConstants.VERTICAL)
            .toggleButtonType(ToggleButtonType.CHECKBOX)
            .buttonBuilder(ButtonBuilder.builder().includeText(true))
            .border(createTitledBorder("Columns"))
            .build();
  }

  private void setColumnSelectionPanelVisible(boolean visible) {
    columnSelectionPanel.setVisible(visible);
    revalidate();
  }

  private void setupControls() {
    setControl(ControlCode.CLEAR, Control.builder(this::clearTableAndConditions)
            .name("Clear")
            .mnemonic('C')
            .smallIcon(FrameworkIcons.instance().clear())
            .build());
    //Get rid of the default column menu
    setControl(ControlCode.SELECT_COLUMNS, null);
    setControl(ControlCode.RESET_COLUMNS, null);
  }

  private void clearTableAndConditions() {
    tableModel().clear();
    tableModel().conditionModel().clear();
  }

  private static Control createSelectAllColumnsControl(Controls toggleColumnsControls) {
    return Control.builder(() -> toggleColumnsControls.actions().stream()
                    .map(ToggleControl.class::cast)
                    .forEach(toggleControl -> toggleControl.value().set(true)))
            .name("Select all")
            .smallIcon(FrameworkIcons.instance().icon(Foundation.CHECK))
            .build();
  }
}
