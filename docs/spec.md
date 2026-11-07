# Anton specification

Anton is a native Android app that trains German police officers and trainees to spell correctly over the radio. It runs fully offline and is used solo. What matters is accuracy: there are no timers, speed scores or leaderboards.

## Domain vocabulary

Code uses the English term. The German term appears in the UI.

| Code | UI | Meaning |
|---|---|---|
| `SpellingTable` | Buchstabiertafel | The mapping from letters to spelling words |
| `Case` | Vorgang | One task, either an ID card or a licence plate |
| `Field` | Feld | One part of a case that is transmitted on its own |
| `Transmission` | Funkspruch | Everything said while the talk key is held |
| `Rank` | Dienstgrad | The player's permanent progress level |
| `Streak` | Serie | Consecutive error-free cases |
| `Mastery` | Sicherheit | How reliably a letter is spelled, from 0 to 5 |

## Spelling table

The classic German table, in this exact form:

A Anton, Ä Ärger, B Berta, C Cäsar, Ch Charlotte, D Dora, E Emil, F Friedrich, G Gustav, H Heinrich, I Ida, J Julius, K Kaufmann, L Ludwig, M Martha, N Nordpol, O Otto, Ö Ökonom, P Paula, Q Quelle, R Richard, S Samuel, Sch Schule, T Theodor, U Ulrich, Ü Übermut, V Viktor, W Wilhelm, X Xanthippe, Y Ypsilon, Z Zacharias, ß Eszett.

- Ch and Sch may be spelled as one word or letter by letter.
- A hyphen in a name is spoken as "Bindestrich".
- Tables are data, so further alphabets can be added without code changes. Only this table ships at first.

## Cases

**ID card.** Fields in fixed order: surname, date of birth, first name. The card looks like a realistic German ID card and carries only fictional data.

**Licence plate.** One field. Letters are spelled, digits are spoken one by one and the hyphen is silent. District codes are real.

**Digits** are spoken one by one: null, eins, zwo, drei, vier, fünf, sechs, sieben, acht, neun. "Zwei" is an error. A date of birth such as 03.12.1987 is spoken as "null drei eins zwo eins neun acht sieben". Spoken date forms may be widened once police training confirms the standard.

**Difficulty** comes from the material:

1. Short German names, simple plates
2. Traps such as Meier and Mayer, umlauts, ß, double names
3. International names
4. Long names, rare letters such as Q, X and Y

The app chooses the next case itself, mixing ID cards and plates and favouring letters with low mastery.

## Speaking and evaluation

- The on-screen talk button and both volume keys are push-to-talk while a case is open.
- Each field is one transmission.
- A name may be said normally before it is spelled. Everything outside the recognizer vocabulary is ignored. If a spoken name is itself a spelling word, it is dropped when the rest of the transmission matches the field.
- The transmission is evaluated on release.

| Result | Reaction | Streak |
|---|---|---|
| Correct | Field is confirmed and the next one opens | Kept |
| Wrong word | Plausibility dialog shows the position, what was sent and what was expected. The field is repeated | Reset |
| Not understood | Asks to repeat | Kept |
| Player taps "Das war richtig" | The field counts as correct and the misrecognition is recorded locally | Restored |
| Spelling table opened during the case | The case can be finished but does not count as error-free | Reset |

## Progress

- A case is error-free when every field was correct at the first attempt.
- Error-free cases fill the progress bar to the next rank. Ranks never drop. Reaching a rank shows an appointment certificate and unlocks harder material.
- Mastery per letter updates after every field and is visible in the spelling table.

Ranks follow the Berlin Schutzpolizei insignia (Polizei Berlin, as of July 2026). Shoulder boards are navy.

| Rank | Insignia |
|---|---|
| In Ausbildung | Blue stripe |
| Polizeimeister/in | 2 blue stars |
| Polizeiobermeister/in | 3 blue stars |
| Polizeikommissar-Anwärter/in | Silver stripe |
| Polizeikommissar/in | 1 silver star |
| Polizeioberkommissar/in | 2 silver stars |
| Polizeihauptkommissar/in | 3 silver stars |
| Polizeihauptkommissar/in A12 | 4 silver stars |
| Erste/r Polizeihauptkommissar/in | 5 silver stars |
| Erste/r Polizeihauptkommissar/in mit Amtszulage | 6 silver stars |
| Polizeireferendar/in | Gold stripe |
| Polizeirat/rätin | 1 gold star |
| Polizeioberrat/rätin | 2 gold stars |
| Polizeidirektor/in | 3 gold stars |
| Leitende/r Polizeidirektor/in | 4 gold stars |
| Direktor/in bei der Polizei Berlin | Wreath, 1 gold star |
| Erste/r Direktor/in bei der Polizei Berlin | Wreath, 2 gold stars |
| Polizeivizepräsident/in | Wreath, 3 gold stars |
| Polizeipräsident/in | Wreath, 4 gold stars |

## Screens

The look is deliberately that of classic Windows government software: grey windows, navy title bars, bevelled buttons, sunken fields, block progress bars and a status bar.

- **First start:** microphone permission.
- **Wache:** the player's personnel file with rank, progress and streak. Actions: next case, spelling table, settings.
- **Vorgang:** the ID card or plate, the active field, a radio log with one cell per letter and the talk button.
- **Plausibility dialog:** feedback for a wrong word, part of Vorgang.
- **Certificate:** appointment on reaching a rank.
- **Buchstabiertafel:** table with mastery per letter. Tapping a row starts a short drill of that letter.
- **Settings:** volume keys as push-to-talk, radio sounds, reset progress.

## Architecture

- Kotlin, Jetpack Compose, Room for progress, Vosk with `vosk-model-small-de-0.15` bundled in the APK.
- The app needs the microphone permission and no internet permission.
- Deep modules behind small interfaces:
  - `SpeechRecognizer`: turns a push-to-talk transmission into recognized words and hides Vosk.
  - `SpellingEvaluator`: compares recognized words with a field. Pure Kotlin.
  - `CaseGenerator`: produces the next case from rank and mastery. Pure Kotlin.
  - `ProgressStore`: persists rank, streak and mastery and hides Room.

### Recognizer vocabulary

Vosk runs with a fixed grammar of spelling words, digits and `[unk]`. "Xanthippe" and "Eszett" are not in the model vocabulary, so they use phrases of known words that are mapped back to the letter. The exact phrases are confirmed by testing on a real device.

## Release plan

1. **Recognition prototype:** push-to-talk with the fixed grammar on a real device, measured with real speech. The result decides whether the bundled model stays.
2. **Core loop:** spelling table, evaluation and ID cards without progress.
3. **Progress:** ranks, streak, mastery, certificate.
4. **Licence plates, name lists, settings.**
5. **Polish:** realistic ID card, exact insignia, sounds, animation and detail work.

Spoken output by the app is out of scope for the first release. A later version may add a spoken control room and a reverse mode in which the player writes down what is spelled.

Distribution starts with an APK on GitHub.
