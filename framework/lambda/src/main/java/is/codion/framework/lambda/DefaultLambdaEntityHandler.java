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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.lambda;

import is.codion.framework.domain.Domain;

import java.util.ServiceLoader;

/**
 * Default Lambda entity handler that loads the domain via ServiceLoader.
 * This handler can be used directly when deploying a single domain application.
 * <p>
 * Requires the {@code DOMAIN_TYPE} environment variable to be set to the
 * domain type identifier.
 */
public final class DefaultLambdaEntityHandler extends AbstractLambdaEntityHandler {

	/**
	 * Creates a new default handler that loads the domain via ServiceLoader.
	 * @throws IllegalStateException if DOMAIN_TYPE is not set or domain not found
	 */
	public DefaultLambdaEntityHandler() {
		super(loadDomain());
	}

	private static Domain loadDomain() {
		String domainType = System.getenv("DOMAIN_TYPE");
		if (domainType == null || domainType.isEmpty()) {
			throw new IllegalStateException("DOMAIN_TYPE environment variable must be set");
		}

		// Load domains via ServiceLoader
		ServiceLoader<Domain> loader = ServiceLoader.load(Domain.class);
		for (Domain domain : loader) {
			if (domain.type().name().equals(domainType)) {
				return domain;
			}
		}

		throw new IllegalStateException("Domain not found: " + domainType);
	}
}