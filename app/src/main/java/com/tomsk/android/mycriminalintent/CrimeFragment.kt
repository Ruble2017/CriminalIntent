package com.tomsk.android.mycriminalintent

import android.app.Activity
import android.app.ProgressDialog.show
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings.System.DATE_FORMAT
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
//import java.text.DateFormat
import android.text.format.DateFormat
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import java.util.*
private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = "request_date"
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

class CrimeFragment:Fragment(), DatePickerFragment.Callbacks {

    private lateinit  var crime: Crime
    private lateinit var photoFile: File
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button

    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoUri: Uri



    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        //ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        Log.d(TAG, "__________ args bundle crime ID: $crimeId")
        // Загрузка преступления из базы данных
        crimeDetailViewModel.loadCrime(crimeId)


    }



    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view: View = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title)
        dateButton = view.findViewById(R.id.crime_date)
        reportButton = view.findViewById(R.id.crime_report)
        suspectButton = view.findViewById(R.id.crime_suspect) as Button

        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView

//        dateButton.apply {
//            text = crime.date.toString()
//            isEnabled = false
//        }
        solvedCheckBox = view.findViewById(R.id.crime_solved)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        crimeDetailViewModel.crimeLiveData.observe(
                viewLifecycleOwner,
                androidx.lifecycle.Observer { crime ->
                    crime?.let {
                        this.crime = crime
                        photoFile = crimeDetailViewModel.getPhotoFile(crime)
                        photoUri = FileProvider.getUriForFile(requireActivity(),
                                "com.tomsk.android.mycriminalintent.fileprovider",
                                photoFile)
                        updateUI()
                    }
                }
        )


//        crimeDetailViewModel.crimeLiveData.observe(
//                viewLifecycleOwner,
//                Observer { crime ->
//                    crime?.let {
//                        this.crime = crime
//                        updateUI()
//                    }
//                })
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
                //solvedCheckBox.isChecked = crime.isSolved
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
            getString(R.string.crime_report_unsolved)
        }
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }
        return getString(R.string.crime_report,
                crime.title, dateString, solvedString, suspect)
    }






    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?,
                                           start: Int,
                                           count: Int,
                                           after: Int) {

            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int, count: Int) {
                crime.title = sequence.toString()
            }

            override fun afterTextChanged(p0: Editable?) {

            }
        }

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }
        dateButton.setOnClickListener {
            // DatePickerFragment().apply {
            DatePickerFragment.newInstance(crime.date).apply {
                //show(this@CrimeFragment.requireFragmentManager(), DIALOG_DATE)
                //setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                this@CrimeFragment.parentFragmentManager.setFragmentResultListener("requestKey", this) { requestKey, bundle ->
                    val result = bundle.getSerializable("bundleKey") as Date
                    onDateSelected(result)
                }

                show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
            }
        }


        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.crime_report_subject))
            }.also { intent ->
                //startActivity(intent)
                val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }

        suspectButton.apply {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener {
                //startActivityForResult(pickContactIntent, REQUEST_CONTACT)
                resultLauncher.launch(pickContactIntent)
            }

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                        PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
            isEnabled = false
            }
        }

        photoButton.apply {

            val packageManager: PackageManager = requireActivity().packageManager
            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? =
                    packageManager.resolveActivity(captureImage,
                            PackageManager.MATCH_DEFAULT_ONLY)

            if (resolvedActivity == null) {
                isEnabled = false
            }

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                val cameraActivities: List<ResolveInfo> =
                        packageManager.queryIntentActivities(captureImage,
                                PackageManager.MATCH_DEFAULT_ONLY)
                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(
                            cameraActivity.activityInfo.packageName,
                            photoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                        //startActivityForResult(captureImage, REQUEST_PHOTO)
                photoRezLauncher.launch(captureImage)
            }


        }


//return view
        titleField.addTextChangedListener(titleWatcher)
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    var photoRezLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){

        requireActivity().revokeUriPermission(photoUri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        updatePhotoView()

    }




    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode === Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            //doSomeOperations()
            val contactUri: Uri? = data?.data

            if(contactUri!=null){
            // Указать, для каких полей ваш запрос должен возвращать значения.
            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            // Выполняемый здесь запрос — contactUri похож на предложение "where"
            val cursor = requireActivity().contentResolver
                    .query(contactUri, queryFields, null, null, null)
            cursor?.use {
                // Verify cursor contains at least one result
                if (it.count > 0) {
                    // Первый столбец первой строки данных —
                    // это имя вашего подозреваемого.
                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
            }

        }
    }




    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
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

    object fragmentListener
    {

    }
    override fun onDateSelected(date: Date) {
        crime.date = date
        updateUI()
    }

}