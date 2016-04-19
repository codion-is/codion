/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.Refreshable;

/**
 * Defines the methods a ComboBoxModel based on a property value that are required
 */
public interface PropertyComboBoxModel extends Refreshable {

  boolean isCleared();
}
