package com.example.jobsheetbook.data.local.dao

import androidx.room.*
import com.example.jobsheetbook.data.local.entities.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE phoneNumber = :phoneNumber AND isDeleted = 0")
    suspend fun getCustomerByPhone(phoneNumber: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers WHERE isDeleted = 0")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("UPDATE customers SET isDeleted = 1, lastModified = :timestamp WHERE phoneNumber = :phone")
    suspend fun softDeleteCustomer(phone: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM customers WHERE lastModified > :since")
    suspend fun getCustomersModifiedSince(since: Long): List<CustomerEntity>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<CustomerEntity>

    @Query("SELECT MAX(lastModified) FROM customers")
    suspend fun getMaxLastModified(): Long?
}
