package de.metas.elasticsearch.denormalizers.impl;

import de.metas.elasticsearch.config.ESTextAnalyzer;
import de.metas.elasticsearch.denormalizers.IESValueDenormalizer;
import lombok.NonNull;
import lombok.ToString;
import org.elasticsearch.common.xcontent.XContentBuilder;

import javax.annotation.Nullable;
import java.io.IOException;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2016 metas GmbH
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

@ToString
final class PassThroughValueDenormalizer implements IESValueDenormalizer
{
	public static PassThroughValueDenormalizer of(
			@NonNull final ESDataType dataType,
			@Nullable final ESTextAnalyzer textAnalyzer)
	{
		return new PassThroughValueDenormalizer(dataType, textAnalyzer);
	}

	@NonNull private final ESDataType dataType;
	@Nullable private final ESTextAnalyzer textAnalyzer;

	private PassThroughValueDenormalizer(
			@NonNull final ESDataType dataType,
			@Nullable final ESTextAnalyzer textAnalyzer)
	{
		this.dataType = dataType;
		this.textAnalyzer = textAnalyzer;
	}

	@Override
	public void appendMapping(final XContentBuilder builder, final String fieldName) throws IOException
	{
		builder.startObject(fieldName);

		builder.field("type", dataType.getEsTypeAsString());
		//builder.field("fielddata", "true");

		if (textAnalyzer != null)
		{
			builder.field("analyzer", textAnalyzer.getAnalyzerAsString());
		}

		builder.endObject();
	}

	@Nullable
	@Override
	public Object denormalizeValue(@Nullable final Object value) { return value; }
}
