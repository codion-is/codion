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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.domain.api;

import is.codion.common.db.operation.FunctionType;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition.ValueSupplier;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.DerivedAttribute;
import is.codion.framework.domain.entity.attribute.DerivedAttribute.SourceValues;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.TypeReference;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.plugin.jasperreports.JRReportType;
import is.codion.plugin.jasperreports.JasperReports;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.FieldPosition;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.regex.Pattern;

import static is.codion.common.db.operation.FunctionType.functionType;
import static is.codion.framework.domain.DomainType.domainType;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;

public interface Chinook {

	DomainType DOMAIN = domainType(Chinook.class);

	interface Artist {
		EntityType TYPE = DOMAIN.entityType("chinook.artist", Artist.class.getName());

		Column<Long> ID = TYPE.longColumn("artistid");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> NUMBER_OF_ALBUMS = TYPE.integerColumn("number_of_albums");
		Column<Integer> NUMBER_OF_TRACKS = TYPE.integerColumn("number_of_tracks");
	}

	interface Album {
		EntityType TYPE = DOMAIN.entityType("chinook.album", Album.class.getName());

		Column<Long> ID = TYPE.longColumn("albumid");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> ARTIST_ID = TYPE.longColumn("artistid");
		Column<byte[]> COVER = TYPE.byteArrayColumn("cover");
		Column<Integer> NUMBER_OF_TRACKS = TYPE.integerColumn("number_of_tracks");
		Column<List<String>> TAGS = TYPE.column("tags", new TypeReference<>() {});
		Column<Integer> RATING = TYPE.integerColumn("rating");

		ForeignKey ARTIST_FK = TYPE.foreignKey("artist_fk", ARTIST_ID, Artist.ID);
	}

	interface Employee {
		EntityType TYPE = DOMAIN.entityType("chinook.employee", Employee.class.getName());

		Column<Long> ID = TYPE.longColumn("employeeid");
		Column<String> LASTNAME = TYPE.stringColumn("lastname");
		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
		Column<String> TITLE = TYPE.stringColumn("title");
		Column<Long> REPORTSTO = TYPE.longColumn("reportsto");
		Column<LocalDate> BIRTHDATE = TYPE.localDateColumn("birthdate");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");
		Column<String> ADDRESS = TYPE.stringColumn("address");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<String> STATE = TYPE.stringColumn("state");
		Column<String> COUNTRY = TYPE.stringColumn("country");
		Column<String> POSTALCODE = TYPE.stringColumn("postalcode");
		Column<String> PHONE = TYPE.stringColumn("phone");
		Column<String> FAX = TYPE.stringColumn("fax");
		Column<String> EMAIL = TYPE.stringColumn("email");

		ForeignKey REPORTSTO_FK = TYPE.foreignKey("reportsto_fk", REPORTSTO, Employee.ID);
	}

	interface Customer {
		EntityType TYPE = DOMAIN.entityType("chinook.customer", Customer.class.getName());

		Column<Long> ID = TYPE.longColumn("customerid");
		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");
		Column<String> LASTNAME = TYPE.stringColumn("lastname");
		Column<String> COMPANY = TYPE.stringColumn("company");
		Column<String> ADDRESS = TYPE.stringColumn("address");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<String> STATE = TYPE.stringColumn("state");
		Column<String> COUNTRY = TYPE.stringColumn("country");
		Column<String> POSTALCODE = TYPE.stringColumn("postalcode");
		Column<String> PHONE = TYPE.stringColumn("phone");
		Column<String> FAX = TYPE.stringColumn("fax");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<Long> SUPPORTREP_ID = TYPE.longColumn("supportrepid");

		ForeignKey SUPPORTREP_FK = TYPE.foreignKey("supportrep_fk", SUPPORTREP_ID, Employee.ID);

		JRReportType REPORT = JasperReports.reportType("customer_report");
	}

	interface Genre {
		EntityType TYPE = DOMAIN.entityType("chinook.genre", Genre.class.getName());

