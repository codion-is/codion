/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.model.PropertySummaryModel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.client.ui.EntityTableSearchPanel;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entities;

import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.Dimension;

public class InvoiceLinePanel extends EntityPanel {

  public InvoiceLinePanel(final EntityModel model) {
    super(model, new InvoiceLineEditPanel(model.getEditModel()), new EntityTablePanel(model.getTableModel(), (EntityTableSearchPanel) null) {
      @Override
      protected JPanel initializeSouthPanel() {
        return null;
      }
    });
    model.getTableModel().setDetailModel(true);
    model.getTableModel().setQueryConfigurationAllowed(false);
    model.getTableModel().getPropertySummaryModel(Chinook.INVOICELINE_UNITPRICE)
            .setSummaryType(PropertySummaryModel.SummaryType.MINIMUM_MAXIMUM);
    model.getTableModel().getPropertySummaryModel(Chinook.INVOICELINE_TOTAL)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);
    model.getTableModel().getPropertySummaryModel(Chinook.INVOICELINE_QUANTITY)
            .setSummaryType(PropertySummaryModel.SummaryType.SUM);

    //todo setColumnVis... must be called after table panel is initialized
    model.getTableModel().setColumnVisible(Entities.getProperty(Chinook.T_INVOICELINE, Chinook.INVOICELINE_INVOICEID_FK), false);
    getTablePanel().setSummaryPanelVisible(true);
    getTablePanel().getJTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

    setEditPanelState(EntityPanel.HIDDEN);
    initializePanel();
    setPreferredSize(new Dimension(360, 50));
  }
}
