/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.db;

/**
 * A base Operation implementation
 */
public class AbstractOperation implements DatabaseConnection.Operation {

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
  public final String getID() {
    return id;
  }

  /** {@inheritDoc} */
  public final String getName() {
    return this.name;
  }
}
