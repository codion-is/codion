/*
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jasperreports;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A default JRDataSource implementation which iterates through the iterator received via the constructor.
 * @param <T> the type to fetch the data from
 */
public class JasperReportsDataSource<T> implements JRDataSource {

  private final Iterator<T> reportIterator;
  private final BiFunction<T, JRField, Object> valueProvider;
  private final Consumer<T> onNext;

  private T currentItem = null;

  /**
   * Instantiates a new JasperReportsDataSource.
   * @param reportIterator the iterator providing the report data
   * @param valueProvider a Function returning the value for a given field from the given item
   */
  public JasperReportsDataSource(Iterator<T> reportIterator, BiFunction<T, JRField, Object> valueProvider) {
    this(reportIterator, valueProvider, null);
  }

  /**
   * Instantiates a new JasperReportsDataSource.
   * @param reportIterator the iterator providing the report data
   * @param valueProvider a Function returning the value for a given field from the given item
   * @param onNext called each time next has been called
   */
  public JasperReportsDataSource(Iterator<T> reportIterator, BiFunction<T, JRField, Object> valueProvider,
                                 Consumer<T> onNext) {
    this.reportIterator = requireNonNull(reportIterator, "reportIterator");
    this.valueProvider = requireNonNull(valueProvider, "valueProvider");
    this.onNext = onNext == null ? next -> {} : onNext;
  }

  @Override
  public final boolean next() {
    boolean hasNext = reportIterator.hasNext();
    if (hasNext) {
      currentItem = reportIterator.next();
      onNext.accept(currentItem);
    }

    return hasNext;
  }

  /**
   * Returns value of the attribute with the name {@code field.getName()}
   * @param field the report field which value to retrieve
   * @return the value of the attribute with the name {@code field.getName()}
   * @throws net.sf.jasperreports.engine.JRException in case of an exception
   */
  @Override
  public final Object getFieldValue(JRField field) throws JRException {
    requireNonNull(field, "field");
    try {
      return valueProvider.apply(currentItem, field);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new JRException("Unable to get field value: " + field.getName(), e);
    }
  }

  /**
   * @return the current item
   * @throws IllegalStateException in case next has not been called and no current item is available
   */
  public final T currentItem() {
    if (currentItem == null) {
      throw new IllegalStateException("Next has not been called, no current item available");
    }

    return currentItem;
  }
}
