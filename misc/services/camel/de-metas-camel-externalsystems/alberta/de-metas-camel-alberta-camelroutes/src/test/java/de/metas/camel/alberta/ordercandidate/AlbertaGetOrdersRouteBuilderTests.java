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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.metas.camel.alberta.patient.AlbertaConnectionDetails;
import de.metas.camel.externalsystems.common.ExternalSystemCamelConstants;
import de.metas.camel.externalsystems.common.v2.BPUpsertCamelRequest;
import de.metas.common.externalreference.JsonExternalReferenceLookupRequest;
import de.metas.common.ordercandidates.v2.request.JsonOLCandCreateBulkRequest;
import io.swagger.client.ApiException;
import io.swagger.client.JSON;
import io.swagger.client.api.DoctorApi;
import io.swagger.client.api.PharmacyApi;
import io.swagger.client.model.ArrayOfOrders;
import io.swagger.client.model.Doctor;
import io.swagger.client.model.Order;
import io.swagger.client.model.Pharmacy;
import lombok.NonNull;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.CREATE_BPARTNER_REQUEST_PROCESSOR_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.CREATE_EXTERNAL_REF_LOOKUP_PROCESSOR_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.CREATE_OLCAND_REQUEST_PROCESSOR_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.GET_ORDERS_PROCESSOR_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.GET_ORDERS_ROUTE_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.IMPORT_MISSING_BP_ROUTE_ID;
import static de.metas.camel.alberta.ordercandidate.AlbertaGetOrdersRouteBuilder.PROCESS_ORDER_ROUTE_ID;
import static de.metas.camel.alberta.ordercandidate.GetOrdersRouteConstants.ROUTE_PROPERTY_ORG_CODE;
import static de.metas.camel.alberta.patient.GetPatientsRouteConstants.ROUTE_PROPERTY_ALBERTA_CONN_DETAILS;
import static de.metas.camel.alberta.patient.GetPatientsRouteConstants.ROUTE_PROPERTY_ALBERTA_PHARMACY_API;
import static de.metas.camel.alberta.patient.GetPatientsRouteConstants.ROUTE_PROPERTY_DOCTOR_API;
import static de.metas.camel.externalsystems.common.ExternalSystemCamelConstants.MF_LOOKUP_EXTERNALREFERENCE_CAMEL_URI;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;

public class AlbertaGetOrdersRouteBuilderTests extends CamelTestSupport
{
	private static final String JSON_ALBERTA_MOCK_ORDERS = "10_AlbertaSalesOrders.json";

	private static final String JSON_EXTERNAL_REFERENCE_LOOKUP_REQUEST = "20_ExternalReferenceLookupRequest.json";
	private static final String JSON_EXTERNAL_REFERENCE_LOOKUP_RESPONSE = "20_ExternalReferenceLookupResponse.json";

	private static final String JSON_ALBERTA_GET_DOCTOR_RESPONSE = "30_AlbertaDoctor.json";
	private static final String JSON_ALBERTA_GET_PHARMACY_RESPONSE = "40_AlbertaPharmacy.json";

	private static final String JSON_BPARTNER_UPSERT_REQUEST = "50_BPartnerUpsertRequest.json";
	private static final String JSON_OL_CAND_CREATE_REQUEST = "60_CreateOLCandRequest.json";

	private static final String MOCK_ESR_QUERY_REQUEST = "mock:queryExternalRefRequest";
	private static final String MOCK_UPSERT_BPARTNER_REQUEST = "mock:upsertBPartnerRequest";
	private static final String MOCK_CREATE_OLCAND_REQUEST = "mock:upsertOLCandRequest";

