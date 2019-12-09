/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.condition.Condition;
import org.jminor.framework.domain.property.Property;

/**
 * A base interface for a column condition based on a property.
 * @param <T> the type of {@link Property} this condition model is based on
 */
public interface PropertyConditionModel<T extends Property> extends ColumnConditionModel<T>, Condition.Provider {

}
