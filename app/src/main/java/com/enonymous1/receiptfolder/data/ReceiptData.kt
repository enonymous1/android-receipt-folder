package com.enonymous1.receiptfolder.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One row per receipt. Money is stored as integer cents, never as a float,
 * so $12.50 is 1250 and can never come back as 12.499999. Dates are epoch
 * milliseconds at UTC midnight, or null when the receipt has no readable date.
 * [imagePath] is a path under the app's private files directory, or null
 * when no photo was kept. Nothing here leaves the device: v2 is local-first,
 * which also removes v1's shared "Trial Run" Firestore collection.
 */
@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val merchantAddress: String = "",
    val dateMillis: Long? = null,
    val totalCents: Long = 0,
    val receiptName: String = "",
    val imagePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
) {
    fun toDomain(): Receipt = Receipt(
        id = id,
        merchant = merchant,
        merchantAddress = merchantAddress,
        dateMillis = dateMillis,
        totalCents = totalCents,
        receiptName = receiptName,
        imagePath = imagePath,
        createdAtMillis = createdAtMillis,
    )

    companion object {
        fun fromDomain(receipt: Receipt): ReceiptEntity = ReceiptEntity(
            id = receipt.id,
            merchant = receipt.merchant,
            merchantAddress = receipt.merchantAddress,
            dateMillis = receipt.dateMillis,
            totalCents = receipt.totalCents,
            receiptName = receipt.receiptName,
            imagePath = receipt.imagePath,
            createdAtMillis = receipt.createdAtMillis,
        )
    }
}

/** Plain model used by the UI and the ViewModel. No Room annotations here. */
data class Receipt(
    val id: Long = 0,
    val merchant: String,
    val merchantAddress: String = "",
    val dateMillis: Long? = null,
    val totalCents: Long = 0,
    val receiptName: String = "",
    val imagePath: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface ReceiptDao {
    // Newest first; id breaks ties so the order is stable across emissions.
    @Query("SELECT * FROM receipts ORDER BY COALESCE(dateMillis, createdAtMillis) DESC, id DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptEntity?>

    @Insert
    suspend fun insert(receipt: ReceiptEntity): Long

    @Update
    suspend fun update(receipt: ReceiptEntity)

    @Delete
    suspend fun delete(receipt: ReceiptEntity)
}

@Database(entities = [ReceiptEntity::class], version = 1, exportSchema = false)
abstract class ReceiptDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
}

/** The data layer behind the ViewModel. Kept as an interface so tests can fake it. */
interface ReceiptRepository {
    val receipts: Flow<List<Receipt>>
    fun observeReceipt(id: Long): Flow<Receipt?>
    suspend fun save(receipt: Receipt): Long
    suspend fun delete(receipt: Receipt)
}

class RoomReceiptRepository(private val dao: ReceiptDao) : ReceiptRepository {
    override val receipts: Flow<List<Receipt>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeReceipt(id: Long): Flow<Receipt?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun save(receipt: Receipt): Long {
        return if (receipt.id == 0L) {
            dao.insert(ReceiptEntity.fromDomain(receipt))
        } else {
            dao.update(ReceiptEntity.fromDomain(receipt))
            receipt.id
        }
    }

    override suspend fun delete(receipt: Receipt) {
        dao.delete(ReceiptEntity.fromDomain(receipt))
    }
}
