package com.example.p2meshandroid.data.repository

import com.example.p2meshandroid.data.storage.WalletStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uniffi.p2pmesh_bridge.Wallet
import uniffi.p2pmesh_bridge.SignedIou
import uniffi.p2pmesh_bridge.createWallet as nativeCreateWallet
import uniffi.p2pmesh_bridge.restoreWallet as nativeRestoreWallet
import uniffi.p2pmesh_bridge.fundWalletFromFaucet
import uniffi.p2pmesh_bridge.faucetDid
import uniffi.p2pmesh_bridge.faucetPublicKey
import uniffi.p2pmesh_bridge.requestFromFaucet

/**
 * Repository that wraps UniFFI native calls with persistence.
 * All native calls run on IO dispatcher for thread safety.
 *
 * Persistence Strategy:
 * - Secret key saved to EncryptedSharedPreferences
 * - Wallet state saved to internal file storage
 * - Auto-saves after any state-changing operation
 */
class WalletRepository(
    private val storage: WalletStorage? = null
) {

    private var currentWallet: Wallet? = null

    /**
     * Try to load existing wallet from storage
     * Returns true if wallet was loaded, false if no wallet exists
     */
    suspend fun tryLoadFromStorage(): Result<WalletInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val walletStorage = storage ?: return@runCatching null

            if (!walletStorage.hasWallet()) {
                return@runCatching null
            }

            val secretKey = walletStorage.getSecretKey()
                ?: return@runCatching null

            // Restore wallet from secret key
            val wallet = nativeRestoreWallet(secretKey)
            currentWallet = wallet

            // Import saved state if exists
            val savedState = walletStorage.getWalletState()
            if (savedState != null) {
                try {
                    wallet.importState(savedState)
                } catch (e: Exception) {
                    // State import failed, but wallet is still usable
                    e.printStackTrace()
                }
            }

            wallet.toWalletInfo()
        }
    }

    /**
     * Create a new wallet with fresh keypair
     */
    suspend fun createNewWallet(): Result<WalletInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = nativeCreateWallet()
            currentWallet = wallet

            // Save to storage
            saveToStorage()

            wallet.toWalletInfo()
        }
    }

    /**
     * Restore wallet from secret key bytes
     */
    suspend fun restoreFromSecretKey(secretKey: ByteArray): Result<WalletInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = nativeRestoreWallet(secretKey)
            currentWallet = wallet

            // Save to storage
            saveToStorage()

            wallet.toWalletInfo()
        }
    }

    /**
     * Get current wallet info
     */
    suspend fun getWalletInfo(): Result<WalletInfo> = withContext(Dispatchers.IO) {
        runCatching {
            currentWallet?.toWalletInfo()
                ?: throw IllegalStateException("No wallet loaded")
        }
    }

    /**
     * Create a payment IOU
     */
    suspend fun createPayment(recipientDid: String, amount: ULong): Result<PaymentInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            val iou = wallet.createPayment(recipientDid, amount)
            PaymentInfo(
                id = iou.id(),
                sender = iou.sender(),
                recipient = iou.recipient(),
                amount = iou.amount(),
                timestamp = iou.timestamp(),
                iou = iou
            )
        }
    }

    /**
     * Mark payment as sent
     */
    suspend fun markPaymentSent(iou: SignedIou): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.markSent(iou)
            saveStateToStorage() // Auto-save after change
        }
    }

    /**
     * Process received payment
     */
    suspend fun processPayment(iou: SignedIou): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.processPayment(iou)
            saveStateToStorage() // Auto-save after change
        }
    }

    /**
     * Process payment with explicit sender key
     */
    suspend fun processPaymentWithKey(iou: SignedIou, senderPubKey: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.processPaymentWithKey(iou, senderPubKey)
            saveStateToStorage() // Auto-save after change
        }
    }

    /**
     * Get pending IOUs
     */
    suspend fun getPendingPayments(): Result<List<PaymentInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.pendingIous().map { iou ->
                PaymentInfo(
                    id = iou.id(),
                    sender = iou.sender(),
                    recipient = iou.recipient(),
                    amount = iou.amount(),
                    timestamp = iou.timestamp(),
                    iou = iou
                )
            }
        }
    }

    /**
     * Export wallet state for persistence
     */
    suspend fun exportState(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.exportState()
        }
    }

    /**
     * Import wallet state
     */
    suspend fun importState(data: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.importState(data)
            saveStateToStorage() // Auto-save after change
        }
    }

    /**
     * Get secret key for backup
     */
    suspend fun getSecretKey(): Result<ByteArray> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            wallet.secretKey()
        }
    }

    /**
     * Check if wallet is loaded
     */
    fun isWalletLoaded(): Boolean = currentWallet != null

    /**
     * Check if wallet exists in storage
     */
    fun hasStoredWallet(): Boolean = storage?.hasWallet() ?: false

    /**
     * Get native wallet for use with other components (Mesh, Collector, etc.)
     */
    fun getNativeWallet(): Result<Wallet> {
        return currentWallet?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("No wallet loaded"))
    }

    /**
     * Clear wallet from memory and storage
     */
    suspend fun clearWallet() = withContext(Dispatchers.IO) {
        currentWallet = null
        storage?.clearAll()
    }

    // ========== Faucet Operations ==========

    /**
     * Fund wallet from faucet (demo funding)
     * Uses the hardcoded faucet keypair to sign IOUs
     */
    suspend fun fundFromFaucet(amount: ULong): Result<WalletInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            fundWalletFromFaucet(wallet, amount)
            saveStateToStorage() // Auto-save after change
            wallet.toWalletInfo()
        }
    }

    /**
     * Request funds from faucet (returns IOU for manual processing)
     */
    suspend fun requestFaucetFunds(amount: ULong): Result<SignedIou> = withContext(Dispatchers.IO) {
        runCatching {
            val wallet = currentWallet ?: throw IllegalStateException("No wallet loaded")
            requestFromFaucet(wallet.did(), amount)
        }
    }

    /**
     * Get faucet DID
     */
    fun getFaucetDid(): String = faucetDid()

    /**
     * Get faucet public key
     */
    fun getFaucetPublicKey(): ByteArray = faucetPublicKey()

    // ========== Private Helpers ==========

    /**
     * Save wallet (key + state) to storage
     */
    private fun saveToStorage() {
        val wallet = currentWallet ?: return
        val walletStorage = storage ?: return

        try {
            // Save secret key
            walletStorage.saveSecretKey(wallet.secretKey())

            // Save state
            walletStorage.saveWalletState(wallet.exportState())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Save only wallet state to storage (after changes)
     */
    private fun saveStateToStorage() {
        val wallet = currentWallet ?: return
        val walletStorage = storage ?: return

        try {
            walletStorage.saveWalletState(wallet.exportState())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun Wallet.toWalletInfo() = WalletInfo(
        did = did(),
        publicKey = publicKey(),
        balance = balance(),
        availableBalance = availableBalance(),
        utxoCount = utxoCount(),
        transactionCount = transactionCount()
    )
}

/**
 * Data class representing wallet information
 */
data class WalletInfo(
    val did: String,
    val publicKey: ByteArray,
    val balance: ULong,
    val availableBalance: ULong,
    val utxoCount: ULong,
    val transactionCount: ULong
) {
    val publicKeyHex: String
        get() = publicKey.joinToString("") { "%02x".format(it) }

    val shortDid: String
        get() = if (did.length > 30) "${did.take(30)}..." else did
}

/**
 * Data class representing a payment
 */
data class PaymentInfo(
    val id: String,
    val sender: String,
    val recipient: String,
    val amount: ULong,
    val timestamp: ULong,
    val iou: SignedIou
)
