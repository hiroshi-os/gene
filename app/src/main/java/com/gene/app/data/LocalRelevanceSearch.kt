package com.gene.app.data

import kotlin.math.sqrt

object LocalRelevanceSearch {
    private const val DIMENSIONS = 256
    private val stopWords = setOf("the", "and", "that", "this", "what", "about", "with", "from", "does", "they", "them", "have", "would", "could", "should", "will", "your", "you", "are", "was", "for", "how", "why", "when", "where", "into", "might")

    fun relevant(memories: List<Interaction>, question: String, limit: Int = 8): List<Interaction> {
        if (memories.size <= limit) return memories
        val questionVector = vector(question)
        return memories.mapIndexed { index, memory ->
            val recency = (memories.size - index).toDouble() / memories.size
            val score = cosine(questionVector, vector(memory.searchText())) + recency * 0.04
            memory to score
        }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    fun confidence(memories: List<Interaction>, question: String): Int {
        if (memories.isEmpty()) return 10
        val selected = relevant(memories, question, limit = minOf(8, memories.size))
        val similarity = if (selected.isEmpty()) 0.0 else selected.map { cosine(vector(question), vector(it.searchText())) }.maxOrNull() ?: 0.0
        val coverage = (selected.size.toDouble() / memories.size).coerceAtMost(1.0)
        return (18 + similarity.coerceIn(0.0, 1.0) * 62 + coverage * 20).toInt().coerceIn(12, 94)
    }

    private fun Interaction.searchText(): String = listOfNotNull(transcript, body.takeIf { it != "Audio recording" }).joinToString(" ").ifBlank { body }

    private fun vector(value: String): DoubleArray {
        val result = DoubleArray(DIMENSIONS)
        tokens(value).forEach { token ->
            result[(token.hashCode() and Int.MAX_VALUE) % DIMENSIONS] += 1.0
            if (token.length > 4) result[(token.drop(1).hashCode() and Int.MAX_VALUE) % DIMENSIONS] += 0.35
        }
        val magnitude = sqrt(result.sumOf { it * it }).coerceAtLeast(1.0)
        return DoubleArray(DIMENSIONS) { result[it] / magnitude }
    }

    private fun cosine(left: DoubleArray, right: DoubleArray): Double = left.indices.sumOf { left[it] * right[it] }.coerceIn(0.0, 1.0)
    private fun tokens(value: String): Set<String> = value.lowercase().split(Regex("[^a-z0-9]+"))
        .filter { it.length > 2 && it !in stopWords }.toSet()
}
