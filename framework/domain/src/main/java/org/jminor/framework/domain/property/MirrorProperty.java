/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

/**
 * Represents a property which is part of a composite foreign key but is already included as part of another composite foreign key,
 * and should not handle updating the underlying property, useful in rare cases where multiple foreign keys are referencing tables
 * having composite natural primary keys, using the same column.
 * todo example pleeeeaaase!
 */
public interface MirrorProperty extends ColumnProperty {}
