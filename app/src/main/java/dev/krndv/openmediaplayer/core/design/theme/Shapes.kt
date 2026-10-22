package dev.krndv.openmediaplayer.core.design.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Shared shape tokens. App-specific experiences may use other Material shapes when a
 * component's purpose benefits from it; these values are the family defaults.
 */
internal val FamilyShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
