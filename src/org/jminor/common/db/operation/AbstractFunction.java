/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A base Function implementation
 */
public abstract class AbstractFunction extends AbstractOperation implements Procedure {

  /**
   * Instantiates a new AbstractFunction.
   * @param id the function ID
   * @param name
   */
  public AbstractFunction(final String id, final String name) {
    super(id, name);
  }
}