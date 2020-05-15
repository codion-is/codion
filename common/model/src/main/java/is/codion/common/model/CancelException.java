/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.common.model;

/**
 * Used when actions must be cancelled from deep within a call stack.
 */
public class CancelException extends RuntimeException {}
