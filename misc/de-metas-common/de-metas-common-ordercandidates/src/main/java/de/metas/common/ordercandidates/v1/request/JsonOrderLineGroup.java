/*
 * #%L
 * de-metas-common-ordercandidates
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

package de.metas.common.ordercandidates.v1.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
public class JsonOrderLineGroup
{
	@ApiModelProperty(value = "All JsonOLCandCreateRequests with the same ExternalHeaderId and the same groupId shall belong to the same bundle (compensation-group)")
	@JsonInclude(NON_NULL)
	String groupKey;
	@ApiModelProperty(value = "If true, marks the associated as the \"main\" product")
	@JsonInclude(NON_NULL)
	boolean isGroupMainItem;

	@Builder
	public JsonOrderLineGroup(@JsonProperty("groupKey") final @Nullable String groupKey,
			@JsonProperty("groupMainItem") final boolean isGroupMainItem)
	{

		this.groupKey = groupKey;
		this.isGroupMainItem = isGroupMainItem;
	}
}
