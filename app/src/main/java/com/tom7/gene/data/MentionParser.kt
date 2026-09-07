package com.tom7.gene.data

/**
 * Resolve `@Name` tokens against session members.
 * Longest name match wins so `@Alexa` does not steal `@Alex`.
 */
object MentionParser {
    fun mentionedPeople(text: String, members: List<Person>): List<Person> {
        if (text.isBlank() || members.isEmpty()) return emptyList()
        val sorted = members
            .filter { !it.isSelf && it.name.isNotBlank() }
            .sortedByDescending { it.name.length }
        if (sorted.isEmpty()) return emptyList()
        val lower = text.lowercase()
        val hit = linkedSetOf<Long>()
        val result = mutableListOf<Person>()
        for (person in sorted) {
            val needle = "@${person.name.trim()}".lowercase()
            var from = 0
            while (true) {
                val at = lower.indexOf(needle, from)
                if (at < 0) break
                val end = at + needle.length
                val beforeOk = at == 0 || !lower[at - 1].isLetterOrDigit()
                val afterOk = end >= lower.length || !lower[end].isLetterOrDigit()
                if (beforeOk && afterOk && hit.add(person.id)) {
                    result.add(person)
                    break
                }
                from = at + 1
            }
        }
        return result
    }

    /** Active @query after the last @ without a space after it. */
    fun activeMentionQuery(text: String): String? {
        val at = text.lastIndexOf('@')
        if (at < 0) return null
        if (at > 0 && text[at - 1].isLetterOrDigit()) return null
        val rest = text.substring(at + 1)
        if (rest.any { it.isWhitespace() }) return null
        return rest
    }

    fun suggestions(query: String, members: List<Person>): List<Person> {
        val q = query.trim()
        return members
            .filter { !it.isSelf }
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }
            .take(8)
    }

    fun applySuggestion(text: String, person: Person): String {
        val at = text.lastIndexOf('@')
        if (at < 0) return text.trimEnd() + " @${person.name} "
        return text.substring(0, at) + "@${person.name} "
    }
}
