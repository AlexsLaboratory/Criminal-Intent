package com.bignerdranch.andriod.criminalintent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.*
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val REQUEST_DATE = "DialogDate"
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment(), FragmentResultListener {
  private lateinit var pickContact: ActivityResultLauncher<Void>
  private lateinit var pickImage: ActivityResultLauncher<Uri>

  private lateinit var crime: Crime
  private lateinit var titleField: EditText
  private lateinit var dateButton: Button
  private lateinit var solvedCheckBox: CheckBox
  private lateinit var reportButton: Button
  private lateinit var suspectButton: Button
  private lateinit var photoButton: ImageButton
  private lateinit var photoView: ImageView
  private lateinit var photoFile: File
  private lateinit var photoUri: Uri

  private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
    ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
  }

  private var permMap: MutableMap<String, Boolean> = mutableMapOf()

  private val requestMultiplePermissionLauncher =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}

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

    pickImage =
      registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
          updatePhotoView()
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
    photoButton = view.findViewById(R.id.crime_camera) as ImageButton
    photoView = view.findViewById(R.id.crime_photo) as ImageView

    return view
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    crimeDetailViewModel.crimeLiveData.observe(
      viewLifecycleOwner,
      { crime ->
        crime?.let {
          this.crime = crime
          photoFile = crimeDetailViewModel.getPhotoFile(crime)
          photoUri = FileProvider.getUriForFile(
            requireActivity(),
            "com.bignerdranch.android.criminalintent.fileprovider",
            photoFile
          )
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

    val contactsPermissions: Array<String> = arrayOf(Manifest.permission.READ_CONTACTS)

    suspectButton.apply {
      isEnabled = true
      setOnClickListener {
        requestMultiplePermissionLauncher.launch(contactsPermissions)

        if (isEnabledPermissions(requireContext(), contactsPermissions)) {
          pickContact.launch(null)
        } else {
          isEnabled = false
        }
      }
    }

    val cameraPerms: Array<String> = arrayOf(
      Manifest.permission.WRITE_EXTERNAL_STORAGE,
      Manifest.permission.CAMERA,
    )

    photoButton.apply {
      isEnabled = true
      setOnClickListener {
        requestMultiplePermissionLauncher.launch(cameraPerms)
        if (isEnabledPermissions(requireContext(), cameraPerms)) {
          pickImage.launch(photoUri)
        } else {
          isEnabled = false
        }
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
    updatePhotoView()
  }

  private fun updatePhotoView() {
    if (photoFile.exists()) {
      val bitmap = getScaledBitmap(photoFile.path, requireActivity())
      photoView.setImageBitmap(bitmap)
    } else {
      photoView.setImageDrawable(null)
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
      getString(R.string.crime_report_suspect, crime.suspect)
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