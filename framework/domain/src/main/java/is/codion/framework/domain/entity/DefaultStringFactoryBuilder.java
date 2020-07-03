/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.text.Format;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

final class DefaultStringFactoryBuilder implements StringFactory.Builder {

  private final StringFactory stringFactory;

  DefaultStringFactoryBuilder(final StringFactory stringFactory) {
    this.stringFactory = requireNonNull(stringFactory);
  }

  @Override
  public StringFactory.Builder value(final Attribute<?> attribute) {
    stringFactory.addValue(attribute);
    return this;
  }

  @Override
  public StringFactory.Builder formattedValue(final Attribute<?> attribute, final Format format) {
    stringFactory.addFormattedValue(attribute, format);
    return this;
  }

  @Override
  public StringFactory.Builder foreignKeyValue(final Attribute<Entity> foreignKeyAttribute,
                                               final Attribute<?> attribute) {
    stringFactory.addForeignKeyValue(foreignKeyAttribute, attribute);
    return this;
  }

  @Override
  public StringFactory.Builder text(final String text) {
    stringFactory.addText(text);
    return this;
  }

  @Override
  public Function<Entity, String> get() {
    return stringFactory;
  }
}
