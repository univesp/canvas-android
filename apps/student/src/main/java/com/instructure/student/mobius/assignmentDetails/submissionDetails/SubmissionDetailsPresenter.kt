/*
 * Copyright (C) 2019 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.instructure.student.mobius.assignmentDetails.submissionDetails

import android.content.Context
import com.instructure.canvasapi2.utils.DateHelper
import com.instructure.student.R
import com.instructure.student.mobius.assignmentDetails.submissionDetails.ui.SubmissionDetailsTabData
import com.instructure.student.mobius.assignmentDetails.submissionDetails.ui.SubmissionDetailsViewState
import com.instructure.student.mobius.common.ui.Presenter


object SubmissionDetailsPresenter : Presenter<SubmissionDetailsModel, SubmissionDetailsViewState> {
    override fun present(model: SubmissionDetailsModel, context: Context): SubmissionDetailsViewState {
        if (model.isLoading) return SubmissionDetailsViewState.Loading
        if (model.assignment?.isSuccess != true || model.rootSubmission?.isSuccess != true) return SubmissionDetailsViewState.Error

        val rootSubmission = model.rootSubmission.dataOrThrow
        val assignment = model.assignment.dataOrThrow

        val atSeparator = context.getString(R.string.at)

        val validSubmissions = rootSubmission.submissionHistory
            .filterNotNull()
            .sortedByDescending { it.submittedAt }

        val selectedSubmission = validSubmissions.first { it.attempt == model.selectedSubmissionAttempt }

        val submissionVersions: List<Pair<Long, String>> = validSubmissions
            .map {
                val formattedDate = DateHelper.getMonthDayAtTime(context, it.submittedAt, atSeparator) ?: ""
                it.attempt to formattedDate
            }

        val selectedVersionIdx = submissionVersions
            .indexOfFirst { it.first == model.selectedSubmissionAttempt }
            .coerceAtLeast(0)

        val tabData = mutableListOf<SubmissionDetailsTabData>()

        // Comments tab
        tabData += SubmissionDetailsTabData.CommentData(
            name = context.getString(R.string.comments),
            assignmentId = model.assignmentId
        )

        // Files tab
        with (selectedSubmission.attachments) {
            val name = if (isEmpty()) {
                context.getString(R.string.files)
            } else {
                context.getString(R.string.submissionDetailsFileTabName, size)
            }
            tabData += SubmissionDetailsTabData.FileData(
                name = name,
                files = this,
                selectedFileId = getOrNull(0)?.id ?: 0,
                canvasContext = model.canvasContext
            )
        }

        // Rubric tab
        tabData += SubmissionDetailsTabData.RubricData(
            name = context.getString(R.string.rubric),
            assignment = assignment,
            submission = rootSubmission
        )

        return SubmissionDetailsViewState.Loaded(
            showVersionsSpinner = submissionVersions.size > 1,
            selectedVersionSpinnerIndex = selectedVersionIdx,
            submissionVersions = submissionVersions,
            tabData = tabData
        )
    }
}
