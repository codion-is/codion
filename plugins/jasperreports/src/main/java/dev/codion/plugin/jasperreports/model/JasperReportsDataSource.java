/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jasperreports.model;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.Iterator;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

/**
 * A default JRDataSource implementation which iterates through the iterator received via the constructor.
 * @param <T> the type to fetch the data from
 */
public final class JasperReportsDataSource<T> implements JRDataSource {

  private final Iterator<T> reportIterator;
  private final BiFunction<T, JRField, Object> valueProvider;
  private T currentItem = null;

  /**
   * Instantiates a new JasperReportsDataSource.
   * @param reportIterator the iterator providing the report data
   * @param valueProvider a Function returning the value for a given field from the given item
   */
  public JasperReportsDataSource(final Iterator<T> reportIterator, final BiFunction<T, JRField, Object> valueProvider) {
    this.reportIterator = requireNonNull(reportIterator, "reportIterator");
    this.valueProvider = requireNonNull(valueProvider, "valueProvider");
  }

  @Override
  public boolean next() {
    final boolean hasNext = reportIterator.hasNext();
    if (hasNext) {
      currentItem = reportIterator.next();
    }

    return hasNext;
  }

  /**
   * Returns the table value of the property identified by {@code field.getName()}
   * @param field the report field which value to retrieve
   * @return the value of the property identified by {@code field.getName()}
   * @throws net.sf.jasperreports.engine.JRException in case of an exception
   */
  @Override
  public Object getFieldValue(final JRField field) throws JRException {
    requireNonNull(field, "field");
    try {
      return valueProvider.apply(currentItem, field);
    }
    catch (final Exception e) {
      throw new JRException("Unable to get field value: " + field.getName(), e);
    }
  }
}
