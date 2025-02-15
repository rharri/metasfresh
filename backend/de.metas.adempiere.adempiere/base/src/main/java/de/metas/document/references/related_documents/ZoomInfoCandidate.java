/*
 * #%L
 * de.metas.adempiere.adempiere.base
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

package de.metas.document.references.related_documents;

import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import de.metas.i18n.ITranslatableString;
import de.metas.i18n.TranslatableStrings;
import de.metas.logging.LogManager;
import de.metas.util.Check;
import de.metas.util.lang.Priority;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.compiere.model.MQuery;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class ZoomInfoCandidate
{
	private static final Logger logger = LogManager.getLogger(ZoomInfoCandidate.class);

	@Getter
	private final ZoomInfoId id;
	private final String internalName;
	private final ITranslatableString destinationDisplay;
	private final MQuery query;
	@Getter
	private final ZoomTargetWindow targetWindow;
	private final Priority priority;
	private final ZoomInfoRecordsCountSupplier recordsCountSupplier;

	@Builder
	private ZoomInfoCandidate(
			@NonNull final ZoomInfoId id,
			@NonNull final String internalName,
			@NonNull final ZoomTargetWindow targetWindow,
			@NonNull final Priority priority,
			@NonNull final MQuery query,
			@NonNull final ITranslatableString destinationDisplay,
			@NonNull final ZoomInfoRecordsCountSupplier recordsCountSupplier)
	{
		this.id = id;
		this.internalName = Check.assumeNotEmpty(internalName, "internalName is not empty");

		this.targetWindow = targetWindow;
		this.priority = priority;

		this.query = query;
		this.destinationDisplay = destinationDisplay;

		this.recordsCountSupplier = recordsCountSupplier;
	}

	@Override
	public String toString()
	{
		return MoreObjects.toStringHelper(this)
				.add("id", id)
				.add("internalName", internalName)
				.add("display", destinationDisplay.getDefaultValue())
				.add("adWindowId", targetWindow)
				.toString();
	}

	public Optional<ZoomInfo> evaluate()
	{
		return evaluate(null);
	}

	public Optional<ZoomInfo> evaluate(@Nullable final ZoomTargetWindowEvaluationContext context)
	{
		//
		// Only consider a window already seen if it actually has record count > 0 (task #1062)
		final Priority alreadySeenZoomInfoPriority = context != null
				? context.getPriorityOrNull(targetWindow)
				: null;
		if (alreadySeenZoomInfoPriority != null
				&& alreadySeenZoomInfoPriority.isHigherThan(priority))
		{
			logger.debug("Skipping zoomInfo {} because there is already one for destination '{}'", this, targetWindow);
			return Optional.empty();
		}

		//
		// Compute records count
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final int recordsCount = recordsCountSupplier.getRecordsCount();
		stopwatch.stop();
		logger.debug("Computed records count for {} in {}", this, stopwatch);

		if (recordsCount <= 0)
		{
			logger.debug("No records found for {}", this);
			return Optional.empty();
		}

		//
		// We got a valid zoom info
		// => accept it
		if (context != null)
		{
			context.putWindow(targetWindow, priority);
		}

		final ITranslatableString caption = TranslatableStrings.builder()
				.append(destinationDisplay)
				.append(" (#" + recordsCount + ")")
				.build();

		final Duration recordsCountDuration = Duration.ofNanos(stopwatch.elapsed(TimeUnit.NANOSECONDS));
		final MQuery queryCopy = query.deepCopy();
		queryCopy.setRecordCount(recordsCount, recordsCountDuration);

		return Optional.of(ZoomInfo.builder()
				.id(id)
				.internalName(internalName)
				.targetWindow(targetWindow)
				.priority(priority)
				.caption(caption)
				.query(queryCopy)
				.build());
	}
}
