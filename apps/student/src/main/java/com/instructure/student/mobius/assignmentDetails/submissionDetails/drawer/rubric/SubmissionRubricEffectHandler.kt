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
package com.instructure.student.mobius.assignmentDetails.submissionDetails.drawer.rubric

import com.instructure.student.mobius.assignmentDetails.submissionDetails.drawer.rubric.ui.SubmissionRubricView
import com.instructure.student.mobius.common.ui.EffectHandler

class SubmissionRubricEffectHandler :
    EffectHandler<SubmissionRubricView, SubmissionRubricEvent, SubmissionRubricEffect>() {
    override fun accept(effect: SubmissionRubricEffect) {
        when (effect) {
            is SubmissionRubricEffect.ShowLongDescription -> {
                view?.displayLongDescription(effect.description, effect.longDescription)
            }
        }
    }
}
