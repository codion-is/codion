/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityTableModel;
import org.jminor.framework.client.ui.EntityTablePanel;
import org.jminor.framework.client.ui.EntityTableSearchPanel;

import javax.swing.JPanel;
import javax.swing.JTable;

public class InvoiceLineTablePanel extends EntityTablePanel {

  public InvoiceLineTablePanel(final EntityTableModel tableModel) {
    super(tableModel, (EntityTableSearchPanel) null);
  }

  @Override
  protected int getAutoResizeMode() {
    return JTable.AUTO_RESIZE_ALL_COLUMNS;
  }

  @Override
  protected JPanel initializeSouthPanel() {
    return null;
  }
}
