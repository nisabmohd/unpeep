package app.keyhole.privacy.domain

/*
 * The room checklist (docs/PLAN.md § 2.6): what to look at by hand, per place. The words live in strings.xml
 * (screens map ids to them) so every language gets them; this file only says which items each place has, in order.
 */

/** A heading of the checklist. */
enum class ChecklistSection { MIRROR, DEVICES, ROOM, BED, BATH, DESK, CAR }

/** One thing to look at. */
enum class ChecklistItem {
    MIRROR_LIGHT, MIRROR_EDGES,
    SMOKE_DETECTOR, CLOCK, CHARGERS, USB, TV,
    LAMPS, FRAMES, VENTS, PLANTS, HOOKS, CORNERS, PARTITION,
    BED,
    SHOWER, SHELVES,
    DESK, OBJECTS,
    CAR_MIRROR, CAR_VENTS, CAR_PORTS, CAR_CEILING, CAR_SEATS,
}

/** How the user marked an item: it looks fine, they're not sure (photo and note), or they skipped it. */
enum class ChecklistAnswer { LOOKS_FINE, NOT_SURE, SKIPPED }

/** An answer with what came with it. A note and a photo only ever come with [ChecklistAnswer.NOT_SURE]. */
data class ChecklistEntry(val answer: ChecklistAnswer, val note: String = "", val photoPath: String? = null)

/** The checklist of [place]: its sections in order, each with its items. */
fun checklist(place: Place): List<Pair<ChecklistSection, List<ChecklistItem>>> = when (place) {
    Place.STAY -> listOf(
        ChecklistSection.MIRROR to listOf(ChecklistItem.MIRROR_LIGHT, ChecklistItem.MIRROR_EDGES),
        ChecklistSection.DEVICES to listOf(ChecklistItem.SMOKE_DETECTOR, ChecklistItem.CLOCK, ChecklistItem.CHARGERS, ChecklistItem.TV),
        // Coat hooks and "gifts" are common hidden-camera forms in rented rooms (reviewed 16 Nov 2026).
        ChecklistSection.ROOM to listOf(ChecklistItem.LAMPS, ChecklistItem.FRAMES, ChecklistItem.VENTS, ChecklistItem.PLANTS, ChecklistItem.HOOKS, ChecklistItem.OBJECTS),
        ChecklistSection.BED to listOf(ChecklistItem.BED),
        ChecklistSection.BATH to listOf(ChecklistItem.SHOWER, ChecklistItem.SHELVES),
    )
    Place.TRIAL_ROOM -> listOf(
        ChecklistSection.MIRROR to listOf(ChecklistItem.MIRROR_LIGHT, ChecklistItem.MIRROR_EDGES),
        ChecklistSection.ROOM to listOf(ChecklistItem.LAMPS, ChecklistItem.HOOKS, ChecklistItem.CORNERS, ChecklistItem.PARTITION, ChecklistItem.SMOKE_DETECTOR),
    )
    Place.WASHROOM -> listOf(
        ChecklistSection.MIRROR to listOf(ChecklistItem.MIRROR_LIGHT, ChecklistItem.MIRROR_EDGES),
        ChecklistSection.BATH to listOf(ChecklistItem.SHOWER, ChecklistItem.SHELVES),
        ChecklistSection.ROOM to listOf(ChecklistItem.LAMPS, ChecklistItem.VENTS, ChecklistItem.SMOKE_DETECTOR, ChecklistItem.HOOKS),
    )
    Place.OFFICE -> listOf(
        ChecklistSection.DEVICES to listOf(ChecklistItem.SMOKE_DETECTOR, ChecklistItem.CLOCK, ChecklistItem.CHARGERS, ChecklistItem.USB),
        ChecklistSection.DESK to listOf(ChecklistItem.DESK, ChecklistItem.OBJECTS),
        ChecklistSection.ROOM to listOf(ChecklistItem.FRAMES, ChecklistItem.LAMPS, ChecklistItem.VENTS, ChecklistItem.PLANTS),
    )
    Place.CAR -> listOf(
        ChecklistSection.CAR to listOf(ChecklistItem.CAR_MIRROR, ChecklistItem.CAR_VENTS, ChecklistItem.CAR_PORTS, ChecklistItem.CAR_CEILING, ChecklistItem.CAR_SEATS),
    )
}

/** Every item of [place], in order. */
fun checklistItems(place: Place): List<ChecklistItem> = checklist(place).flatMap { it.second }

/** How many of [place]'s items have an answer (a skip counts: the user looked at the list and moved on). */
fun checklistDone(place: Place, answers: Map<ChecklistItem, ChecklistEntry>): Int = checklistItems(place).count { it in answers }
