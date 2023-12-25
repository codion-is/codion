/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.framework.demos.chinook.model.EmployeeEditModel;
import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.swing.common.model.worker.ProgressWorker.Task;
import is.codion.swing.common.ui.control.Control.Command;
import is.codion.swing.common.ui.dialog.ProgressWorkerDialogBuilder;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityDialogs.EditDialogBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.util.Collection;

import static is.codion.framework.demos.chinook.ui.EmployeeEditPanel.DELETING;
import static is.codion.framework.demos.chinook.ui.EmployeeEditPanel.UPDATING;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.component.Components.splitPane;
import static is.codion.swing.common.ui.dialog.Dialogs.progressWorkerDialog;
import static is.codion.swing.framework.ui.EntityTree.entityTree;

public final class EmployeeTablePanel extends EntityTablePanel {

  public EmployeeTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
  }

  @Override
  protected void setupControls() {
    control(TableControl.DELETE_SELECTED).map(delete -> delete.copy(new DeleteCommand()).build());
  }

  @Override
  protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
    EmployeeTableModel tableModel = tableModel();
    super.layoutPanel(splitPane()
            .orientation(JSplitPane.HORIZONTAL_SPLIT)
            .continuousLayout(true)
            .oneTouchExpandable(true)
            .resizeWeight(0.65)
            .leftComponent(tableComponent)
            .rightComponent(scrollPane(entityTree(tableModel.treeModel())).build())
            .build(), southPanel);
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
