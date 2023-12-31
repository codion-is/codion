/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.model.EmployeeEditModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.model.worker.ProgressWorker.Task;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.dialog.ProgressWorkerDialogBuilder;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityDialogs.EditDialogBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.util.Collection;

import static is.codion.framework.demos.chinook.ui.EmployeeEditPanel.DELETING;
import static is.codion.framework.demos.chinook.ui.EmployeeEditPanel.UPDATING;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;

public final class EmployeeTablePanel extends EntityTablePanel {

  public EmployeeTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    // Remove the CTRL modifier from the DELETE key shortcut for this table panel
    configure().keyStroke(KeyboardShortcut.DELETE_SELECTED)
            .map(keyStroke -> keyStroke(keyStroke.getKeyCode()));
  }

  @Override
  protected void setupControls() {
    control(TableControl.DELETE_SELECTED).map(delete -> delete.copy(new DeleteCommand()).build());
  }

  @Override
  protected <T> EditDialogBuilder<T> editDialogBuilder(Attribute<T> attribute) {
    return super.editDialogBuilder(attribute)
            .updater(new EmployeeUpdater());
  }

  private final class DeleteCommand implements Command {

    @Override
    public void execute() {
      if (confirmDelete()) {
        EmployeeEditModel editModel = tableModel().editModel();
        EmployeeEditModel.DeleteMany delete = editModel.createDelete(tableModel().selectionModel().getSelectedItems());
        createWorker(delete::execute, DELETING)
                .onResult(delete::onResult)
                .execute();
      }
    }
  }

  private final class EmployeeUpdater implements EditDialogBuilder.Updater {

    @Override
    public void update(SwingEntityEditModel editModel, Collection<Entity> entities) {
      EmployeeEditModel employeeEditModel = (EmployeeEditModel) editModel;
      EmployeeEditModel.UpdateMany update = employeeEditModel.createUpdate(entities);
      createWorker(update::execute, UPDATING)
              .onResult(update::onResult)
              .execute();
    }
  }

  private ProgressWorkerDialogBuilder<Collection<Entity>, ?> createWorker(Task<Collection<Entity>> task, String dialogTitle) {
    return progressWorkerDialog(task)
            .title(dialogTitle)
            .owner(EmployeeTablePanel.this)
            .onException(this::onException);
  }
}
