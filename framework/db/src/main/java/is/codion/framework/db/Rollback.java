/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db;

/**
 * A rollback on behalf of a transactional which threw, the exception it threw remaining the one reported.
 */
final class Rollback {

	private Rollback() {}

	/**
	 * Performs the given rollback, adding any exception it throws to the given one as suppressed.
	 * @param exception the exception the transactional threw
	 * @param rollback the rollback
	 * @return the exception to throw, the given one if a {@link RuntimeException}, otherwise one wrapping it
	 * @throws Error in case the given exception is an {@link Error}
	 */
	static RuntimeException after(Throwable exception, Runnable rollback) {
		try {
			rollback.run();
		}
		catch (Exception rollbackException) {
			exception.addSuppressed(rollbackException);
		}
		if (exception instanceof Error) {
			throw (Error) exception;
		}
		if (exception instanceof RuntimeException) {
			return (RuntimeException) exception;
		}

		return new RuntimeException(exception);
	}
}
