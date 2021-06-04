package com.bignerdranch.andriod.criminalintent

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.lifecycle.Observer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_DATE = "DialogDate"

class CrimeFragment : Fragment(), FragmentResultListener {
  private lateinit var crime: Crime
  private lateinit var titleField: EditText
  private lateinit var dateButton: Button
  private lateinit var solvedCheckBox: CheckBox
  private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
    ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    crime = Crime()
    val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
    crimeDetailViewModel.loadCrime(crimeId)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    val view = inflater.inflate(R.layout.fragment_crime, container, false)
    titleField = view.findViewById(R.id.crime_title) as EditText
    dateButton = view.findViewById(R.id.crime_date) as Button
    solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    crimeDetailViewModel.crimeLiveData.observe(
      viewLifecycleOwner,
      Observer { crime ->
        crime?.let {
          this.crime = crime
          updateUI()
        }
      })
    parentFragmentManager.setFragmentResultListener(REQUEST_DATE, viewLifecycleOwner, this)
  }

  override fun onStart() {
    super.onStart()
    val titleWatcher = object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
      }

      override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        crime.title = s.toString()
      }

      override fun afterTextChanged(s: Editable?) {
      }
    }
    titleField.addTextChangedListener(titleWatcher)

    solvedCheckBox.setOnCheckedChangeListener { _, isChecked ->
      crime.isSolved = isChecked
    }

    dateButton.setOnClickListener {
      DatePickerFragment
        .newInstance(crime.date, REQUEST_DATE)
        .show(parentFragmentManager, REQUEST_DATE)
    }
  }

  override fun onStop() {
    super.onStop()
    crimeDetailViewModel.saveCrime(crime)
  }

  private fun updateUI() {
    titleField.setText(crime.title)
    dateButton.text = crime.date.toString()
    solvedCheckBox.apply {
      isChecked = crime.isSolved
      jumpDrawablesToCurrentState()
    }
  }

  companion object {
    fun newInstance(crimeId: UUID): CrimeFragment {
      val args = Bundle().apply {
        putSerializable(ARG_CRIME_ID, crimeId)
      }
      return CrimeFragment().apply {
        arguments = args
      }
    }
  }

  override fun onFragmentResult(requestKey: String, result: Bundle) {
    when(requestKey) {
      REQUEST_DATE -> {
        crime.date = DatePickerFragment.getSelectedDate(result)
        updateUI()
      }
    }
  }
}