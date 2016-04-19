/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.schemabrowser.client.ui;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.demos.schemabrowser.domain.SchemaBrowser;
import org.jminor.framework.model.DefaultEntityApplicationModel;
import org.jminor.framework.model.EntityApplicationModel;
import org.jminor.swing.SwingConfiguration;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import javax.swing.JTable;

public class SchemaBrowserAppPanel extends EntityApplicationPanel {

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
  protected EntityApplicationModel initializeApplicationModel(final EntityConnectionProvider connectionProvider) throws CancelException {
    return new SchemaBrowserApplicationModel(connectionProvider);
  }

  @Override
  protected String getApplicationIdentifier() {
    return "org.jminor.demo.SchemaBrowser";
  }

  public static void main(final String[] args) {
    SwingConfiguration.setValue(SwingConfiguration.TABLE_AUTO_RESIZE_MODE, JTable.AUTO_RESIZE_ALL_COLUMNS);
    Configuration.setValue(Configuration.TABLE_CRITERIA_PANEL_VISIBLE, true);
    new SchemaBrowserAppPanel().startApplication("Schema Browser", null, false, UiUtil.getScreenSizeRatio(0.5), new User("scott", "tiger"));
  }

  private static final class SchemaBrowserApplicationModel extends DefaultEntityApplicationModel {
    private SchemaBrowserApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

    @Override
    protected void loadDomainModel() {
      SchemaBrowser.init();
    }
  }
}