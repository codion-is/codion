/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import java.io.Serializable;

/**
 * @param <C> the connection type
 * @param <T> the function argument type
 * @param <R> the function result type
 */
public interface FunctionType<C, T, R> extends OperationType, Serializable {}