		Column<Long> ID = TYPE.longColumn("genreid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	interface MediaType {
		EntityType TYPE = DOMAIN.entityType("chinook.mediatype", MediaType.class.getName());

		Column<Long> ID = TYPE.longColumn("mediatypeid");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	interface Track {
		EntityType TYPE = DOMAIN.entityType("chinook.track", Track.class.getName());

		Column<Long> ID = TYPE.longColumn("trackid");
		Column<String> NAME = TYPE.stringColumn("name");
		Attribute<Entity> ARTIST = TYPE.entityAttribute("artist");
		Column<Long> ALBUM_ID = TYPE.longColumn("albumid");
		Column<Long> MEDIATYPE_ID = TYPE.longColumn("mediatypeid");
		Column<Long> GENRE_ID = TYPE.longColumn("genreid");
		Column<String> COMPOSER = TYPE.stringColumn("composer");
		Column<Integer> MILLISECONDS = TYPE.integerColumn("milliseconds");
		Column<Integer> BYTES = TYPE.integerColumn("bytes");
		Column<Integer> RATING = TYPE.integerColumn("rating");
		Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
		Column<Void> RANDOM = TYPE.column("random()", Void.class);

		ForeignKey ALBUM_FK = TYPE.foreignKey("album_fk", ALBUM_ID, Album.ID);
		ForeignKey MEDIATYPE_FK = TYPE.foreignKey("mediatype_fk", MEDIATYPE_ID, MediaType.ID);
		ForeignKey GENRE_FK = TYPE.foreignKey("genre_fk", GENRE_ID, Genre.ID);

		FunctionType<EntityConnection, RaisePriceParameters, Collection<Entity>> RAISE_PRICE = functionType("chinook.raise_price");

		ConditionType NOT_IN_PLAYLIST = TYPE.conditionType("not_in_playlist");

		record RaisePriceParameters(Collection<Long> trackIds, BigDecimal priceIncrease) implements Serializable {

			public RaisePriceParameters {
				requireNonNull(trackIds);
				requireNonNull(priceIncrease);
			}
		}
	}

	interface Invoice {
		EntityType TYPE = DOMAIN.entityType("chinook.invoice", Invoice.class.getName());

		Column<Long> ID = TYPE.longColumn("invoiceid");
		Column<Long> CUSTOMER_ID = TYPE.longColumn("customerid");
		Column<LocalDate> DATE = TYPE.localDateColumn("invoicedate");
		Column<String> BILLINGADDRESS = TYPE.stringColumn("billingaddress");
		Column<String> BILLINGCITY = TYPE.stringColumn("billingcity");
		Column<String> BILLINGSTATE = TYPE.stringColumn("billingstate");
		Column<String> BILLINGCOUNTRY = TYPE.stringColumn("billingcountry");
		Column<String> BILLINGPOSTALCODE = TYPE.stringColumn("billingpostalcode");
		Column<BigDecimal> TOTAL = TYPE.bigDecimalColumn("total");
		Column<BigDecimal> CALCULATED_TOTAL = TYPE.bigDecimalColumn("calculated_total");

		ForeignKey CUSTOMER_FK = TYPE.foreignKey("customer_fk", CUSTOMER_ID, Customer.ID);

		FunctionType<EntityConnection, Collection<Long>, Collection<Entity>> UPDATE_TOTALS = functionType("chinook.update_totals");

		ValueSupplier<LocalDate> DATE_DEFAULT_VALUE = LocalDate::now;
	}

	interface InvoiceLine {
		EntityType TYPE = DOMAIN.entityType("chinook.invoiceline", InvoiceLine.class.getName());

		Column<Long> ID = TYPE.longColumn("invoicelineid");
		Column<Long> INVOICE_ID = TYPE.longColumn("invoiceid");
		Column<Long> TRACK_ID = TYPE.longColumn("trackid");
		Column<BigDecimal> UNITPRICE = TYPE.bigDecimalColumn("unitprice");
		Column<Integer> QUANTITY = TYPE.integerColumn("quantity");
		Attribute<BigDecimal> TOTAL = TYPE.bigDecimalAttribute("total");

		ForeignKey INVOICE_FK = TYPE.foreignKey("invoice_fk", INVOICE_ID, Invoice.ID);
		ForeignKey TRACK_FK = TYPE.foreignKey("track_fk", TRACK_ID, Track.ID);
	}

	interface Playlist {
		EntityType TYPE = DOMAIN.entityType("chinook.playlist", Playlist.class.getName());

		Column<Long> ID = TYPE.longColumn("playlistid");
		Column<String> NAME = TYPE.stringColumn("name");

		FunctionType<EntityConnection, RandomPlaylistParameters, Entity> RANDOM_PLAYLIST = functionType("chinook.random_playlist");

		record RandomPlaylistParameters(String playlistName, Integer noOfTracks, Collection<Entity> genres) implements Serializable {}
	}

	interface PlaylistTrack {
		EntityType TYPE = DOMAIN.entityType("chinook.playlisttrack", PlaylistTrack.class.getName());

		Column<Long> ID = TYPE.longColumn("playlisttrackid");
		Column<Long> PLAYLIST_ID = TYPE.longColumn("playlistid");
		Column<Long> TRACK_ID = TYPE.longColumn("trackid");
		Attribute<Entity> ALBUM = TYPE.entityAttribute("album");
		Attribute<Entity> ARTIST = TYPE.entityAttribute("artist");

		ForeignKey PLAYLIST_FK = TYPE.foreignKey("playlist_fk", PLAYLIST_ID, Playlist.ID);
		ForeignKey TRACK_FK = TYPE.foreignKey("track_fk", TRACK_ID, Track.ID);
	}

	final class InvoiceLineTotalProvider
					implements DerivedAttribute.Provider<BigDecimal> {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public BigDecimal get(SourceValues values) {
			Integer quantity = values.get(InvoiceLine.QUANTITY);
			BigDecimal unitPrice = values.get(InvoiceLine.UNITPRICE);
			if (unitPrice == null || quantity == null) {
				return null;
			}

			return unitPrice.multiply(BigDecimal.valueOf(quantity));
		}
	}

	final class CustomerStringFactory
					implements Function<Entity, String>, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private static final String LANGUAGE = Locale.getDefault().getLanguage();

		@Override
		public String apply(Entity customer) {
			switch (LANGUAGE) {
				case "en":
					return new StringBuilder()
									.append(customer.get(Customer.LASTNAME))
									.append(", ")
									.append(customer.get(Customer.FIRSTNAME))
									.toString();
				case "is":
					return new StringBuilder()
									.append(customer.get(Customer.FIRSTNAME))
									.append(" ")
									.append(customer.get(Customer.LASTNAME))
									.toString();
				default:
					throw new IllegalArgumentException("Unsupported language: " + LANGUAGE);
			}
		}
	}

	final class CoverFormatter extends Format {

		private final NumberFormat kbFormat = NumberFormat.getIntegerInstance();

		@Override
		public StringBuffer format(Object value, StringBuffer toAppendTo, FieldPosition pos) {
			if (value != null) {
				toAppendTo.append(kbFormat.format(((byte[]) value).length / 1024) + " Kb");
			}

			return toAppendTo;
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			throw new UnsupportedOperationException();
		}
	}

	final class EmailValidator extends DefaultEntityValidator {

		private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.+)@(.+)$");
		private static final ResourceBundle BUNDLE = getBundle(Chinook.class.getName());

		private final Column<String> emailColumn;

		public EmailValidator(Column<String> emailColumn) {
			this.emailColumn = emailColumn;
		}

		@Override
		public <T> void validate(Entity entity, Attribute<T> attribute) {
			super.validate(entity, attribute);
			if (attribute.equals(emailColumn)) {
				validateEmail(entity.get(emailColumn));
			}
		}

		private void validateEmail(String email) {
			if (!EMAIL_PATTERN.matcher(email).matches()) {
				throw new ValidationException(emailColumn, email, BUNDLE.getString("invalid_email"));
			}
		}
	}
}
