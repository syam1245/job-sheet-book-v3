package com.example.jobsheetbook.domain.policy

enum class WarningLevel {
    SOFT,
    STRONG
}

sealed interface WarningReason {
    object SkippedStep : WarningReason
    object BackwardMove : WarningReason
    object LeavingTerminal : WarningReason
    object ZeroCost : WarningReason
}

sealed interface TransitionResult {
    object Allowed : TransitionResult
    data class Warn(val level: WarningLevel, val reasons: List<WarningReason>) : TransitionResult
}
