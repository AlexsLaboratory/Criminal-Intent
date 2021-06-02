package com.bignerdranch.andriod.criminalintent.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bignerdranch.andriod.criminalintent.Crime
import java.util.*

@Dao
interface CrimeDao {
  @Query("SELECT * FROM crime")
  fun getCrimes(): LiveData<List<Crime>>

  @Query("SELECT * FROM crime WHERE id=(:id)")
  fun getCrime(id: UUID): LiveData<Crime?>

  @Update
  fun updateCrime(cime: Crime)

  @Insert
  fun addCrime(crime: Crime)
}