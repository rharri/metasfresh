/*
 * #%L
 * de.metas.ui.web.base
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

package de.metas.ui.web.dashboard;

import de.metas.ui.web.dashboard.json.KPIJsonOptions;
import lombok.NonNull;
import lombok.Value;

@Value
public class KPIDataSetValuesAggregationKey
{
	public static final KPIDataSetValuesAggregationKey NO_KEY = new KPIDataSetValuesAggregationKey(KPIDataValue.ofUnknownType(null));

	public static KPIDataSetValuesAggregationKey of(@NonNull final KPIDataValue value)
	{
		return new KPIDataSetValuesAggregationKey(value);
	}

	@NonNull KPIDataValue value;

	private KPIDataSetValuesAggregationKey(@NonNull final KPIDataValue value)
	{
		this.value = value;
	}

	public Object toJsonValue(@NonNull final KPIJsonOptions jsonOpts)
	{
		return value.toJsonValue(jsonOpts);
	}
}
