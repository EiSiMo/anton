package run.moritz.anton.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import run.moritz.anton.spelling.SpellingTable

class RecognitionVocabularyTest {
    private val vocabulary = RecognitionVocabulary(SpellingTable.classicGerman)

    @Test
    fun `decodes spelling words into symbols and ignores unknown speech`() {
        assertEquals(listOf("A", "B"), vocabulary.decode("[unk] anton [unk] berta"))
    }

    @Test
    fun `decodes phrases that stand in for words missing from the model`() {
        assertEquals(listOf("A", "X", "ß"), vocabulary.decode("anton sankt tippe es zett"))
    }

    @Test
    fun `treats zwo as a digit and zwei as a distinct wrong word`() {
        assertEquals(listOf("2", "zwei"), vocabulary.decode("zwo zwei"))
    }

    @Test
    fun `hears a name instead of forcing it onto a spelling word`() {
        assertEquals(listOf("A", "Siegfried"), vocabulary.decode("anton siegfried"))
        assertTrue("siegfried" in vocabulary.grammar)
    }

    @Test
    fun `no wrong word hides a spelling word or digit`() {
        val correct = SpellingTable.classicGerman.entries.map { it.word.lowercase() }
        assertEquals(emptyList<String>(), WRONG_WORDS.map { it.lowercase() }.filter { it in correct })
    }

    @Test
    fun `grammar offers only known model phrases and unknown speech`() {
        assertTrue("[unk]" in vocabulary.grammar)
        assertTrue("zwo" in vocabulary.grammar)
        assertTrue("xanthippe" !in vocabulary.grammar)
    }
}
