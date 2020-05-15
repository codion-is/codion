/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.db.operation;

/**
 * A base Operation implementation
 */
public abstract class AbstractDatabaseOperation implements DatabaseOperation {

  private final String id;
  private final String name;

  /**
   * Instantiates a new AbstractOperation
   * @param id a unique operation id
   * @param name the operation name
   */
  public AbstractDatabaseOperation(final String id, final String name) {
    this.id = id;
    this.name = name;
  }

  @Override
  public final String getId() {
    return id;
  }

  @Override
  public final String getName() {
    return this.name;
  }
}
