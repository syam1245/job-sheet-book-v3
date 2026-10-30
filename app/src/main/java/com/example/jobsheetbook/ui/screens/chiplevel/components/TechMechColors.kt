package com.example.jobsheetbook.ui.screens.chiplevel.components

import androidx.compose.ui.graphics.Color

// ── Semantic Status Accent Colors (theme-independent) ──
// These represent pipeline states and must stay the same in both light & dark.
val AwaitingGray    = Color(0xFF64748B)
val AtTechMechAmber = Color(0xFFF59E0B)
val BackInGreen     = Color(0xFF10B981)
val OverdueRed      = Color(0xFFEF4444)
val FollowUpOrange  = Color(0xFFEA580C)

// Surface/text constants have been removed.
// All surface, background, and text colors are now resolved from
// MaterialTheme.colorScheme at each call site to support light + dark themes.
