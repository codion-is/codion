/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

/**
 * Used when actions must be cancelled from deep within a call stack.
 */
public class CancelException extends RuntimeException {}
