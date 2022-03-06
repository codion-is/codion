/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

final class DefaultSubqueryProperty<T> extends DefaultColumnProperty<T> implements SubqueryProperty<T> {

  private static final long serialVersionUID = 1;

  private final transient String subquery;

  private DefaultSubqueryProperty(DefaultSubqueryPropertyBuilder<T, ?> builder) {
    super(builder);
    this.subquery = builder.subquery;
  }

  @Override
  public String getSubquery() {
    return subquery;
  }

  static final class DefaultSubqueryPropertyBuilder<T, B extends ColumnProperty.Builder<T, B>>
          extends AbstractReadOnlyColumnPropertyBuilder<T, B> implements Property.Builder<T, B> {

    private final String subquery;

    DefaultSubqueryPropertyBuilder(Attribute<T> attribute, String caption, String subquery) {
      super(attribute, caption);
      this.subquery = subquery;
    }

    @Override
    public Property<T> build() {
      return new DefaultSubqueryProperty<>(this);
    }
  }
}
