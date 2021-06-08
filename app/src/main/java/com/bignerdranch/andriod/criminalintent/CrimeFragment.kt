package com.bignerdranch.andriod.criminalintent

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.*
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_DATE = "DialogDate"
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), FragmentResultListener {
  private lateinit var pickContact: ActivityResultLauncher<Void>

  private lateinit var crime: Crime
  private lateinit var titleField: EditText
  private lateinit var dateButton: Button
  private lateinit var solvedCheckBox: CheckBox
  private lateinit var reportButton: Button
  private lateinit var suspectButton: Button

  private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
    ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    crime = Crime()
    val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
    crimeDetailViewModel.loadCrime(crimeId)

    pickContact = registerForActivityResult(ActivityResultContracts.PickContact()) { result ->
      val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
      val cursor = result?.let {
        requireActivity().contentResolver
          .query(it, queryFields, null, null, null)
      }
      cursor?.use {
        if (it.count == 0) {
          return@use
        }

        it.moveToFirst()
        val suspect = it.getString(0)
        crime.suspect = suspect
        crimeDetailViewModel.saveCrime(crime)
        suspectButton.text = suspect
      }
    }
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
    reportButton = view.findViewById(R.id.crime_report) as Button
    suspectButton = view.findViewById(R.id.crime_suspect) as Button

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    crimeDetailViewModel.crimeLiveData.observe(
      viewLifecycleOwner,
      { crime ->
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

    reportButton.setOnClickListener {
      Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, getCrimeReport())
        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject)).also { intent ->
          val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
          startActivity(chooserIntent)
        }
      }
    }

    suspectButton.apply {
      val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

      setOnClickListener {
        pickContact.launch(null)
      }
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

    if (crime.suspect.isNotEmpty()) {
      suspectButton.text = crime.suspect
    }
  }

  private fun getCrimeReport(): String {
    val solvedString = if (crime.isSolved) {
      getString(R.string.crime_report_solved)
    } else {
      getString(R.string.crime_report_no_suspect)
    }

    val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
    val suspect = if (crime.suspect.isBlank()) {
      getString(R.string.crime_report_no_suspect)
    } else {
      getString(R.string.crime_report)
    }

    return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
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
    when (requestKey) {
      REQUEST_DATE -> {
        crime.date = DatePickerFragment.getSelectedDate(result)
        updateUI()
      }
    }
  }
}