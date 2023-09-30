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
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.DomainType;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityObjectMapperFactory} implementation, extend to add custom serialisers/deserializers.<br><br>
 * Subclasses should be exposed as a service.
 */
public class DefaultEntityObjectMapperFactory implements EntityObjectMapperFactory {

  private final DomainType domainType;

  /**
   * Instantiates a new instance compatible with the given domain type.
   * @param domainType the domain type
   */
  protected DefaultEntityObjectMapperFactory(DomainType domainType) {
    this.domainType = requireNonNull(domainType);
  }

  @Override
  public final boolean compatibleWith(DomainType domainType) {
    return this.domainType.equals(requireNonNull(domainType));
  }
}
