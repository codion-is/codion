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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common;

import static is.codion.common.resource.MessageBundle.messageBundle;
import static java.util.ResourceBundle.getBundle;

/**
 * Enumerating all the available operator types.
 */
public enum Operator {

	/**
	 * {@code α = x}
	 */
	EQUAL,
	/**
	 * {@code α ≠ x}
	 */
	NOT_EQUAL,
	/**
	 * {@code α < x}
	 */
	LESS_THAN,
	/**
	 * {@code α ≤ x}
	 */
	LESS_THAN_OR_EQUAL,
	/**
	 * {@code α > x}
	 */
	GREATER_THAN,
	/**
	 * {@code α ≥ x}
	 */
	GREATER_THAN_OR_EQUAL,
	/**
	 * {@code α ∈ {x,y,z...}}
	 */
	IN,
	/**
	 * {@code α ∉ {x,y,z...}}
	 */
	NOT_IN,
	/**
	 * {@code x < α < y}
	 */
	BETWEEN_EXCLUSIVE,
	/**
	 * {@code x ≤ α ≤ y}
	 */
	BETWEEN,
	/**
	 * {@code x ≥ α ≥ y}
	 */
	NOT_BETWEEN_EXCLUSIVE,
	/**
	 * {@code x > α > y}
	 */
	NOT_BETWEEN;

	private final String description;

	Operator() {
		this.description = messageBundle(Operator.class, getBundle(Operator.class.getName())).getString(name().toLowerCase());
	}

	/**
	 * @return the operator description
	 */
	public String description() {
		return description;
	}
}
