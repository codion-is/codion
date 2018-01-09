/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.jasperreports.model;

import org.jminor.common.db.reports.ReportDataWrapper;
import org.jminor.framework.domain.Entity;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.Iterator;
import java.util.Objects;

/**
 * A default JRDataSource implementation which iterates through the iterator received via the constructor.
 */
public class JasperReportsEntityDataSource implements JRDataSource, ReportDataWrapper<JRDataSource> {

  private final Iterator<Entity> reportIterator;
  private Entity currentEntity = null;

  /**
   * @param reportIterator the iterator providing the report data
   */
  public JasperReportsEntityDataSource(final Iterator<Entity> reportIterator) {
    Objects.requireNonNull(reportIterator, "reportIterator");
    this.reportIterator = reportIterator;
  }

  /** {@inheritDoc} */
  @Override
  public final boolean next() {
    final boolean hasNext = reportIterator.hasNext();
    if (hasNext) {
      currentEntity = reportIterator.next();
    }

    return hasNext;
  }

  /** {@inheritDoc} */
  @Override
  public final JRDataSource getDataSource() {
    return this;
  }

  /**
   * Returns the table value of the property identified by {@code jrField.getName()}
   * @param jrField the report field which value to retrieve
   * @return the value of the property identified by {@code jrField.getName()}
   * @throws net.sf.jasperreports.engine.JRException in case of an exception
   * @see org.jminor.framework.domain.Entity#get(org.jminor.common.db.Attribute)
   */
  @Override
  public Object getFieldValue(final JRField jrField) throws JRException {
    Objects.requireNonNull(jrField, "jrField");
    try {
      return currentEntity.get(jrField.getName());
    }
    catch (final Exception e) {
      throw new JRException("Unable to get field value: " + jrField.getName(), e);
    }
  }

  protected final Entity getCurrentEntity() {
    return currentEntity;
  }
}
