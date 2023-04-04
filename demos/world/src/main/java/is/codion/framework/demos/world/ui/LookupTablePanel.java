package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.LookupTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icons.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JTable;
import java.io.File;
import java.io.IOException;
import java.util.List;

final class LookupTablePanel extends EntityTablePanel {

  LookupTablePanel(SwingEntityTableModel lookupModel) {
    super(lookupModel);
    table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    setConditionPanelVisible(true);
    setShowRefreshProgressBar(true);
    setControl(ControlCode.CLEAR, Control.builder(this::clearTableAndConditions)
            .caption("Clear")
            .mnemonic('C')
            .smallIcon(FrameworkIcons.instance().clear())
            .build());
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return super.createPopupMenuControls(additionalPopupMenuControls)
            .addSeparatorAt(2)
            .addAt(3, Control.builder(this::exportCSV)
                    .caption("Export CSV...")
                    .smallIcon(FrameworkIcons.instance().getIcon(Foundation.PAGE_EXPORT_CSV))
                    .build());
  }

  private void exportCSV() throws IOException {
    File fileToSave = Dialogs.fileSelectionDialog()
            .owner(this)
            .selectFileToSave("export.csv");
    Dialogs.progressWorkerDialog(() -> ((LookupTableModel) tableModel()).exportCSV(fileToSave))
            .owner(this)
            .title("Exporting data")
            .onResult("Export successful")
            .onException("Export failed")
            .execute();
  }

  private void clearTableAndConditions() {
    tableModel().clear();
    tableModel().tableConditionModel().clearConditions();
  }
}
