/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.OrderBy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultSelectCondition extends AbstractCondition implements SelectCondition {

  private static final long serialVersionUID = 1;

  private final Condition condition;
  private Map<ForeignKey, Integer> foreignKeyFetchDepths;
  private Collection<Attribute<?>> selectAttributes = emptyList();

  private OrderBy orderBy;
  private Integer fetchDepth;
  private boolean forUpdate;
  private int limit = -1;
  private int offset = -1;

  /**
   * Instantiates a new {@link DefaultSelectCondition}
   * @param condition the Condition object
   */
  DefaultSelectCondition(final Condition condition) {
    super(requireNonNull(condition, "condition").getEntityType());
    this.condition = condition;
  }

  private DefaultSelectCondition(final DefaultSelectCondition selectCondition) {
    super(selectCondition.getEntityType());
    this.condition = selectCondition.condition;
    this.foreignKeyFetchDepths = selectCondition.foreignKeyFetchDepths;
    this.selectAttributes = selectCondition.selectAttributes;
    this.orderBy = selectCondition.orderBy;
    this.fetchDepth = selectCondition.fetchDepth;
    this.forUpdate = selectCondition.forUpdate;
    this.limit = selectCondition.limit;
    this.offset = selectCondition.offset;
  }

  @Override
  public List<?> getValues() {
    return condition.getValues();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return condition.getAttributes();
  }

  @Override
  public String getConditionString(final EntityDefinition definition) {
    return condition.getConditionString(definition);
  }

  @Override
  public Condition getCondition() {
    return condition;
  }

  @Override
  public OrderBy getOrderBy() {
    return orderBy;
  }

  @Override
  public int getLimit() {
    return limit;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public boolean isForUpdate() {
    return forUpdate;
  }

  @Override
  public Integer getFetchDepth() {
    return fetchDepth;
  }

  @Override
  public Integer getFetchDepth(final ForeignKey foreignKey) {
    if (foreignKeyFetchDepths != null && foreignKeyFetchDepths.containsKey(foreignKey)) {
      return foreignKeyFetchDepths.get(foreignKey);
    }

    return fetchDepth;
  }

  @Override
  public Collection<Attribute<?>> getSelectAttributes() {
    return selectAttributes == null ? emptyList() : selectAttributes;
  }

  @Override
  public SelectCondition orderBy(final OrderBy orderBy) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.orderBy = orderBy;

    return selectCondition;
  }

  @Override
  public SelectCondition limit(final int limit) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.limit = limit;

    return selectCondition;
  }

  @Override
  public SelectCondition offset(final int offset) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.offset = offset;

    return selectCondition;
  }

  @Override
  public SelectCondition forUpdate() {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.forUpdate = true;

    return selectCondition;
  }

  @Override
  public SelectCondition fetchDepth(final int fetchDepth) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.fetchDepth = fetchDepth;

    return selectCondition;
  }

  @Override
  public SelectCondition fetchDepth(final ForeignKey foreignKey, final int fetchDepth) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    if (selectCondition.foreignKeyFetchDepths == null) {
      selectCondition.foreignKeyFetchDepths = new HashMap<>();
    }
    selectCondition.foreignKeyFetchDepths.put(foreignKey, fetchDepth);

    return selectCondition;
  }

  @Override
  public SelectCondition selectAttributes(final Attribute<?>... attributes) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.selectAttributes = requireNonNull(attributes).length == 0 ? emptyList() : unmodifiableList(asList(attributes));

    return selectCondition;
  }

  @Override
  public SelectCondition selectAttributes(final Collection<Attribute<?>> attributes) {
    DefaultSelectCondition selectCondition = new DefaultSelectCondition(this);
    selectCondition.selectAttributes = requireNonNull(attributes).isEmpty() ? emptyList() : unmodifiableList(new ArrayList<>(attributes));

    return selectCondition;
  }
}
