package com.bignerdranch.andriod.criminalintent

import androidx.lifecycle.ViewModel

class CrimeLIstViewModel : ViewModel() {
  private val crimeRepository = CrimeRepository.get()
  val crimeListLiveData = crimeRepository.getCrimes()
}