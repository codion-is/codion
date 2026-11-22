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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.EntityType;

import static java.util.Objects.requireNonNull;

/**
 * Identifies a domain model and serves as a factory for {@link EntityType}
 * instances associated with this domain model type.
 */
public sealed interface DomainType permits DefaultDomainType {

	/**
	 * @return the domain name
	 */
	String name();

	/**
	 * Instantiates a {@link EntityType} associated with this domain type.
	 * If this entity type has been defined previously that instance is returned.
	 * @param name the entity type name
	 * @return a {@link EntityType} with the given name
	 */
	EntityType entityType(String name);

	/**
	 * Instantiates a {@link EntityType} associated with this domain type.
	 * If this entity type has been defined previously that instance is returned.
	 * <p>The bundle's base name is the class's name, read at runtime, so in an application minified by R8 or ProGuard,
	 * an Android release for example, the class must keep its name, by a rule of the application's own, or the bundle
	 * is not found.
	 * @param name the entity type name
	 * @param resourceBundleClass the class to use as base name of the resource bundle to use for captions
	 * @return a {@link EntityType} with the given name
	 */
	EntityType entityType(String name, Class<?> resourceBundleClass);

	/**
	 * Instantiates a {@link EntityType} associated with this domain type.
	 * If this entity type has been defined previously that instance is returned.
	 * @param name the entity type name
	 * @param resourceBundleName the name of a resource bundle to use for captions
	 * @return a {@link EntityType} with the given name
	 */
	EntityType entityType(String name, String resourceBundleName);

	/**
	 * @param entityType the entity type
	 * @return true if this domain type contains the given entity type
	 */
	boolean contains(EntityType entityType);

	/**
	 * Returns a new {@link DomainType} using {@code domainClass.getSimpleName()} as domain name.
	 * <p>The name is read from the class at runtime, so in an application minified by R8 or ProGuard, an Android
	 * release for example, the class must keep its name, or the domain type is named for whatever the class was renamed
	 * to, and a remote connection, sending the name to the server, finds no such domain. This module's R8 rules keep the
	 * name of a class declaring a static {@link DomainType} field, as {@code DomainType DOMAIN = domainType(Chinook.class)}
	 * in a domain's API does. A domain type named for any other class needs a rule of the application's own keeping its
	 * name, or a name given by {@link #domainType(String)}.
	 * @param domainClass the domain class
	 * @return a {@link DomainType}
	 */
	static DomainType domainType(Class<?> domainClass) {
		return domainType(requireNonNull(domainClass).getSimpleName());
	}

	/**
	 * Returns a {@link DomainType} instance with the given name.
	 * @param domainName domain name
	 * @return a {@link DomainType} with the given name
	 */
	static DomainType domainType(String domainName) {
		return DefaultDomainType.getOrCreateDomainType(domainName);
	}

	/**
	 * Returns the domain type with the given name.
	 * @param domainName the domain name
	 * @return the domain type with the given name
	 * @throws IllegalArgumentException in case a domain by that name has not been defined
	 */
	static DomainType getOrThrow(String domainName) {
		return DefaultDomainType.getDomainType(domainName);
	}
}
