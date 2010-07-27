/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchModel;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.framework.Configuration;
import org.jminor.framework.domain.Property;

import java.sql.Types;
import java.text.SimpleDateFormat;

public final class PropertyFilterPanel extends AbstractSearchPanel<Property> {

  public PropertyFilterPanel(final SearchModel<Property> model) {
    this(model, false, false);
  }

  public PropertyFilterPanel(final SearchModel<Property> model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
  }

  @Override
  protected final boolean isLowerBoundFieldRequired(final Property property) {
    return !property.isBoolean();
  }

  @Override
  protected final SimpleDateFormat getDateFormat() {
    if (getModel().getType() == Types.TIMESTAMP) {
      return Configuration.getDefaultTimestampFormat();
    }
    if (getModel().getType() == Types.DATE) {
      return Configuration.getDefaultDateFormat();
    }

    return null;
  }
}