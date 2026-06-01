package com.example.stepforge.ui.streak

enum class StreakBehaviorState {
    ACTIVE,
    STABLE,
    UNSTABLE,
    CRITICAL,
    RESCUED,
    LOST
}

enum class StreakStateMessage {
    SAFE,
    NEEDS_ATTENTION,
    CLOSE_TO_ENDING,
    RESCUED,
    LOST
}
