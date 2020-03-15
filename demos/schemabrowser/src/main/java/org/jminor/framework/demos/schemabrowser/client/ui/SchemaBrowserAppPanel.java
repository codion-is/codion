/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

  @Override
  protected void setupEntityPanelProviders() {
    final EntityPanelProvider columnConstraintProvider = new EntityPanelProvider(SchemaBrowser.T_COLUMN_CONSTRAINT);
    final EntityPanelProvider constraintProvider = new EntityPanelProvider(SchemaBrowser.T_CONSTRAINT);
    constraintProvider.addDetailPanelProvider(columnConstraintProvider);
    final EntityPanelProvider columnProvider = new EntityPanelProvider(SchemaBrowser.T_COLUMN);
    final EntityPanelProvider dbObjectProvider = new EntityPanelProvider(SchemaBrowser.T_TABLE);
    dbObjectProvider.addDetailPanelProvider(columnProvider);
    dbObjectProvider.addDetailPanelProvider(constraintProvider);
    final EntityPanelProvider schemaProvider = new EntityPanelProvider(SchemaBrowser.T_SCHEMA);
    schemaProvider.addDetailPanelProvider(dbObjectProvider).setDetailSplitPanelResizeWeight(0.3);
    addEntityPanelProvider(schemaProvider);
  }

  @Override
  protected SchemaBrowserApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new SchemaBrowserApplicationModel(connectionProvider);
  }

  @Override
  protected String getApplicationIdentifier() {
    return "org.jminor.demo.SchemaBrowser";
  }

  public static void main(final String[] args) {
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.TABLE_CONDITION_PANEL_VISIBLE.set(true);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser");
    new SchemaBrowserAppPanel().startApplication("Schema Browser", null, false,
            Windows.getScreenSizeRatio(0.5), Users.parseUser("scott:tiger"));
  }

  public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
    public SchemaBrowserApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }
}