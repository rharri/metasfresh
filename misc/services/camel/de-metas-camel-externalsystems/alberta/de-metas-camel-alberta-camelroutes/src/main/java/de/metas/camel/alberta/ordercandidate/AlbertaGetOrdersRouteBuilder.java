/*
 * #%L
 * de-metas-camel-alberta-camelroutes
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

package de.metas.camel.alberta.ordercandidate;

import de.metas.camel.alberta.ordercandidate.processor.CreateExternalReferenceLookupProcessor;
import de.metas.camel.alberta.ordercandidate.processor.CreateJsonOLCandCreateRequestProcessor;
import de.metas.camel.alberta.ordercandidate.processor.CreateMissingBPartnerProcessor;
import de.metas.camel.alberta.ordercandidate.processor.RetrieveOrdersProcessor;
import de.metas.camel.externalsystems.common.ExternalSystemCamelConstants;
import de.metas.common.externalreference.JsonExternalReferenceLookupResponse;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.endpoint.StaticEndpointBuilders;
import org.springframework.stereotype.Component;

import static de.metas.camel.alberta.CamelRouteUtil.setupJacksonDataFormatFor;
import static de.metas.camel.externalsystems.common.ExternalSystemCamelConstants.MF_ERROR_ROUTE_ID;
import static org.apache.camel.builder.endpoint.StaticEndpointBuilders.direct;

@Component
public class AlbertaGetOrdersRouteBuilder extends RouteBuilder
{
	public static final String GET_ORDERS_ROUTE_ID = "Alberta-getSalesOrders";
	public static final String PROCESS_ORDERS_ROUTE_ID = "Alberta-processOrders";
	public static final String PROCESS_ORDER_ROUTE_ID = "Alberta-processOrder";
	public static final String IMPORT_MISSING_BP_ROUTE_ID = "Alberta-importMissingBPs";

	public static final String GET_ORDERS_PROCESSOR_ID = "GetOrdersProcessorId";
	public static final String CREATE_EXTERNAL_REF_LOOKUP_PROCESSOR_ID = "CreateExternalRefLookupProcessorId";
	public static final String CREATE_BPARTNER_REQUEST_PROCESSOR_ID = "CreateBPartnerRequestProcessorId";
	public static final String CREATE_OLCAND_REQUEST_PROCESSOR_ID = "CreateOLCandRequestProcessorId";

	@Override
	public void configure()
	{
		errorHandler(defaultErrorHandler());
		onException(Exception.class)
				.to(direct(MF_ERROR_ROUTE_ID));

		//@formatter:off
			// this EP's name is matching the JsonExternalSystemRequest's ExternalSystem and Command
			from(direct(GET_ORDERS_ROUTE_ID))
					.routeId(GET_ORDERS_ROUTE_ID)
					.streamCaching()
					.process(new RetrieveOrdersProcessor()).id(GET_ORDERS_PROCESSOR_ID)
					.multicast()
						.stopOnException()
						.parallelProcessing(false)
						.to(direct(IMPORT_MISSING_BP_ROUTE_ID), direct(PROCESS_ORDERS_ROUTE_ID))
					.end();

			from(direct(PROCESS_ORDERS_ROUTE_ID))
				.routeId(PROCESS_ORDERS_ROUTE_ID)
				.choice()
					.when(body().isNull())
						.log(LoggingLevel.INFO, "Nothing to do! No orders pulled from alberta!")
					.otherwise()
						.split(body())
							.to(StaticEndpointBuilders.direct(PROCESS_ORDER_ROUTE_ID))
						.end()
				.endChoice();

			from(direct(PROCESS_ORDER_ROUTE_ID))
					.routeId(PROCESS_ORDER_ROUTE_ID)
					.process(new CreateJsonOLCandCreateRequestProcessor()).id(CREATE_OLCAND_REQUEST_PROCESSOR_ID)

					.log(LoggingLevel.DEBUG, "Calling metasfresh-api to store order candidates!")
					.to( direct(ExternalSystemCamelConstants.MF_PUSH_OL_CANDIDATES_ROUTE_ID) );

			from(direct(IMPORT_MISSING_BP_ROUTE_ID))
				.routeId(IMPORT_MISSING_BP_ROUTE_ID)
				.process(new CreateExternalReferenceLookupProcessor()).id(CREATE_EXTERNAL_REF_LOOKUP_PROCESSOR_ID)
				.choice()
					.when(body().isNull())
						.log(LoggingLevel.INFO, "Nothing to do! No external reference lookup request found!")
					.otherwise()
						.log(LoggingLevel.DEBUG, "Calling metasfresh-api to query ESR records: ${body}")
						.to("{{" + ExternalSystemCamelConstants.MF_LOOKUP_EXTERNALREFERENCE_CAMEL_URI + "}}")
						.unmarshal(setupJacksonDataFormatFor(getContext(), JsonExternalReferenceLookupResponse.class))

						.process(new CreateMissingBPartnerProcessor()).id(CREATE_BPARTNER_REQUEST_PROCESSOR_ID)
						.choice()
							.when(body().isNull())
								.log(LoggingLevel.INFO, "Nothing to do! No BPUpsertCamelRequest found!")
							.otherwise()
								.log(LoggingLevel.DEBUG, "Calling metasfresh-api to upsert BP records!")
								.to("{{" + ExternalSystemCamelConstants.MF_UPSERT_BPARTNER_V2_CAMEL_URI + "}}")
						.endChoice()
				.endChoice();
			//@formatter:on
	}
}

