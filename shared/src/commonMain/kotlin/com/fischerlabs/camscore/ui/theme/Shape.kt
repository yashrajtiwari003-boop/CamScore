package com.fischerlabs.camscore.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Small rounded corners — chips, small buttons */
val SmallRound = RoundedCornerShape(8.dp)

/** Medium rounded corners — input fields, compact cards */
val MediumRound = RoundedCornerShape(12.dp)

/** Large rounded corners — cards, dialogs */
val LargeRound = RoundedCornerShape(16.dp)

/** Extra-large rounded corners — hero cards, bottom sheets */
val ExtraLargeRound = RoundedCornerShape(24.dp)

/** Full pill shape — buttons, status badges */
val PillShape = RoundedCornerShape(percent = 50)

/** Standard card shape */
val CardShape = RoundedCornerShape(16.dp)

/** Badge / pill indicator shape */
val BadgeShape = RoundedCornerShape(20.dp)

/** Certificate card shape — slightly more formal */
val CertificateShape = RoundedCornerShape(12.dp)

/** Material 3 Shapes integration */
val CamScoreShapes = Shapes(
    small = SmallRound,
    medium = MediumRound,
    large = LargeRound,
    extraLarge = ExtraLargeRound
)