	@Override
	protected Properties useOverridePropertiesWithPropertiesComponent()
	{
		final Properties properties = new Properties();
		try
		{
			properties.load(AlbertaGetOrdersRouteBuilderTests.class.getClassLoader().getResourceAsStream("application.properties"));
			return properties;
		}
		catch (final IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	protected RouteBuilder createRouteBuilder()
	{
		return new AlbertaGetOrdersRouteBuilder();
	}

	@Override
	public boolean isUseAdviceWith()
	{
		return true;
	}

	@Test
	void happyFlow() throws Exception
	{
		final MockExternalReferenceResponse mockExternalReferenceResponse = new MockExternalReferenceResponse();
		final MockBPartnerUpsertResponse mockBPartnerUpsertResponse = new MockBPartnerUpsertResponse();
		final MockOLCandUpsertResponse mockOLCandUpsertResponse = new MockOLCandUpsertResponse();

		prepareRouteForTesting(mockExternalReferenceResponse, mockBPartnerUpsertResponse, mockOLCandUpsertResponse);

		context.start();

		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());

		//validate the external system query request that is done towards metasfresh
		final MockEndpoint esrQueryValidationMockEndpoint = getMockEndpoint(MOCK_ESR_QUERY_REQUEST);
		final InputStream esrQueryRequestExpected = this.getClass().getResourceAsStream(JSON_EXTERNAL_REFERENCE_LOOKUP_REQUEST);
		esrQueryValidationMockEndpoint.expectedBodiesReceived(objectMapper.readValue(esrQueryRequestExpected, JsonExternalReferenceLookupRequest.class));

		//validate the upsert-bpartner-request that is done towards metasfresh
		final MockEndpoint bpartnerUpsertMockEndpoint = getMockEndpoint(MOCK_UPSERT_BPARTNER_REQUEST);
		final InputStream bparnerUpsertRequestExpected = this.getClass().getResourceAsStream(JSON_BPARTNER_UPSERT_REQUEST);
		final BPUpsertCamelRequest bpUpsertCamelRequest = objectMapper.readValue(bparnerUpsertRequestExpected, BPUpsertCamelRequest.class);

		bpartnerUpsertMockEndpoint.expectedBodiesReceived(bpUpsertCamelRequest);

		//validate the upsert ol cand request that is done towards metasfresh
		final MockEndpoint mockCreateOlCandEndpoint = getMockEndpoint(MOCK_CREATE_OLCAND_REQUEST);
		final InputStream olCandRequestIS = this.getClass().getResourceAsStream(JSON_OL_CAND_CREATE_REQUEST);
		final JsonOLCandCreateBulkRequest jsonOLCandCreateBulkRequest = objectMapper.readValue(olCandRequestIS, JsonOLCandCreateBulkRequest.class);
		mockCreateOlCandEndpoint.expectedBodiesReceived(jsonOLCandCreateBulkRequest);

		//fire the route
		template.sendBody("direct:" + GET_ORDERS_ROUTE_ID, "Nothing relevant");

		assertMockEndpointsSatisfied();
		assertThat(mockExternalReferenceResponse.called).isEqualTo(1);
		assertThat(mockBPartnerUpsertResponse.called).isEqualTo(1);
		assertThat(mockOLCandUpsertResponse.called).isEqualTo(1);
	}

	private void prepareRouteForTesting(
			final MockExternalReferenceResponse mockExternalReferenceResponse,
			final MockBPartnerUpsertResponse mockBPartnerUpsertResponse,
			final MockOLCandUpsertResponse mockOLCandUpsertResponse) throws Exception
	{
		// inject our mock processor that returns the orders-JSON from alberta
		AdviceWith.adviceWith(context, GET_ORDERS_ROUTE_ID,
							  advice -> advice.weaveById(GET_ORDERS_PROCESSOR_ID)
									  .replace()
									  .process(new MockGetOrdersApiProcessor()));

		AdviceWith.adviceWith(context, IMPORT_MISSING_BP_ROUTE_ID,
							  advice -> {
								  // validate the the external reference query request and send a response
								  advice.weaveById(CREATE_EXTERNAL_REF_LOOKUP_PROCESSOR_ID)
										  .after()
										  .to(MOCK_ESR_QUERY_REQUEST);

								  advice.interceptSendToEndpoint("{{" + MF_LOOKUP_EXTERNALREFERENCE_CAMEL_URI + "}}")
										  .skipSendToOriginalEndpoint()
										  .process(mockExternalReferenceResponse);

								  // validate the the bpartner upsert request and send a response
								  advice.weaveById(CREATE_BPARTNER_REQUEST_PROCESSOR_ID)
										  .after()
										  .to(MOCK_UPSERT_BPARTNER_REQUEST);

								  advice.interceptSendToEndpoint("{{" + ExternalSystemCamelConstants.MF_UPSERT_BPARTNER_V2_CAMEL_URI + "}}")
										  .skipSendToOriginalEndpoint()
										  .process(mockBPartnerUpsertResponse);
							  });

		AdviceWith.adviceWith(context, PROCESS_ORDER_ROUTE_ID,
							  advice -> {
								  // validate the the external reference query request and send a response
								  advice.weaveById(CREATE_OLCAND_REQUEST_PROCESSOR_ID)
										  .after()
										  .to(MOCK_CREATE_OLCAND_REQUEST);

								  advice.interceptSendToEndpoint("direct:" + ExternalSystemCamelConstants.MF_PUSH_OL_CANDIDATES_ROUTE_ID)
										  .skipSendToOriginalEndpoint()
										  .process(mockOLCandUpsertResponse);
							  });
	}

	private static String loadAsString(@NonNull final String name)
	{
		final InputStream stream = AlbertaGetOrdersRouteBuilderTests.class.getResourceAsStream(name);
		return new BufferedReader(
				new InputStreamReader(stream, StandardCharsets.UTF_8))
				.lines()
				.collect(Collectors.joining("\n"));
	}

	private static class MockGetOrdersApiProcessor implements Processor
	{
		@Override
		public void process(@NonNull final Exchange exchange) throws ApiException
		{
			final JSON json = new JSON();

			exchange.setProperty(ROUTE_PROPERTY_DOCTOR_API, prepareDoctorApiClient(json));
			exchange.setProperty(ROUTE_PROPERTY_ALBERTA_PHARMACY_API, preparePharmacyApiClient(json));
			exchange.setProperty(ROUTE_PROPERTY_ORG_CODE, "001");
			exchange.setProperty(ROUTE_PROPERTY_ALBERTA_CONN_DETAILS, getMockConnectionDetails());

			exchange.getIn().setBody(getMockOrders(json));
		}

		@NonNull
		private List<Order> getMockOrders(@NonNull final JSON json)
		{
			final String mockOrdersStr = loadAsString(JSON_ALBERTA_MOCK_ORDERS);

			return json.deserialize(mockOrdersStr, ArrayOfOrders.class);
		}

		@NonNull
		private PharmacyApi preparePharmacyApiClient(@NonNull final JSON json) throws ApiException
		{
			final PharmacyApi pharmacyApi = Mockito.mock(PharmacyApi.class);

			final String pharmacyStr = loadAsString(JSON_ALBERTA_GET_PHARMACY_RESPONSE);

			final Pharmacy pharmacy = json.deserialize(pharmacyStr, Pharmacy.class);

			Mockito.when(pharmacyApi.getPharmacy(any(String.class), any(String.class), any(String.class)))
					.thenReturn(pharmacy);

			return pharmacyApi;
		}

		@NonNull
		private DoctorApi prepareDoctorApiClient(@NonNull final JSON json) throws ApiException
		{
			final DoctorApi albertaDoctorApi = Mockito.mock(DoctorApi.class);

			final String doctorStr = loadAsString(JSON_ALBERTA_GET_DOCTOR_RESPONSE);

			final Doctor doctor = json.deserialize(doctorStr, Doctor.class);

			Mockito.when(albertaDoctorApi.getDoctor(any(String.class), any(String.class), any(String.class)))
					.thenReturn(doctor);

			return albertaDoctorApi;
		}

		private AlbertaConnectionDetails getMockConnectionDetails()
		{
			return AlbertaConnectionDetails.builder()
					.apiKey("notRelevant")
					.basePath("notRelevant")
					.tenant("notRelevant")
					.build();
		}
	}

	private static class MockExternalReferenceResponse implements Processor
	{
		private int called = 0;

		@Override
		public void process(final Exchange exchange)
		{
			called++;
			final InputStream esrLookupResponse = AlbertaGetOrdersRouteBuilderTests.class.getResourceAsStream(JSON_EXTERNAL_REFERENCE_LOOKUP_RESPONSE);
			exchange.getIn().setBody(esrLookupResponse);
		}
	}

	private static class MockBPartnerUpsertResponse implements Processor
	{
		private int called = 0;

		@Override
		public void process(final Exchange exchange)
		{
			called++;
		}
	}

	private static class MockOLCandUpsertResponse implements Processor
	{
		private int called = 0;

		@Override
		public void process(final Exchange exchange)
		{
			called++;
		}
	}
}
