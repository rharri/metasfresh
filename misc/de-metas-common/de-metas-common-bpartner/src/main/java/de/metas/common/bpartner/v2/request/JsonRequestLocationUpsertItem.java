/*
 * #%L
 * de-metas-common-bpartner
 * %%
 * Copyright (C) 2021 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package de.metas.common.bpartner.v2.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import static de.metas.common.rest_api.v2.SwaggerDocConstants.LOCATION_IDENTIFIER_DOC;

@Value
@Builder(toBuilder = true)
@ApiModel(description = "Contains an external id and the actual bpartner to insert or update. The response will contain the given external id.")
public class JsonRequestLocationUpsertItem
{
	@ApiModelProperty(allowEmptyValue = false, position = 10, //
			value = LOCATION_IDENTIFIER_DOC
					+ "If a new location is created and the request's location has no different identifier, then this identifier is stored within the newly created location.") //
	@NonNull
	String locationIdentifier;

	@ApiModelProperty(allowEmptyValue = false, position = 20, value = "The location to upsert")
	@NonNull
	JsonRequestLocation location;

	@JsonCreator
	public JsonRequestLocationUpsertItem(
			@NonNull @JsonProperty("locationIdentifier") final String locationIdentifier,
			@NonNull @JsonProperty("location") final JsonRequestLocation location)
	{
		this.locationIdentifier = locationIdentifier;
		this.location = location;
	}
}
