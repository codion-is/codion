/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.model.SearchModel;
import org.jminor.common.ui.AbstractSearchPanel;
import org.jminor.framework.domain.Property;

public final class PropertyFilterPanel extends AbstractSearchPanel<Property> {

  public PropertyFilterPanel(final SearchModel<Property> model) {
    this(model, false, false);
  }

  public PropertyFilterPanel(final SearchModel<Property> model, final boolean includeActivateBtn,
                             final boolean includeToggleAdvBtn) {
    super(model, includeActivateBtn, includeToggleAdvBtn);
  }

  @Override
  protected boolean isLowerBoundFieldRequired(final Property searchKey) {
    return !searchKey.isBoolean();
  }
}