/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.awt.event.ActionListener;

/**
 * User: darri
 * Date: 29.7.2010
 * Time: 11:54:38
 */
public interface EventObserver {

  void addListener(final ActionListener listener);
}
