/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.client.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Column;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ColumnConstraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  @Override
  protected void setupEntityPanelBuilders(final SchemaBrowserApplicationModel applicationModel) {
    final SwingEntityModel schemaModel = applicationModel.getEntityModel(Schema.TYPE);
    final SwingEntityModel tableModel = schemaModel.getDetailModel(Table.TYPE);
    final SwingEntityModel columnModel = tableModel.getDetailModel(Column.TYPE);
    final SwingEntityModel constraintModel = tableModel.getDetailModel(Constraint.TYPE);
    final SwingEntityModel columnConstraintModel = constraintModel.getDetailModel(ColumnConstraint.TYPE);

    addEntityPanelBuilder(EntityPanel.builder(schemaModel)
            .detailPanelBuilder(EntityPanel.builder(tableModel)
                    .detailPanelBuilder(EntityPanel.builder(columnModel))
                    .detailPanelBuilder(EntityPanel.builder(constraintModel)
                            .detailPanelBuilder(EntityPanel.builder(columnConstraintModel))))
            .detailSplitPanelResizeWeight(0.3));
  }

  @Override
  protected SchemaBrowserApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new SchemaBrowserApplicationModel(connectionProvider);
  }

  @Override
  protected String getApplicationIdentifier() {
    return "is.codion.demo.SchemaBrowser";
  }

  public static void main(final String[] args) {
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.TABLE_CONDITION_PANEL_VISIBLE.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.schemabrowser.domain.SchemaBrowser");
    new SchemaBrowserAppPanel().startApplication("Schema Browser", null, MaximizeFrame.NO,
            Windows.getScreenSizeRatio(0.5), User.parseUser("scott:tiger"));
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      final SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
      final SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
      final SwingEntityModel columnModel = new SwingEntityModel(Column.TYPE, connectionProvider);
      final SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
      final SwingEntityModel columnConstraintModel = new SwingEntityModel(ColumnConstraint.TYPE, connectionProvider);

      schemaModel.addDetailModel(tableModel);
      tableModel.addDetailModels(columnModel, constraintModel);
      constraintModel.addDetailModels(columnConstraintModel);

      addEntityModel(schemaModel);
    }
  }
}