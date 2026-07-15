package com.elendheim.pictureeditor.model

/**
 * The premade one tap looks that ship with the app. Each is just a preset of
 * adjustment values (and sometimes a vignette), so they are fully editable
 * after tapping and make a perfect starting point for saving your own.
 */
object BuiltInFilters {

    val all: List<FilterPreset> = listOf(
        FilterPreset(
            id = "original",
            name = "Original",
            adjust = AdjustParams(),
            builtIn = true
        ),
        FilterPreset(
            id = "elendheim",
            name = "Elendheim",
            // The house look: softly desaturated with a faint warm red lift.
            adjust = AdjustParams(
                exposure = 0.02f, contrast = 0.12f, saturation = -0.18f,
                warmth = 0.14f, tint = 0.03f
            ),
            vignette = Vignette(amount = 0.22f, size = 0.55f, feather = 0.6f),
            builtIn = true
        ),
        FilterPreset(
            id = "mono",
            name = "Mono",
            adjust = AdjustParams(saturation = -1f, contrast = 0.08f),
            builtIn = true
        ),
        FilterPreset(
            id = "mono_punch",
            name = "Mono Punch",
            adjust = AdjustParams(saturation = -1f, contrast = 0.45f),
            builtIn = true
        ),
        FilterPreset(
            id = "warm_faded",
            name = "Warm Faded",
            adjust = AdjustParams(
                exposure = 0.08f, contrast = -0.18f, saturation = -0.1f, warmth = 0.28f
            ),
            builtIn = true
        ),
        FilterPreset(
            id = "cool_faded",
            name = "Cool Faded",
            adjust = AdjustParams(
                contrast = -0.14f, saturation = -0.12f, warmth = -0.3f, tint = -0.05f
            ),
            builtIn = true
        ),
        FilterPreset(
            id = "golden",
            name = "Golden Hour",
            adjust = AdjustParams(
                exposure = 0.05f, contrast = 0.1f, saturation = 0.22f, warmth = 0.42f
            ),
            builtIn = true
        ),
        FilterPreset(
            id = "teal",
            name = "Teal Cool",
            adjust = AdjustParams(
                contrast = 0.16f, saturation = 0.14f, warmth = -0.4f, tint = 0.12f
            ),
            builtIn = true
        ),
        FilterPreset(
            id = "vivid",
            name = "Vivid Pop",
            adjust = AdjustParams(contrast = 0.28f, saturation = 0.5f, exposure = 0.03f),
            builtIn = true
        ),
        FilterPreset(
            id = "matte",
            name = "Muted Matte",
            adjust = AdjustParams(contrast = -0.28f, saturation = -0.24f, brightness = 0.06f),
            vignette = Vignette(amount = 0.16f, size = 0.6f, feather = 0.7f),
            builtIn = true
        )
    )
}
