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
  protected List<EntityPanel> initializeEntityPanels(SchemaBrowserApplicationModel applicationModel) {
    SwingEntityModel schemaModel = applicationModel.getEntityModel(Schema.TYPE);
    SwingEntityModel tableModel = schemaModel.getDetailModel(Table.TYPE);
    SwingEntityModel columnModel = tableModel.getDetailModel(Column.TYPE);
    SwingEntityModel constraintModel = tableModel.getDetailModel(Constraint.TYPE);
    SwingEntityModel columnConstraintModel = constraintModel.getDetailModel(ConstraintColumn.TYPE);

    EntityPanel schemaPanel = new EntityPanel(schemaModel);
    EntityPanel tablePanel = new EntityPanel(tableModel);
    EntityPanel columnPanel = new EntityPanel(columnModel);
    EntityPanel constraintPanel = new EntityPanel(constraintModel);
    EntityPanel columnConstraintPanel = new EntityPanel(columnConstraintModel);

    schemaPanel.addDetailPanel(tablePanel);
    tablePanel.addDetailPanels(columnPanel);
    tablePanel.addDetailPanel(constraintPanel);
    constraintPanel.addDetailPanel(columnConstraintPanel);

    schemaPanel.setDetailSplitPanelResizeWeight(0.3);

    schemaModel.getTableModel().refresh();

    return Collections.singletonList(schemaPanel);
  }

  @Override
  protected SchemaBrowserApplicationModel initializeApplicationModel(EntityConnectionProvider connectionProvider) throws CancelException {
    return new SchemaBrowserApplicationModel(connectionProvider);
  }

  @Override
  protected String getApplicationIdentifier() {
    return "is.codion.demo.SchemaBrowser";
  }

  public static void main(String[] args) {
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.TABLE_CONDITION_PANEL_VISIBLE.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.schemabrowser.domain.SchemaBrowser");
    SwingUtilities.invokeLater(() -> new SchemaBrowserAppPanel().starter()
            .frameSize(Windows.getScreenSizeRatio(0.5))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
      SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
      SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
      SwingEntityModel columnModel = new SwingEntityModel(Column.TYPE, connectionProvider);
      SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
      SwingEntityModel constraintColumnModel = new SwingEntityModel(ConstraintColumn.TYPE, connectionProvider);

      schemaModel.addDetailModel(tableModel);
      tableModel.addDetailModels(columnModel, constraintModel);
      constraintModel.addDetailModels(constraintColumnModel);

      addEntityModel(schemaModel);
    }
  }
}