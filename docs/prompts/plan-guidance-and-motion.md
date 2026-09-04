# /plan — Evidence-Backed Guidance + Motion Pass

Paste everything below the line into Claude Code from the repo root.
Produce a plan. Do not implement until the plan is approved.

---

<task>
Plan two additions to the Recovery Coach Android app (Kotlin / Jetpack Compose):

1. **A guidance layer** that reads the user's accumulated activity + check-in history,
   cross-references it against published physical-activity guidance from reputable
   medical bodies, and surfaces a small number of specific, useful tips.
2. **A motion + iconography pass** that makes the app feel alive — screen
   transitions, state animations, and icons — without redesigning any screen.

Output an implementation plan only. Write no production code in this turn.
</task>

<non_goals>
These are out of scope. Do not touch them, and do not propose them as "while we're here" additions.

- **No unit changes.** km, metres, steps, minutes and heart points stay exactly as they are.
  No imperial toggle, no conversion helpers, no unit-preference setting. Leave the
  "Units, reminder times and Health Connect sync are planned for later" string in
  `SettingsScreen.kt:46` untouched.
- No visual redesign. Existing layouts, spacing and screen structure stay put.
- No new colour or type values. Everything comes from `RecoveryColors` and `RecoveryType`.
- No Health Connect / Google Fit integration, no new network dependency at runtime.
- No changes to auth, build config, or the DataStore key names already in `RecoveryStore`.

Exception to the repo's usual "don't touch navigation" rule: adding
`enterTransition`/`exitTransition` to the existing `NavHost` **is** in scope, because
the user asked for transitions. Route graph shape, destinations and argument
contracts stay unchanged.
</non_goals>

<blocking_question>
Resolve this with me BEFORE designing the guidance layer. Do not pick for me.

The Week screen's history is fake. `RecoveryViewModel.kt:151` is commented
"placeholder history until real logging accumulates", and lines 170-173 are
hardcoded `WeekDayRecord` literals. `RecoveryStore`'s own doc comment states the
Week trend numbers are placeholder reference data and are never persisted.

Cross-verifying hardcoded demo rows against WHO or ADA guidance produces confident
nonsense. So ask me which path to take:

- **(a) Guidance on real data only.** Drive tips from `ActivityLog` plus the persisted
  check-in values. Keep the Week placeholders as demo content and label them clearly
  as sample data in the UI. Ships sooner; tips are thin until a few days accumulate.
- **(b) Accumulate real history first.** Persist a dated daily snapshot on each
  check-in/log, build the Week screen from stored snapshots, then run guidance over
  the real series. More work; the guidance is actually meaningful.

State the tradeoff in two or three lines and ask. Then plan against my answer.
</blocking_question>

<research_protocol>
Research before proposing any threshold, target, or tip. Do not write recommendations
from memory — this is health content and your training data may be stale.

**Step 1 — Investigate.** Use `/investigate` to gather current published guidance on
physical activity for adults managing type 2 diabetes. Search first; let the search
determine which topics matter, rather than assuming.

**Step 2 — Rank sources.** Prefer, in this order:
1. Standards-setting clinical bodies (American Diabetes Association *Standards of Care*,
   WHO physical activity guidelines, ACSM position stands, Diabetes UK, national
   physical-activity guidelines).
2. Systematic reviews and meta-analyses in indexed journals.
3. Major clinical centres and public health agencies (NHS, CDC, Mayo Clinic).

Do not stop at the three bodies I named — find what the field actually converges on.
Do not cite blogs, supplement vendors, content farms, or fitness influencers.

**Step 3 — Cite everything numeric.** Every number that reaches the user — a weekly
minute target, a sedentary-break interval, a session-length floor — carries
`body · document · year` in the plan. A number without a citation does not go in.

**Step 4 — Report disagreement.** Where reputable sources diverge, say so and propose
the conservative option rather than silently averaging them.

**Step 5 — Library docs.** Use Context7 for current Compose animation and
`androidx.navigation.compose` transition APIs before specifying them. These APIs have
moved; do not write them from memory.
</research_protocol>

<medical_scoping>
Non-negotiable, because this app gives health guidance to someone managing a condition.

- General educational guidance only, sourced and dated. Never an individualized
  prescription.
- Never medication, insulin, or dosing content of any kind — not even indirectly
  ("adjust before your dose").
- Where a tip touches a clinical threshold (hypoglycemia, foot care, neuropathy,
  cardiac symptoms), the app points to a clinician rather than resolving it.
- One quiet, permanent disclaimer surface — not a banner repeated on every card.
</medical_scoping>

<interactivity_scope>
The user asked for "native animations, transition bursts, or icons." Plan these
concretely; a plan that says "add animations" is not a plan.

**Navigation transitions** — `enterTransition` / `exitTransition` on the existing
`NavHost` composables. Directional slide + fade between the four bottom-nav
destinations.

**State animations** — name the API and the exact call site for each:
- `animateFloatAsState` / `animateColorAsState` for the load bars and the recovery-level
  card when `RecoveryLevel` changes.
- `AnimatedVisibility` for the Log Activity sheet and any expanding detail rows.
- `Crossfade` where a card swaps content wholesale.
- `animateContentSize` where a card grows as a tip appears.
- A check-off animation on `PlanItem` completion in `TodayScreen`.

**Icons** — `androidx.compose.material.icons`. Bottom nav, plan-item categories
(walk / swim / steps), and tip severity. No custom vector assets unless a needed
glyph genuinely has no Material equivalent.

**Constraints on all of the above:**
- Colours and durations resolve through `RecoveryColors` / `RecoveryType` and a single
  new motion-token object. No magic hex or scattered literals.
- Respect the system reduce-motion setting; degrade to instant state changes.
- Nothing blocks input. No animation longer than ~300ms on a navigation path.
- The app must remain fully usable with every animation stripped out.
</interactivity_scope>

<plan_format>
Return the plan in this shape:

1. **Open with the blocking question.** Nothing else until I answer it.
2. **Research findings** — the guidance you found, each with `body · document · year`,
   and a note on where sources disagree.
3. **Guidance layer design** — the data shape, where the rules live, how a tip is
   selected and ranked, and where tips surface in the UI. Include the tip catalogue
   with each tip mapped to its source.
4. **Motion + icon changes** — a file-by-file table: `file · what changes · which API`.
5. **Sequencing** — ordered steps, each independently shippable and verifiable.
6. **Risks and open questions.**

Keep it tight. Tables and short bullets over paragraphs.
</plan_format>

<verification>
There is no `scripts/preflight.sh` in this repo. The plan must name the real checks:

- `./gradlew assembleDebug` must pass.
- `./gradlew lint` for the Compose and accessibility warnings.
- Unit tests for the tip-selection logic — it is pure decision logic over a data series
  and is fully testable without a device.
- Manual check on the golden path: launch, log activity, complete a check-in, see the
  recovery level and any tips update.

State plainly what you ran and what you did not. Do not claim a check passed that you
did not execute.
</verification>

<self_check_before_responding>
Before returning the plan, confirm each of these:

- Did I ask the placeholder-data question and stop, rather than choosing for the user?
- Does every number in the plan carry a named source and a year?
- Did I search rather than recall the guidance figures?
- Did I avoid every unit change, including the Settings string?
- Is each animation named to a specific API at a specific call site?
- Does every new colour and duration resolve to a token, not a literal?
- Is there any medication, dosing, or diagnostic content? Remove it.
- Did I write production code when I was asked only to plan? Remove it.
</self_check_before_responding>
