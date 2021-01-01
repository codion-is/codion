/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model;

/**
 * Used when actions must be cancelled from deep within a call stack.
 */
public class CancelException extends RuntimeException {}
