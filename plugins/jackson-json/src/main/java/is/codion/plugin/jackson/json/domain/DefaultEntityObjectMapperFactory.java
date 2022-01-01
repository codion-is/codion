/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.DomainType;

import static java.util.Objects.requireNonNull;

/**
 * A default {@link EntityObjectMapperFactory} implementation, extend to add custom serialisers/deserializers.<br><br>
 * Subclasses should be exposed as a service.
 */
public abstract class DefaultEntityObjectMapperFactory implements EntityObjectMapperFactory {

  private final DomainType domainType;

  /**
   * Instantiates a new instance compatible with the given domain type.
   * @param domainType the domain type
   */
  protected DefaultEntityObjectMapperFactory(final DomainType domainType) {
    this.domainType = requireNonNull(domainType);
  }

  @Override
  public final boolean isCompatibleWith(final DomainType domainType) {
    return this.domainType.equals(requireNonNull(domainType));
  }
}
