/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.result;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static java.util.Objects.requireNonNull;

final class DefaultIterator<T> implements Iterator<T> {

  private final ResultIterator<T> resultIterator;

  DefaultIterator(ResultIterator<T> resultIterator) {
    this.resultIterator = requireNonNull(resultIterator);
  }

  @Override
  public boolean hasNext() {
    try {
      return resultIterator.hasNext();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public T next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    try {
      return resultIterator.next();
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
}
