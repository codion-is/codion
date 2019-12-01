/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db.operation;

/**
 * A base Operation implementation
 */
public abstract class AbstractOperation implements Operation {

  private final String id;
  private final String name;

  /**
   * Instantiates a new AbstractOperation
   * @param id a unique operation ID
   * @param name the operation name
   */
  public AbstractOperation(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  /** {@inheritDoc} */
  @Override
  public final String getId() {
    return id;
  }

  /** {@inheritDoc} */
  @Override
  public final String getName() {
    return this.name;
  }
}
