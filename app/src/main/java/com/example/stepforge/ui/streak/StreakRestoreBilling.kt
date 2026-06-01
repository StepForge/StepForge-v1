package com.example.stepforge.ui.streak

const val STREAK_RESTORE_PRODUCT_ID = "streak_restore_eur2"

/**
 * Placeholder for Google Play Billing integration.
 * Wire [GooglePlayStreakRestoreBilling] when billing is ready.
 */
interface StreakRestoreBilling {
    suspend fun purchaseRestore(productId: String = STREAK_RESTORE_PRODUCT_ID): StreakRestoreResult
}

data class StreakRestoreResult(
    val success: Boolean,
    val errorMessage: String? = null
)

object MockStreakRestoreBilling : StreakRestoreBilling {

    override suspend fun purchaseRestore(productId: String): StreakRestoreResult {
        return StreakRestoreResult(success = true)
    }
}
