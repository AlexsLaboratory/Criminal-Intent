package com.bignerdranch.andriod.criminalintent

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

private const val TAG = "CrimeListFragment"

class CrimeListFragment : Fragment() {
  private val crimeListViewModel: CrimeLIstViewModel by lazy {
    ViewModelProvider(this).get(CrimeLIstViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Log.d(TAG, "Total crimes: ${crimeListViewModel.crimes.size}")
  }

  companion object {
    fun newInstance(): CrimeListFragment {
      return CrimeListFragment()
    }
  }
}