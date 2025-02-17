/*
 * Copyright (C) 2019 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.instructure.student.test.assignment.details.submissionDetails

import android.net.Uri
import android.webkit.MimeTypeMap
import com.instructure.canvasapi2.models.*
import com.instructure.canvasapi2.utils.ApiPrefs
import com.instructure.canvasapi2.utils.DataResult
import com.instructure.canvasapi2.utils.Failure
import com.instructure.student.mobius.assignmentDetails.submissionDetails.*
import com.instructure.student.test.util.matchesEffects
import com.instructure.student.test.util.matchesFirstEffects
import com.instructure.student.util.Const
import com.spotify.mobius.test.FirstMatchers
import com.spotify.mobius.test.InitSpec
import com.spotify.mobius.test.InitSpec.assertThatFirst
import com.spotify.mobius.test.NextMatchers
import com.spotify.mobius.test.NextMatchers.hasModel
import com.spotify.mobius.test.UpdateSpec
import com.spotify.mobius.test.UpdateSpec.assertThatNext
import io.mockk.*
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SubmissionDetailsUpdateTest : Assert() {

    private val initSpec = InitSpec(SubmissionDetailsUpdate()::init)
    private val updateSpec = UpdateSpec(SubmissionDetailsUpdate()::update)
    private val url = "http://www.google.com"

    private lateinit var course: Course
    private lateinit var assignment: Assignment
    private lateinit var submission: Submission
    private lateinit var initModel: SubmissionDetailsModel
    private var isArcEnabled = false

    @Before
    fun setup() {
        course = Course()
        assignment = Assignment(id = 1234L, courseId = course.id)
        submission = Submission(id = 30L, attempt = 1L, assignmentId = assignment.id)
        initModel = SubmissionDetailsModel(assignmentId = assignment.id, canvasContext = course, isArcEnabled = isArcEnabled)
    }

    @Test
    fun `Initializes into a loading state`() {
        val expectedModel = initModel.copy(isLoading = true)
        initSpec
            .whenInit(initModel)
            .then(
                assertThatFirst(
                    FirstMatchers.hasModel(expectedModel),
                    matchesFirstEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(
                        SubmissionDetailsEffect.LoadData(course.id, assignment.id)
                    )
                )
            )
    }

    @Test
    fun `RefreshRequested event produces a loading state and LoadData effect`() {
        val expectedModel = initModel.copy(isLoading = true)
        val expectedEffect = SubmissionDetailsEffect.LoadData(initModel.canvasContext.id, initModel.assignmentId)
        updateSpec.given(initModel)
            .whenEvent(SubmissionDetailsEvent.RefreshRequested)
            .then(
                assertThatNext(
                    hasModel(expectedModel),
                    matchesEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(expectedEffect)
                )
            )
    }

    // region SubmissionClicked event tests

    @Test
    fun `SubmissionClicked event results in model change and a ShowSubmissionContentType effect`() {
        val submission = submission.copy(
            body = "submission body",
            submissionType = Assignment.SubmissionType.ONLINE_TEXT_ENTRY.apiString
        )
        initModel = initModel.copy(
            assignment = DataResult.Success(assignment),
            rootSubmission = DataResult.Success(
                Submission(
                    submissionHistory = listOf(
                        Submission(id = 1),
                        Submission(id = 2),
                        submission
                    )
                )
            )
        )
        val contentType = SubmissionDetailsContentType.TextContent(submission.body!!)
        val expectedModel = initModel.copy(
            selectedSubmissionAttempt = submission.attempt
        )

        updateSpec
            .given(initModel)
            .whenEvent(SubmissionDetailsEvent.SubmissionClicked(submission.attempt))
            .then(
                assertThatNext(
                    hasModel(expectedModel),
                    matchesEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(
                        SubmissionDetailsEffect.ShowSubmissionContentType(contentType)
                    )
                )
            )
    }

    @Test
    fun `SubmissionClicked event results in no change if submission is already selected`() {
        val submission = submission.copy(
            body = "submission body",
            submissionType = Assignment.SubmissionType.ONLINE_TEXT_ENTRY.apiString
        )
        initModel = initModel.copy(
            selectedSubmissionAttempt = submission.attempt,
            assignment = DataResult.Success(assignment),
            rootSubmission = DataResult.Success(
                Submission(
                    submissionHistory = listOf(
                        Submission(id = 1),
                        Submission(id = 2),
                        submission
                    )
                )
            )
        )

        updateSpec
            .given(initModel)
            .whenEvent(SubmissionDetailsEvent.SubmissionClicked(submission.attempt))
            .then(assertThatNext(NextMatchers.hasNothing()))
    }

    @Test
    fun `SubmissionClicked event with no corresponding submission results in model change and a ShowSubmissionContentType effect of NoSubmissionContent`() {
        val submissionId = 1234L
        val contentType =
            SubmissionDetailsContentType.NoSubmissionContent(course, assignment, isArcEnabled) // No submission in the model with the selected ID maps to NoSubmissionContent type

        initModel = initModel.copy(assignment = DataResult.Success(assignment))

        val expectedModel = initModel.copy(
                selectedSubmissionAttempt = submissionId,
                assignment = DataResult.Success(assignment)
        )

        updateSpec
            .given(initModel)
            .whenEvent(SubmissionDetailsEvent.SubmissionClicked(submissionId))
            .then(
                assertThatNext(
                    hasModel(expectedModel),
                    matchesEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(
                        SubmissionDetailsEffect.ShowSubmissionContentType(contentType)
                    )
                )
            )
    }

    // endregion

    // region DataLoaded event tests

    @Test
    fun `DataLoaded event with a failed submission DataResult results in model change and a ShowSubmissionContentType effect of NoSubmissionContent`() {
        initModel = initModel.copy(isLoading = true)
        val assignment = DataResult.Success(assignment)
        val submission = DataResult.Fail(Failure.Network("ErRoR"))
        val contentType = SubmissionDetailsContentType.NoSubmissionContent(course, assignment.data, isArcEnabled)
        val expectedModel = initModel.copy(
            isLoading = false,
            assignment = assignment,
            rootSubmission = submission,
            selectedSubmissionAttempt = null,
            isArcEnabled = isArcEnabled
        )
        updateSpec
            .given(initModel)
            .whenEvent(SubmissionDetailsEvent.DataLoaded(assignment, submission, isArcEnabled))
            .then(
                assertThatNext(
                    hasModel(expectedModel),
                    matchesEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(
                        SubmissionDetailsEffect.ShowSubmissionContentType(contentType)
                    )
                )
            )
    }

    // region getSubmissionContentType tests

    @Test
    fun `NONE results in SubmissionDetailsContentType of NoneContent`() {
        verifyGetSubmissionContentType(
            assignment.copy(submissionTypesRaw = listOf(Assignment.SubmissionType.NONE.apiString)),
            submission,
            SubmissionDetailsContentType.NoneContent
        )
    }

    @Test
    fun `ON_PAPER results in SubmissionDetailsContentType of OnPaperContent`() {
        verifyGetSubmissionContentType(
            assignment.copy(submissionTypesRaw = listOf(Assignment.SubmissionType.ON_PAPER.apiString)),
            submission,
            SubmissionDetailsContentType.OnPaperContent
        )
    }

    @Test
    fun `ASSIGNMENT_STATE_MISSING results in SubmissionDetailsContentType of NoSubmissionContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(attempt = 0),
            SubmissionDetailsContentType.NoSubmissionContent(course, assignment, isArcEnabled)
        )
    }

    @Test
    fun `ASSIGNMENT_STATE_GRADED_MISSING results in SubmissionDetailsContentType of NoSubmissionContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(missing = true),
            SubmissionDetailsContentType.NoSubmissionContent(course, assignment, isArcEnabled)
        )
    }

    @Test
    fun `ONLINE_TEXT_ENTRY results in SubmissionDetailsContentType of TextContent`() {
        val body = "submission body"
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(body = body, submissionType = Assignment.SubmissionType.ONLINE_TEXT_ENTRY.apiString),
            SubmissionDetailsContentType.TextContent(body)
        )
    }

    @Test
    fun `ONLINE_TEXT_ENTRY with a null body results in SubmissionDetailsContentType of TextContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(body = null, submissionType = Assignment.SubmissionType.ONLINE_TEXT_ENTRY.apiString),
            SubmissionDetailsContentType.TextContent("")
        )
    }

    @Test
    fun `BASIC_LTI_LAUNCH results in SubmissionDetailsContentType of ExternalToolContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(previewUrl = url, submissionType = Assignment.SubmissionType.BASIC_LTI_LAUNCH.apiString),
            SubmissionDetailsContentType.ExternalToolContent(initModel.canvasContext, url)
        )
    }

    @Test
    fun `BASIC_LTI_LAUNCH without a preview url results in SubmissionDetailsContentType of ExternalToolContent`() {
        verifyGetSubmissionContentType(
            assignment.copy(url = url),
            submission.copy(previewUrl = null, submissionType = Assignment.SubmissionType.BASIC_LTI_LAUNCH.apiString),
            SubmissionDetailsContentType.ExternalToolContent(initModel.canvasContext, url)
        )
    }

    @Test
    fun `BASIC_LTI_LAUNCH without a preview url or an assignment url results in SubmissionDetailsContentType of ExternalToolContent`() {
        verifyGetSubmissionContentType(
            assignment.copy(url = null, htmlUrl = url),
            submission.copy(previewUrl = null, submissionType = Assignment.SubmissionType.BASIC_LTI_LAUNCH.apiString),
            SubmissionDetailsContentType.ExternalToolContent(initModel.canvasContext, url)
        )
    }

    @Test
    fun `BASIC_LTI_LAUNCH without a preview url or an assignment url or an assignment html url results in SubmissionDetailsContentType of ExternalToolContent`() {
        verifyGetSubmissionContentType(
            assignment.copy(url = null, htmlUrl = null),
            submission.copy(previewUrl = null, submissionType = Assignment.SubmissionType.BASIC_LTI_LAUNCH.apiString),
            SubmissionDetailsContentType.ExternalToolContent(initModel.canvasContext, "")
        )
    }

    @Test
    fun `MEDIA_RECORDING results in SubmissionDetailsContentType of MediaContent`() {
        val contentType = "jpeg"
        val displayName = "Display Name"

        val uri = mockk<Uri>()
        mockkStatic(Uri::class)
        every { Uri.parse(url) } returns uri

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                mediaComment = MediaComment(
                    url = url,
                    contentType = contentType,
                    displayName = displayName
                ),
                submissionType = Assignment.SubmissionType.MEDIA_RECORDING.apiString
            ),
            SubmissionDetailsContentType.MediaContent(Uri.parse(url), contentType, null, displayName)
        )
    }

    @Test
    fun `MEDIA_RECORDING with null content type results in SubmissionDetailsContentType of MediaContent`() {
        val displayName = "Display Name"

        val uri = mockk<Uri>()
        mockkStatic(Uri::class)
        every { Uri.parse(url) } returns uri

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                mediaComment = MediaComment(url = url, contentType = null, displayName = displayName),
                submissionType = Assignment.SubmissionType.MEDIA_RECORDING.apiString
            ),
            SubmissionDetailsContentType.MediaContent(uri, "", null, displayName)
        )
    }

    @Test
    fun `MEDIA_RECORDING with null media comment results in SubmissionDetailsContentType of UnsupportedContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(mediaComment = null, submissionType = Assignment.SubmissionType.MEDIA_RECORDING.apiString),
            SubmissionDetailsContentType.UnsupportedContent
        )
    }

    @Test
    fun `ONLINE_URL results in SubmissionDetailsContentType of UrlContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                url = url,
                attachments = arrayListOf(Attachment(url = url)),
                submissionType = Assignment.SubmissionType.ONLINE_URL.apiString
            ),
            SubmissionDetailsContentType.UrlContent(url, url)
        )
    }

    @Test
    fun `ONLINE_URL with no attachments results in SubmissionDetailsContentType of UrlContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(url = url, submissionType = Assignment.SubmissionType.ONLINE_URL.apiString),
            SubmissionDetailsContentType.UrlContent(url, null)
        )
    }

    @Test
    fun `ONLINE_QUIZ results in SubmissionDetailsContentType of QuizContent`() {
        val url = "https://example.com"
        val quizId = 987L
        val attempt = 2L
        mockkStatic(ApiPrefs::class)
        every { ApiPrefs.fullDomain } returns url

        verifyGetSubmissionContentType(
            assignment.copy(quizId = quizId),
            submission.copy(
                submissionType = Assignment.SubmissionType.ONLINE_QUIZ.apiString,
                attempt = attempt
            ),
            SubmissionDetailsContentType.QuizContent(
                url + "/courses/${initModel.canvasContext.id}/quizzes/$quizId/history?version=$attempt&headless=1"
            )
        )
        unmockkStatic(ApiPrefs::class)
    }

    @Test
    fun `DISCUSSION_TOPIC results in SubmissionDetailsContentType of DiscussionContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(previewUrl = url, submissionType = Assignment.SubmissionType.DISCUSSION_TOPIC.apiString),
            SubmissionDetailsContentType.DiscussionContent(url)
        )
    }

    @Test
    fun `BROKEN_TYPE results in SubmissionDetailsContentType of UnsupportedContent`() {
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(submissionType = "BROKEN_TYPE"),
            SubmissionDetailsContentType.UnsupportedContent
        )
    }

    @Test
    fun `ONLINE_UPLOAD with no content type results in SubmissionDetailsContentType of OtherAttachmentContent`() {
        val attachment = Attachment(contentType = null)
        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.OtherAttachmentContent(attachment)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with wildcard type uses filename for type and results in SubmissionDetailsContentType of OtherAttachmentContent`() {
        val attachment = Attachment(contentType = "*/*", filename = "stuff.apk")

        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton().getMimeTypeFromExtension(any()) } returns "apk"

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.OtherAttachmentContent(attachment)
        )
    }

    @Test
    fun `ONLINE_UPLOAD uses url for type and results in SubmissionDetailsContentType of OtherAttachmentContent`() {
        val attachment = Attachment(contentType = "*/*", filename = null, url = "www.google.com/thing.apk")

        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton().getMimeTypeFromExtension(any()) } returns null
        every { MimeTypeMap.getFileExtensionFromUrl(any()) } returns "apk"

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.OtherAttachmentContent(attachment)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with wildcard type results in SubmissionDetailsContentType of OtherAttachmentContent`() {
        val attachment = Attachment(contentType = "*/*", filename = null, url = null)

        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton().getMimeTypeFromExtension(any()) } returns null
        every { MimeTypeMap.getFileExtensionFromUrl(any()) } returns null

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.OtherAttachmentContent(attachment)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with canvadoc preview url results in SubmissionDetailsContentType of PdfContent`() {
        val attachment =
            Attachment(contentType = "can be anything, just not null", previewUrl = url + "/" + Const.CANVADOC)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.PdfContent(attachment.previewUrl!!)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with pdf type and canvadoc preview url results in SubmissionDetailsContentType of PdfContent`() {
        val attachment = Attachment(contentType = "application/pdf", previewUrl = url + "/" + Const.CANVADOC)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.PdfContent(attachment.previewUrl!!)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with pdf type and url results in SubmissionDetailsContentType of PdfContent`() {
        val attachment = Attachment(contentType = "application/pdf", url = url)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.PdfContent(attachment.url!!)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with pdf type and no url results in SubmissionDetailsContentType of PdfContent`() {
        val attachment = Attachment(contentType = "application/pdf", url = null)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.PdfContent("")
        )
    }

    @Test
    fun `ONLINE_UPLOAD with audio type results in SubmissionDetailsContentType of MediaContent`() {
        val attachment = Attachment(contentType = "audio", url = url)

        val uri = mockk<Uri>()
        mockkStatic(Uri::class)
        every { Uri.parse(url) } returns uri

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.MediaContent(
                uri,
                attachment.contentType,
                attachment.thumbnailUrl,
                attachment.displayName
            )
        )
    }

    @Test
    fun `ONLINE_UPLOAD with video type results in SubmissionDetailsContentType of MediaContent`() {
        val attachment = Attachment(contentType = "video", url = url)

        val uri = mockk<Uri>()
        mockkStatic(Uri::class)
        every { Uri.parse(url) } returns uri

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.MediaContent(
                uri,
                attachment.contentType,
                attachment.thumbnailUrl,
                attachment.displayName
            )
        )
    }

    @Test
    fun `ONLINE_UPLOAD with image type results in SubmissionDetailsContentType of ImageContent`() {
        val attachment = Attachment(contentType = "image", url = url)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.ImageContent(url, attachment.contentType!!)
        )
    }

    @Test
    fun `ONLINE_UPLOAD with image type and no url results in SubmissionDetailsContentType of ImageContent`() {
        val attachment = Attachment(contentType = "image", url = null)

        verifyGetSubmissionContentType(
            assignment,
            submission.copy(
                attachments = arrayListOf(attachment),
                submissionType = Assignment.SubmissionType.ONLINE_UPLOAD.apiString
            ),
            SubmissionDetailsContentType.ImageContent("", attachment.contentType!!)
        )
    }

    private fun verifyGetSubmissionContentType(
        assignment: Assignment,
        submission: Submission,
        expectedContentType: SubmissionDetailsContentType
    ) {
        val assignmentResult = DataResult.Success(assignment)
        val submissionResult = DataResult.Success(submission)
        val expectedModel = initModel.copy(
            isLoading = false,
            assignment = assignmentResult,
            rootSubmission = submissionResult,
            selectedSubmissionAttempt = submission.attempt
        )

        updateSpec
            .given(initModel)
            .whenEvent(SubmissionDetailsEvent.DataLoaded(assignmentResult, submissionResult, isArcEnabled))
            .then(
                assertThatNext(
                    hasModel(expectedModel),
                    matchesEffects<SubmissionDetailsModel, SubmissionDetailsEffect>(
                        SubmissionDetailsEffect.ShowSubmissionContentType(expectedContentType)
                    )
                )
            )
    }
    // endregion getSubmissionContentType

    // endregion DataLoaded
}
