package app.keyhole.privacy.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.Check
import app.keyhole.privacy.domain.Place
import app.keyhole.privacy.ui.theme.Hues

/** How each check looks: its hue (on its icon circle only), Solar icon and short label (docs/ICONS.md). */
val Check.hue: Float
    get() = when (this) {
        Check.WIFI -> Hues.WIFI
        Check.HOTSPOTS -> Hues.HOTSPOTS
        Check.BLUETOOTH -> Hues.BLUETOOTH
        Check.LENS -> Hues.LENS
        Check.INFRARED -> Hues.INFRARED
        Check.MAGNET -> Hues.MAGNET
        Check.CHECKLIST -> Hues.CHECKLIST
    }

val Check.icon: Int
    @DrawableRes get() = when (this) {
        Check.WIFI -> R.drawable.ic_wifi
        Check.HOTSPOTS -> R.drawable.ic_hotspot
        Check.BLUETOOTH -> R.drawable.ic_bluetooth
        Check.LENS -> R.drawable.ic_lens
        Check.INFRARED -> R.drawable.ic_infrared
        Check.MAGNET -> R.drawable.ic_magnet
        Check.CHECKLIST -> R.drawable.ic_checklist
    }

val Check.label: Int
    @StringRes get() = when (this) {
        Check.WIFI -> R.string.check_wifi
        Check.HOTSPOTS -> R.string.check_hotspots
        Check.BLUETOOTH -> R.string.check_bluetooth
        Check.LENS -> R.string.check_lens
        Check.INFRARED -> R.string.check_infrared
        Check.MAGNET -> R.string.check_magnet
        Check.CHECKLIST -> R.string.check_checklist
    }

val Place.label: Int
    @StringRes get() = when (this) {
        Place.STAY -> R.string.place_stay
        Place.TRIAL_ROOM -> R.string.place_trial_room
        Place.WASHROOM -> R.string.place_washroom
        Place.OFFICE -> R.string.place_office
        Place.CAR -> R.string.place_car
    }

val Place.icon: Int
    @DrawableRes get() = when (this) {
        Place.STAY -> R.drawable.ic_place_stay
        Place.TRIAL_ROOM -> R.drawable.ic_place_trial_room
        Place.WASHROOM -> R.drawable.ic_place_washroom
        Place.OFFICE -> R.drawable.ic_place_office
        Place.CAR -> R.drawable.ic_place_car
    }

/** "7 checks for a stay": one sentence per place, so every language can order it its own way. */
val Place.checksPlural: Int
    @androidx.annotation.PluralsRes get() = when (this) {
        Place.STAY -> R.plurals.checks_for_stay
        Place.TRIAL_ROOM -> R.plurals.checks_for_trial_room
        Place.WASHROOM -> R.plurals.checks_for_washroom
        Place.OFFICE -> R.plurals.checks_for_office
        Place.CAR -> R.plurals.checks_for_car
    }

/** "Next: hotspots": one sentence per step, so every language can word it its own way. */
val Check.next: Int
    @StringRes get() = when (this) {
        Check.WIFI -> R.string.next_wifi
        Check.HOTSPOTS -> R.string.next_hotspots
        Check.BLUETOOTH -> R.string.next_bluetooth
        Check.LENS -> R.string.next_lens
        Check.INFRARED -> R.string.next_infrared
        Check.MAGNET -> R.string.next_magnet
        Check.CHECKLIST -> R.string.next_checklist
    }

/** "Skip to lens finder": a sweep jumping to a step that needs no signal (E09). */
val Check.skipTo: Int
    @StringRes get() = when (this) {
        Check.INFRARED -> R.string.skip_to_infrared
        Check.MAGNET -> R.string.skip_to_magnet
        Check.CHECKLIST -> R.string.skip_to_checklist
        else -> R.string.skip_to_lens
    }
