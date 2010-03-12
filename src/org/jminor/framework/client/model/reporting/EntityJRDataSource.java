/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.reporting;

import org.jminor.framework.domain.Entity;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.Iterator;

/**
 * A default JRDataSource implementation which iterates through the iterator received via the constructor
 */
public class EntityJRDataSource implements JRDataSource {

  private final Iterator<Entity> reportIterator;
  private Entity currentEntity;

  public EntityJRDataSource(final Iterator<Entity> reportIterator) {
    if (reportIterator == null)
      throw new IllegalArgumentException("EntityJRDataSource requires a non-null reportIterator");
    this.reportIterator = reportIterator;
  }

  public Entity getCurrentEntity() {
    return currentEntity;
  }

  public boolean next() throws JRException {
    final boolean hasNext = reportIterator.hasNext();
    if (hasNext)
      currentEntity = reportIterator.next();

    return hasNext;
  }

  /**
   * Returns the table value of the property identified by <code>jrField.getName()</code>
   * @param jrField the report field which value to retrieve
   * @return the value of the property identified by <code>jrField.getName()</code>
   * @throws net.sf.jasperreports.engine.JRException in case of an exception
   * @see org.jminor.framework.domain.Entity#getTableValue(String)
   */
  public Object getFieldValue(final JRField jrField) throws JRException {
    return getCurrentEntity().getTableValue(jrField.getName());
  }
}
