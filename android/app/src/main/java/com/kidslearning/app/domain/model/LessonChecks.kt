package com.kidslearning.app.domain.model

/**
 * On-device semantic validation for personal-use mode -- the critical subset of
 * the backend's deterministic validator, so a lesson generated directly on the
 * phone gets the same mechanical safety net before the parent previews it.
 */
object LessonChecks {

    fun validate(lesson: Lesson, imageCount: Int): List<String> {
        val errors = mutableListOf<String>()
        val conceptIds = lesson.concepts.map { it.conceptId }.toSet()

        val sectionIds = lesson.sections.map { it.id }
        if (sectionIds.size != sectionIds.toSet().size) {
            errors += "Duplicate section ids."
        }

        val seenQuestions = mutableSetOf<String>()
        var activityCount = 0

        lesson.sections.forEach { section ->
            val sid = section.id

            section.conceptId?.let {
                if (it !in conceptIds) errors += "[$sid] unknown concept_id '$it'."
            }
            section.imageRef?.let {
                if (it !in 0 until imageCount) {
                    errors += "[$sid] image_ref $it but only $imageCount image(s) attached."
                }
            }

            when (section) {
                is MultipleChoiceSection -> {
                    activityCount++
                    val optionIds = section.options.map { it.id }
                    if (optionIds.size != optionIds.toSet().size) errors += "[$sid] duplicate option ids."
                    section.correctAnswerIds.forEach {
                        if (it !in optionIds) errors += "[$sid] correct answer '$it' is not an option."
                    }
                    if (section.selectionMode == "single" && section.correctAnswerIds.size != 1) {
                        errors += "[$sid] single mode needs exactly one correct answer."
                    }
                    if (section.correctAnswerIds.isEmpty()) errors += "[$sid] no correct answer."
                }
                is FillInTheBlankSection -> {
                    activityCount++
                    val markers = Regex("\\{\\{([a-z0-9][a-z0-9_-]*)\\}\\}")
                        .findAll(section.template).map { it.groupValues[1] }.toSet()
                    val blankIds = section.blanks.map { it.id }.toSet()
                    if (markers != blankIds) errors += "[$sid] template blanks do not match blank list."
                    section.wordBank?.let { bank ->
                        val bankLower = bank.map { it.lowercase() }.toSet()
                        section.blanks.forEach { blank ->
                            if (blank.acceptedAnswers.none { it.lowercase() in bankLower }) {
                                errors += "[$sid] no accepted answer present in the word bank."
                            }
                        }
                    }
                }
                is MatchPairsSection -> {
                    activityCount++
                    val leftTexts = section.pairs.map { it.left.text.trim().lowercase() }
                    val rightTexts = section.pairs.map { it.right.text.trim().lowercase() }
                    if (leftTexts.size != leftTexts.toSet().size ||
                        rightTexts.size != rightTexts.toSet().size
                    ) errors += "[$sid] ambiguous duplicate pair text."
                }
                is DragIntoOrderSection -> {
                    activityCount++
                    if (section.correctOrder.sorted() != section.items.map { it.id }.sorted()) {
                        errors += "[$sid] correct_order is not a permutation of the items."
                    }
                }
                is TrueFalseSection -> activityCount++
                is ExplanationSection -> Unit
            }

            val questionText = when (section) {
                is MultipleChoiceSection -> section.question
                is TrueFalseSection -> section.statement
                is FillInTheBlankSection -> section.template
                is MatchPairsSection -> section.instruction
                is DragIntoOrderSection -> section.instruction
                is ExplanationSection -> null
            }?.lowercase()?.replace(Regex("[^a-z0-9 ]"), "")?.trim()
            if (!questionText.isNullOrBlank() && !seenQuestions.add(questionText)) {
                errors += "[$sid] duplicate question."
            }
        }

        if (activityCount == 0) errors += "Lesson has no interactive activities."
        return errors
    }
}
