/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.db.operation;

import java.io.Serializable;

/**
 * @param <C> the connection type
 * @param <T> the procedure argument type
 */
public interface ProcedureType<C, T> extends OperationType, Serializable {}
