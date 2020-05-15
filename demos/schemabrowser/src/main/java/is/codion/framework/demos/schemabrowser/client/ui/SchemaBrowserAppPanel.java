/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.schemabrowser.client.ui;

import is.codion.common.model.CancelException;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  @Override
  protected void setupEntityPanelBuilders() {
    final EntityPanelBuilder columnConstraintProvider = new EntityPanelBuilder(SchemaBrowser.T_COLUMN_CONSTRAINT);
    final EntityPanelBuilder constraintProvider = new EntityPanelBuilder(SchemaBrowser.T_CONSTRAINT);
    constraintProvider.addDetailPanelBuilder(columnConstraintProvider);
    final EntityPanelBuilder columnProvider = new EntityPanelBuilder(SchemaBrowser.T_COLUMN);
    final EntityPanelBuilder dbObjectProvider = new EntityPanelBuilder(SchemaBrowser.T_TABLE);
    dbObjectProvider.addDetailPanelBuilder(columnProvider);
    dbObjectProvider.addDetailPanelBuilder(constraintProvider);
    final EntityPanelBuilder schemaProvider = new EntityPanelBuilder(SchemaBrowser.T_SCHEMA);
    schemaProvider.addDetailPanelBuilder(dbObjectProvider).setDetailSplitPanelResizeWeight(0.3);
    addEntityPanelBuilder(schemaProvider);
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
            Windows.getScreenSizeRatio(0.5), Users.parseUser("scott:tiger"));
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }
}