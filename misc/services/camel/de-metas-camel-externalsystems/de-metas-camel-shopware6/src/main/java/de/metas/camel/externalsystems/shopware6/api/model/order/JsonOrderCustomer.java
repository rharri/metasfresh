/*
 * #%L
 * de-metas-camel-shopware6
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

package de.metas.camel.externalsystems.shopware6.api.model.order;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder
@JsonDeserialize(builder = JsonOrderCustomer.JsonOrderCustomerBuilder.class)
public class JsonOrderCustomer
{
	@NonNull
	@JsonProperty("id")
	String id;

	@NonNull
	@JsonProperty("versionId")
	String versionId;

	@NonNull
	@JsonProperty("orderId")
	String orderId;

	@NonNull
	@JsonProperty("customerId")
	String customerId;

	@NonNull
	@JsonProperty("firstName")
	String firstName;

	@NonNull
	@JsonProperty("lastName")
	String lastName;

	@JsonProperty("company")
	String company;

	@JsonProperty("customerNumber")
	String customerNumber;

	@JsonProperty("email")
	String email;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonPOJOBuilder(withPrefix = "")
	static class JsonOrderCustomerBuilder
	{
	}
}

