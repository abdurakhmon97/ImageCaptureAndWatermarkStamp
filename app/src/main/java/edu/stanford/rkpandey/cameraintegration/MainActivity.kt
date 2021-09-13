package edu.stanford.rkpandey.cameraintegration

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.content.DialogInterface
import android.location.Address
import android.location.Location
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import com.google.android.gms.location.*
import java.lang.Exception
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*
import android.location.Geocoder
import android.util.Log


private const val FILE_NAME = "photo.jpg"
private const val REQUEST_CODE = 42
private lateinit var photoFile: File
class MainActivity : AppCompatActivity() {

    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    var str: String? = null

    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        permissionRequest()

        btnTakePicture.setOnClickListener {

            try {
                requestLocation()
                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                photoFile = getPhotoFile(FILE_NAME)

                val fileProvider = FileProvider.getUriForFile(this, "edu.stanford.rkpandey.fileprovider", photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)


                if (takePictureIntent.resolveActivity(this.packageManager) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_CODE)
                } else {
                    Toast.makeText(this, "Unable to open camera", Toast.LENGTH_SHORT).show()
                }
            }
            catch (e: Exception) {
                Toast.makeText(this, "No permission was granted by user", Toast.LENGTH_SHORT).show()
            }

        }
    }

    private fun getPhotoFile(fileName: String): File {
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", storageDirectory)
    }

    @SuppressLint("SimpleDateFormat")
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
//            val takenImage = data?.extras?.get("data") as Bitmap
            val takenImage = BitmapFactory.decodeFile(photoFile.absolutePath)

            val matrix: Matrix = Matrix()

            matrix.postRotate(90F);
            val rotatedBitmap: Bitmap = Bitmap.createBitmap(takenImage, 0, 0, takenImage.width, takenImage.height, matrix, true);
            Toast.makeText(this, "${rotatedBitmap.height} + ${rotatedBitmap.width}", Toast.LENGTH_LONG).show()
            val currentDateTime = LocalDateTime.now()
            val parser =  SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
            val formattedDate = formatter.format(parser.parse(currentDateTime.toString()))

            iv_zoomable.setImageBitmap(addWatermark(rotatedBitmap,
                "User: Jon\nLat: $mLatitude\nLong: $mLongitude\nAddress: $str\nTime: $formattedDate"
            ))
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }

    }

    private fun permissionRequest() {
        Dexter.withContext(this)
            .withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA
            ).withListener(object: MultiplePermissionsListener{
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                    if(p0!!.areAllPermissionsGranted()){
                        Toast.makeText(this@MainActivity, "permissions granted", Toast.LENGTH_LONG).show()
                        requestLocation()
                    }
                    if (p0.isAnyPermissionPermanentlyDenied) {
                        showSettingsDialog();
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: MutableList<PermissionRequest>?,
                    p1: PermissionToken?
                ) {
                    p1!!.continuePermissionRequest()
                }

            }).onSameThread().check()
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Need permission")
        builder.setMessage("The app needs to use these features. You can grant permission in settings")

        builder.setPositiveButton(
            "GOTO SETTINGS"
        ) { dialog, _ ->
            dialog.cancel()
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivityForResult(intent, 101)
        }

        builder.setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000
            numUpdates = 1
        }

        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper()!!)
    }

    private val locationCallback = object: LocationCallback() {
        override fun onLocationResult(location: LocationResult) {
            val lastLocation: Location = location.lastLocation
            mLatitude = lastLocation.latitude
            mLongitude = lastLocation.longitude
            val addresses: List<Address>
            val geocoder = Geocoder(this@MainActivity, Locale.getDefault())
            try {
                addresses = geocoder.getFromLocation(mLatitude, mLongitude, 1)
                addresses[0].getAddressLine(0)
                str = addresses[0].getAddressLine(0)
                Toast.makeText(this@MainActivity, addresses[0]?.getAddressLine(0), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
        }
    }
}
