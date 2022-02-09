/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.client.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Column;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.util.Collections;
import java.util.List;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  public SchemaBrowserAppPanel() {
    super("Schema Browser");
  }

  @Override
  protected List<EntityPanel> initializeEntityPanels(final SchemaBrowserApplicationModel applicationModel) {
    final SwingEntityModel schemaModel = applicationModel.getEntityModel(Schema.TYPE);
    final SwingEntityModel tableModel = schemaModel.getDetailModel(Table.TYPE);
    final SwingEntityModel columnModel = tableModel.getDetailModel(Column.TYPE);
    final SwingEntityModel constraintModel = tableModel.getDetailModel(Constraint.TYPE);
    final SwingEntityModel columnConstraintModel = constraintModel.getDetailModel(ConstraintColumn.TYPE);

    final EntityPanel schemaPanel = new EntityPanel(schemaModel);
    final EntityPanel tablePanel = new EntityPanel(tableModel);
    final EntityPanel columnPanel = new EntityPanel(columnModel);
    final EntityPanel constraintPanel = new EntityPanel(constraintModel);
    final EntityPanel columnConstraintPanel = new EntityPanel(columnConstraintModel);

    schemaPanel.addDetailPanel(tablePanel);
    tablePanel.addDetailPanels(columnPanel);
    tablePanel.addDetailPanel(constraintPanel);
    constraintPanel.addDetailPanel(columnConstraintPanel);

    schemaPanel.setDetailSplitPanelResizeWeight(0.3);

    schemaModel.getTableModel().refresh();

    return Collections.singletonList(schemaPanel);
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
    SwingUtilities.invokeLater(() -> new SchemaBrowserAppPanel().starter()
            .frameSize(Windows.getScreenSizeRatio(0.5))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      final SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
      final SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
      final SwingEntityModel columnModel = new SwingEntityModel(Column.TYPE, connectionProvider);
      final SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
      final SwingEntityModel constraintColumnModel = new SwingEntityModel(ConstraintColumn.TYPE, connectionProvider);

      schemaModel.addDetailModel(tableModel);
      tableModel.addDetailModels(columnModel, constraintModel);
      constraintModel.addDetailModels(constraintColumnModel);

      addEntityModel(schemaModel);
    }
  }
}