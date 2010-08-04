/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.ColumnSearchModel;
import org.jminor.common.model.SearchType;
import org.jminor.common.ui.ColumnSearchPanel;
import org.jminor.framework.domain.Property;

public final class PropertyFilterPanel extends ColumnSearchPanel<Property> {

  public PropertyFilterPanel(final ColumnSearchModel<Property> model) {
    this(model, false, false);
  }

  public PropertyFilterPanel(final ColumnSearchModel<Property> model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn, getSearchTypes(model));
  }

  private static SearchType[] getSearchTypes(final ColumnSearchModel<Property> model) {
    if (model.getColumnIdentifier().isBoolean()) {
      return new SearchType[] {SearchType.LIKE};
    }
    else {
      return SearchType.values();
    }
  }
}