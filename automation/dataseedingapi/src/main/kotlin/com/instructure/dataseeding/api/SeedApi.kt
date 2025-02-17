//
// Copyright (C) 2018-present Instructure, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.instructure.dataseeding.api

import com.instructure.dataseeding.model.CanvasUserApiModel
import com.instructure.dataseeding.model.CourseApiModel
import com.instructure.dataseeding.model.DiscussionApiModel
import com.instructure.dataseeding.model.EnrollmentApiModel
import com.instructure.dataseeding.model.FavoriteApiModel

// Data-seeding API
object SeedApi {

    // Seed data request
    data class SeedDataRequest (
            val teachers: Int = 0,
            val students: Int = 0,
            val courses: Int = 0,
            val pastCourses: Int = 0,
            val favoriteCourses: Int = 0,
            val gradingPeriods: Boolean = false,
            val discussions: Int = 0,
            val announcements: Int = 0
    )

    // Seed data object/model, made to look very much like the old proto-generated SeededData class
    class SeededDataApiModel {
        // lists of students, teachers, courses, etc...
        val teachersList = mutableListOf<CanvasUserApiModel>()
        val studentsList = mutableListOf<CanvasUserApiModel>()
        val coursesList = mutableListOf<CourseApiModel>()
        val enrollmentsList = mutableListOf<EnrollmentApiModel>()
        val favoriteCoursesList = mutableListOf<FavoriteApiModel>()
        val discussionsList = mutableListOf<DiscussionApiModel>()
        val announcementsList = mutableListOf<DiscussionApiModel>()

        // Methods to add elements to lists
        fun addTeachers(teacher: CanvasUserApiModel) {
            teachersList.add(teacher)
        }
        fun addStudents(student: CanvasUserApiModel) {
            studentsList.add(student)
        }
        fun addCourses(course: CourseApiModel) {
            coursesList.add(course)
        }
        fun addEnrollments(enrollment: EnrollmentApiModel) {
            enrollmentsList.add(enrollment)
        }
        fun addFavoriteCourses(favoriteCourse: FavoriteApiModel) {
            favoriteCoursesList.add(favoriteCourse)
        }
        fun addDiscussions(discussion: DiscussionApiModel) {
            discussionsList.add(discussion)
        }
        fun addAnnouncements(announcement: DiscussionApiModel) {
            announcementsList.add(announcement)
        }

        // Methods to import collections into our lists
        fun addAllFavorites(favorites: Iterable<FavoriteApiModel>) {
            favoriteCoursesList.addAll(favorites)
        }
        fun addAllDiscussions(discussions: Iterable<DiscussionApiModel>) {
            discussionsList.addAll(discussions)
        }
        fun addAllAnnouncements(announcements: Iterable<DiscussionApiModel>) {
            announcementsList.addAll(announcements)
        }


    }

    // Seed data from a SeedDataRequest, return SeededDataApiModel
    fun seedData(request: SeedDataRequest) : SeededDataApiModel {
        val seededData = SeededDataApiModel()

        with(seededData) {
            for (c in 0 until maxOf(request.courses + request.pastCourses, request.favoriteCourses)) {
                // Seed course
                addCourses(createCourse(request.gradingPeriods))

                // Seed users
                for (t in 0 until request.teachers) {
                    addTeachers(UserApi.createCanvasUser())
                    addEnrollments(EnrollmentsApi.enrollUserAsTeacher(coursesList[c].id, teachersList[t].id))
                }

                for (s in 0 until request.students) {
                    addStudents(UserApi.createCanvasUser())
                    addEnrollments(EnrollmentsApi.enrollUserAsStudent(coursesList[c].id, studentsList[s].id))
                }
            }

            // Make the last x courses concluded to keep the first ones as favorites
            for (c in coursesList.size - request.pastCourses until coursesList.size) {
                CoursesApi.concludeCourse(coursesList[c].id)
            }

            // Seed favorite courses
            addAllFavorites(
                    (0 until minOf(request.favoriteCourses, coursesList.size))
                            .map {
                                CoursesApi.addCourseToFavorites(coursesList[it].id,teachersList[0].token)
                            }
            )

            // Seed discussions
            addAllDiscussions(
                    (0 until request.discussions).map {
                        DiscussionTopicsApi.createDiscussion(coursesList[0].id, false, teachersList[0].token)
                    }
            )

            // Seed announcements
            addAllAnnouncements(
                    (0 until request.announcements).map {
                        DiscussionTopicsApi.createAnnouncement(coursesList[0].id, teachersList[0].token)
                    }
            )
        }

        return seededData
    }

    // Seed parent data request
    data class SeedParentDataRequest (
            val courses: Int = 0,
            val students: Int = 0,
            val parents: Int = 0
    )

    // Seeded parent data object/model, made to look very much like the old SeededParentData proto-generated model.
    class SeededParentDataApiModel {
        val coursesList = mutableListOf<CourseApiModel>()
        val studentsList = mutableListOf<CanvasUserApiModel>()
        val parentsList = mutableListOf<CanvasUserApiModel>()
        val enrollmentsList = mutableListOf<EnrollmentApiModel>()

        fun addCourses(course: CourseApiModel) {
            coursesList.add(course)
        }
        fun addStudents(student: CanvasUserApiModel) {
            studentsList.add(student)
        }
        fun addParents(parent: CanvasUserApiModel) {
            parentsList.add(parent)
        }
        fun addEnrollments(enrollment: EnrollmentApiModel) {
            enrollmentsList.add(enrollment)
        }
    }

    // Seed parent data from a SeedParentDataRequest, return a SeededParentDataApiModel
    fun seedParentData(request: SeedParentDataRequest ) : SeededParentDataApiModel {
        val seededData = SeededParentDataApiModel()

        with(seededData) {
            for (c in 0 until request.courses) {
                // Seed course
                addCourses(createCourse())

                // Seed users
                for (s in 0 until request.students) {
                    addStudents(UserApi.createCanvasUser())
                    addEnrollments(EnrollmentsApi.enrollUserAsStudent(coursesList[c].id, studentsList[s].id) )
                }

                // Seed parents
                for (t in 0 until request.parents) {
                    addParents(UserApi.createCanvasUser())
                    studentsList.forEach { student ->
                        addEnrollments(EnrollmentsApi.enrollUserAsObserver(coursesList[c].id, parentsList[t].id, student.id))
                    }
                }
            }

        }

        return seededData
    }

    // Private course-creation method that does some special handling for grading periods
    private fun createCourse(gradingPeriods: Boolean = false) : CourseApiModel {
        return if(gradingPeriods) {
            val enrollmentTerm = EnrollmentTermsApi.createEnrollmentTerm()
            val gradingPeriodSetWrapper = GradingPeriodsApi.createGradingPeriodSet(enrollmentTerm.id)
            val gradingPeriodSet = GradingPeriodsApi.createGradingPeriod(gradingPeriodSetWrapper.gradingPeriodSet.id)
            val courseWithTerm = CoursesApi.createCourse(enrollmentTerm.id)
            courseWithTerm
        }
        else {
            val course = CoursesApi.createCourse()
            course
        }
    }
}