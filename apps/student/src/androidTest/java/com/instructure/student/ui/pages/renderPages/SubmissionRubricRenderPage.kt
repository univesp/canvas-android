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
package com.instructure.student.ui.pages.renderPages

import com.instructure.espresso.OnViewWithId
import com.instructure.espresso.page.BasePage
import com.instructure.student.R

class SubmissionRubricRenderPage : BasePage(R.id.submissionRubricPage) {
    val emptyView by OnViewWithId(R.id.rubricEmptyView)
    val gradeView by OnViewWithId(R.id.rubricGradeView)

    val criterionDescription by OnViewWithId(R.id.criterionDescription)
    val selectedRatingDescription by OnViewWithId(R.id.selectedRatingDescription)
    val ratingLayout by OnViewWithId(R.id.ratingLayout)
    val commentContainer by OnViewWithId(R.id.commentContainer)
    val comment by OnViewWithId(R.id.comment)
    val longDescriptionButton by OnViewWithId(R.id.viewLongDescriptionButton)
    val bottomPadding by OnViewWithId(R.id.bottomPadding)

    val tooltip by OnViewWithId(R.id.tooltipTextView)
}
