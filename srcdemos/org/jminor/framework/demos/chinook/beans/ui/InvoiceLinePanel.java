/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class InvoiceLinePanel extends EntityPanel {

  public InvoiceLinePanel(final EntityModel model) {
    super(model, "Invoice line");
  }

  protected EntityEditPanel initializeEditPanel(EntityEditModel editModel) {
    return null;
  }
}