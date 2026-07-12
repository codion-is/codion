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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.framework.domain.DomainType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

/**
 * An abstract {@link EntityObjectMapperFactory} implementation, extend to add custom serializers/deserializers.
 * <p>
 * Subclasses should be exposed as a service.
 */
public abstract class AbstractEntityObjectMapperFactory implements EntityObjectMapperFactory {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityObjectMapperFactory.class);

	private final DomainType domainType;

	/**
	 * Instantiates a new instance compatible with the given domain type.
	 * @param domainType the domain type
	 */
	protected AbstractEntityObjectMapperFactory(DomainType domainType) {
		this.domainType = requireNonNull(domainType);
	}

	@Override
	public final boolean compatibleWith(DomainType domainType) {
		return this.domainType.equals(requireNonNull(domainType));
	}

	static EntityObjectMapperFactory resolve(DomainType domainType) {
		try {
			Optional<EntityObjectMapperFactory> mapperFactory = stream(ServiceLoader.load(EntityObjectMapperFactory.class).spliterator(), false)
							.filter(factory -> factory.compatibleWith(domainType))
							.findFirst();
			if (mapperFactory.isPresent()) {
				return mapperFactory.get();
			}
			LOG.warn("No EntityObjectMapperFactory registered for domain: {}, using the default one", domainType);

			//compatible with all domain models
			return mapperDomainType -> true;
		}
		catch (ServiceConfigurationError e) {
			throw Exceptions.runtime(e, ServiceConfigurationError.class);
		}
	}
}
