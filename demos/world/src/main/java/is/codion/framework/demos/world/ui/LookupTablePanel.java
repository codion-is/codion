package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.LookupTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.kordamp.ikonli.foundation.Foundation;
import org.kordamp.ikonli.swing.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static is.codion.swing.common.ui.dialog.Dialogs.selectFileToSave;
import static is.codion.swing.common.ui.worker.ProgressWorker.runWithProgressBar;
import static is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons.ICON_SIZE;
import static is.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons.imageIcon;

public final class LookupTablePanel extends EntityTablePanel {

  public LookupTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setConditionPanelVisible(true);
  }

  @Override
  protected ControlList getPopupControls(List<ControlList> additionalPopupControls) {
    ControlList controls = super.getPopupControls(additionalPopupControls);
    controls.addSeparatorAt(1);

    Control exportControl = Control.controlBuilder()
            .command(this::exportCSV).name("Export CSV...")
            .icon(imageIcon(FontIcon.of(Foundation.PAGE_EXPORT_CSV, ICON_SIZE)))
            .build();

    controls.addAt(2, exportControl);

    return controls;
  }

  private void exportCSV() throws IOException {
    File fileToSave = selectFileToSave(this, null, "export.csv");
    runWithProgressBar(this, () -> ((LookupTableModel) getTableModel()).exportCSV(fileToSave),
            "Exporting data", "Export successful", "Export failed");
  }
}
