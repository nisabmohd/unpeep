package app.keyhole.privacy.ui.checklist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.keyhole.privacy.R
import app.keyhole.privacy.domain.ChecklistItem
import app.keyhole.privacy.domain.ChecklistSection
import app.keyhole.privacy.ui.theme.Hues

/** How a checklist item looks: its words, its Solar icon and the hue of its circle. */
data class ItemLook(@StringRes val title: Int, @StringRes val line: Int, @DrawableRes val icon: Int, val hue: Float)

val ChecklistItem.look: ItemLook
    get() = when (this) {
        ChecklistItem.MIRROR_LIGHT -> ItemLook(R.string.item_mirror_light, R.string.item_mirror_light_line, R.drawable.ic_lens, Hues.LENS)
        ChecklistItem.MIRROR_EDGES -> ItemLook(R.string.item_mirror_edges, R.string.item_mirror_edges_line, R.drawable.ic_mirror, Hues.CHECKLIST)
        ChecklistItem.SMOKE_DETECTOR -> ItemLook(R.string.item_smoke_detector, R.string.item_smoke_detector_line, R.drawable.ic_fire, Hues.INFRARED)
        ChecklistItem.CLOCK -> ItemLook(R.string.item_clock, R.string.item_clock_line, R.drawable.ic_clock, Hues.BLUETOOTH)
        ChecklistItem.CHARGERS -> ItemLook(R.string.item_chargers, R.string.item_chargers_line, R.drawable.ic_socket, Hues.HOTSPOTS)
        ChecklistItem.USB -> ItemLook(R.string.item_usb, R.string.item_usb_line, R.drawable.ic_usb, Hues.LENS)
        ChecklistItem.TV -> ItemLook(R.string.item_tv, R.string.item_tv_line, R.drawable.ic_tv, Hues.WIFI)
        ChecklistItem.LAMPS -> ItemLook(R.string.item_lamps, R.string.item_lamps_line, R.drawable.ic_lamp, Hues.MAGNET)
        ChecklistItem.FRAMES -> ItemLook(R.string.item_frames, R.string.item_frames_line, R.drawable.ic_frame, Hues.MAGNET)
        ChecklistItem.VENTS -> ItemLook(R.string.item_vents, R.string.item_vents_line, R.drawable.ic_wind, Hues.CHECKLIST)
        ChecklistItem.PLANTS -> ItemLook(R.string.item_plants, R.string.item_plants_line, R.drawable.ic_leaf, Hues.HOTSPOTS)
        ChecklistItem.HOOKS -> ItemLook(R.string.item_hooks, R.string.item_hooks_line, R.drawable.ic_hanger, Hues.INFRARED)
        ChecklistItem.CORNERS -> ItemLook(R.string.item_corners, R.string.item_corners_line, R.drawable.ic_widget, Hues.BLUETOOTH)
        ChecklistItem.PARTITION -> ItemLook(R.string.item_partition, R.string.item_partition_line, R.drawable.ic_structure, Hues.HOTSPOTS)
        ChecklistItem.BED -> ItemLook(R.string.item_bed, R.string.item_bed_line, R.drawable.ic_bed, Hues.BLUETOOTH)
        ChecklistItem.SHOWER -> ItemLook(R.string.item_shower, R.string.item_shower_line, R.drawable.ic_place_washroom, Hues.INFRARED)
        ChecklistItem.SHELVES -> ItemLook(R.string.item_shelves, R.string.item_shelves_line, R.drawable.ic_box, Hues.MAGNET)
        ChecklistItem.DESK -> ItemLook(R.string.item_desk, R.string.item_desk_line, R.drawable.ic_monitor, Hues.WIFI)
        ChecklistItem.OBJECTS -> ItemLook(R.string.item_objects, R.string.item_objects_line, R.drawable.ic_book, Hues.CHECKLIST)
        ChecklistItem.CAR_MIRROR -> ItemLook(R.string.item_car_mirror, R.string.item_car_mirror_line, R.drawable.ic_mirror, Hues.CHECKLIST)
        ChecklistItem.CAR_VENTS -> ItemLook(R.string.item_car_vents, R.string.item_car_vents_line, R.drawable.ic_wind, Hues.LENS)
        ChecklistItem.CAR_PORTS -> ItemLook(R.string.item_car_ports, R.string.item_car_ports_line, R.drawable.ic_usb, Hues.HOTSPOTS)
        ChecklistItem.CAR_CEILING -> ItemLook(R.string.item_car_ceiling, R.string.item_car_ceiling_line, R.drawable.ic_lamp, Hues.MAGNET)
        ChecklistItem.CAR_SEATS -> ItemLook(R.string.item_car_seats, R.string.item_car_seats_line, R.drawable.ic_chair, Hues.BLUETOOTH)
    }

val ChecklistSection.label: Int
    @StringRes get() = when (this) {
        ChecklistSection.MIRROR -> R.string.section_mirror
        ChecklistSection.DEVICES -> R.string.section_devices
        ChecklistSection.ROOM -> R.string.section_room
        ChecklistSection.BED -> R.string.section_bed
        ChecklistSection.BATH -> R.string.section_bath
        ChecklistSection.DESK -> R.string.section_desk
        ChecklistSection.CAR -> R.string.section_car
    }
