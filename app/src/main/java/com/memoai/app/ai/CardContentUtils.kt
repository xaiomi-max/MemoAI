package com.memoai.app.ai

object CardContentUtils {
    private val taskStepPrefix = Regex("""^\[?\s*步骤\s*\d+\s*\]?\s*[：:.、]?\s*""", RegexOption.IGNORE_CASE)
    private val ideaPrefix = Regex(
        """^\[?\s*AI\s*脑风暴\s*\]?\s*\d*\)?\s*[：:.、]?\s*""",
        RegexOption.IGNORE_CASE
    )
    private val numberedPrefix = Regex("""^\d+[\).、]\s*""")

    fun parseTaskSteps(content: String): List<String> {
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line -> line.replace(taskStepPrefix, "").trim() }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun parseIdeaItems(content: String): List<String> {
        return content.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                line.replace(ideaPrefix, "")
                    .replace(numberedPrefix, "")
                    .trim()
            }
            .filter { it.isNotEmpty() }
            .toList()
    }

    fun stepHighlightKey(index: Int): String = "step:$index"

    fun isStepCompleted(highlights: List<String>, index: Int): Boolean =
        highlights.contains(stepHighlightKey(index))

    fun formatTaskContentForDisplay(content: String): String {
        val steps = parseTaskSteps(content)
        if (steps.isEmpty()) return content.trim()
        return steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n")
    }

    fun formatIdeaContentForDisplay(content: String): String {
        val ideas = parseIdeaItems(content)
        if (ideas.isEmpty()) return content.trim()
        return buildString {
            append("AI脑风暴")
            ideas.forEach { idea ->
                append('\n')
                append(idea)
            }
        }
    }
}
