package run.moritz.anton.session

import run.moritz.anton.cases.Field
import run.moritz.anton.progress.ProgressEvent
import run.moritz.anton.spelling.Evaluation
import run.moritz.anton.spelling.SpellingEvaluator

/** The result of [CaseSession.advance]: how many symbols were used up and what that changed. */
class Advance(val consumed: Int, val events: List<ProgressEvent>)

/**
 * One case being worked on, field by field.
 *
 * A wrong word stays [pending] until the player decides in the plausibility dialog, so a misrecognition that is
 * overruled never touches the streak or mastery.
 */
class CaseSession(
    val fields: List<Field>,
    private val countsTowardProgress: Boolean,
    private val evaluator: SpellingEvaluator
) {
    var fieldIndex = 0
        private set
    var pending: Evaluation.Wrong? = null
        private set
    var notUnderstood = false
        private set
    var finished = false
        private set
    var errorFree = true
        private set
    private var tableOpened = false
    private var pendingHeard = ""

    val currentField: Field? get() = fields.getOrNull(fieldIndex)

    /** Evaluates a released transmission and returns what it changed. */
    fun transmit(symbols: List<String>, heard: String): List<ProgressEvent> {
        val field = checkNotNull(currentField) { "The case is already finished" }
        check(pending == null) { "The plausibility dialog is still open" }
        notUnderstood = false
        return when (val evaluation = evaluator.evaluate(field, symbols)) {
            is Evaluation.Correct -> confirm(evaluation.confirmed)

            is Evaluation.Wrong -> {
                pending = evaluation
                pendingHeard = heard
                emptyList()
            }

            Evaluation.NotUnderstood -> {
                notUnderstood = true
                emptyList()
            }
        }
    }

    /**
     * Confirms every field that [symbols] complete from the start, while the talk key is still held. Whatever
     * stays unfinished is left for later, so an incomplete field neither advances nor counts as not understood.
     */
    fun advance(symbols: List<String>): Advance {
        check(pending == null) { "The plausibility dialog is still open" }
        var consumed = 0
        val events = mutableListOf<ProgressEvent>()
        while (true) {
            val field = currentField ?: break
            val rest = symbols.drop(consumed)
            val count = evaluator.symbolsCompleting(field, rest) ?: break
            val correct = evaluator.evaluate(field, rest.take(count)) as Evaluation.Correct
            events += confirm(correct.confirmed)
            consumed += count
        }
        return Advance(consumed, events)
    }

    /** The player accepts the error. The field is repeated. */
    fun repeat(): List<ProgressEvent> {
        val wrong = checkNotNull(pending) { "No plausibility dialog is open" }
        pending = null
        errorFree = false
        return buildList {
            wrong.expectedSymbol?.let { add(ProgressEvent.Missed(it)) }
            if (countsTowardProgress) add(ProgressEvent.StreakBroken)
        }
    }

    /** The player says the transmission was right. The field counts as correct. */
    fun overrule(): List<ProgressEvent> {
        val wrong = checkNotNull(pending) { "No plausibility dialog is open" }
        val field = checkNotNull(currentField)
        pending = null
        val confirmed = confirm(evaluator.symbolsOf(field))
        return buildList {
            add(confirmed.first())
            add(ProgressEvent.Misrecognized(wrong.expectedWord, pendingHeard))
            addAll(confirmed.drop(1))
        }
    }

    /** Looking up the table is allowed, but the case no longer counts as error-free. */
    fun openTable(): List<ProgressEvent> {
        if (!countsTowardProgress || tableOpened) return emptyList()
        tableOpened = true
        errorFree = false
        return listOf(ProgressEvent.StreakBroken)
    }

    /** True until the player has opened the table in this case, so the screen can warn once. */
    val tableStillFree: Boolean get() = countsTowardProgress && !tableOpened

    private fun confirm(symbols: List<String>): List<ProgressEvent> {
        notUnderstood = false
        fieldIndex += 1
        finished = fieldIndex == fields.size
        return buildList {
            add(ProgressEvent.Confirmed(symbols))
            if (finished && countsTowardProgress) add(ProgressEvent.CaseFinished(errorFree))
        }
    }
}
