package com.tw.offlinegpslocationdemo

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatImageView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tw.offlinegpslocationdemo.location.LocationCallback
import com.tw.offlinegpslocationdemo.databinding.ActivityMainBinding
import com.tw.offlinegpslocationdemo.location.LocationModel
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), LocationCallback {


    /*
    *
    *
    * public static final String PASSIVE_PROVIDER = "passive"
    * Standard name of the passive location provider.
    * Operation of this provider may require a network connection.
    *
    * public static final String GPS_PROVIDER = "gps"
    * Standard name of the GPS location provider.
    * Operation of this provider may require a network connection.
    *
    * public static final String NETWORK_PROVIDER = "network"
    * Standard name of the network location provider.
    * If present, this provider determines location based on nearby of cell tower and WiFi access points.
    * Operation of this provider may require no data connection.
    *
    *
    * */
    lateinit var binding: ActivityMainBinding
    val TAG : String = this.javaClass.simpleName

    private var dialogShownOnce = false
    private var mCurrentLocation: Location? = null

    var progressDialog: ProgressDialog? = null
    lateinit var gpsDialog: Dialog
    lateinit var tvLatitute: TextView
    lateinit var tvLongitude: TextView
    lateinit var tvAccuracy: TextView
    lateinit var tvDate: TextView
    lateinit var ivCancel: AppCompatImageView
    lateinit var btnGPS: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnLocation.setOnClickListener {
            requestPermissions()
        }

    }

    private fun openGpsLocationDialog() {

        gpsDialog = Dialog(this)

        gpsDialog.setCancelable(true)
        gpsDialog.setContentView(R.layout.gps_locationdialog)
//        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        tvLatitute = gpsDialog.findViewById(R.id.tvLatitude)!!
        tvLongitude = gpsDialog.findViewById(R.id.tvLongitude)!!
        tvAccuracy = gpsDialog.findViewById(R.id.tvAccuracy)!!
        tvDate = gpsDialog.findViewById(R.id.tvDate)!!
        ivCancel = gpsDialog.findViewById(R.id.ivCancel)!!
        btnGPS = gpsDialog.findViewById(R.id.btnGPS)!!


        ivCancel.setOnClickListener {
            gpsDialog.dismiss()
            dialogShownOnce = true
        }

        btnGPS.setOnClickListener {
            // dialog.dismiss()
            dialogShownOnce = true

            try {
                val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    progressDialog = ProgressDialog.show(this@MainActivity, "", "Getting Your Location.....", false)
                    val locationModel = LocationModel(this@MainActivity, null)
                    locationModel.initialize()
                    MyApp.instance.getLocationModel()?.setLocationCallback(this)

                } else {
                    val dlgAlert = AlertDialog.Builder(this@MainActivity)
                    dlgAlert.setMessage("Please start GPS sensor manually.")
                    dlgAlert.setTitle("Alert")
                    dlgAlert.setPositiveButton("OK", null)
                    dlgAlert.setCancelable(true)
                    dlgAlert.create().show()
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        if (!gpsDialog.isShowing) {
            gpsDialog.show()
            dialogShownOnce = true
        }

    }

    override fun onLocationFound(latitude: Double, longitude: Double, accuracy: Float) {

        if (progressDialog != null) {
            if (progressDialog!!.isShowing) {
                progressDialog!!.dismiss()
            }
        }

        tvLatitute.text = MyApp.instance.getLocationModel()!!.latitude.toString()
        tvLongitude.text = MyApp.instance.getLocationModel()!!.longitude.toString()
        tvAccuracy.text = MyApp.instance.getLocationModel()!!.accuracy.toString()


        val lat: String = tvLatitute.getText().toString().trim { it <= ' ' }
        if (lat == "") {
            btnGPS.text = "GET LOCATION"
        } else {

            btnGPS.text = "REFRESH LOCATION"

            Log.e(TAG, "onLocationFound accuracy : $accuracy", )
            if (accuracy < 35){
                Toast.makeText(this@MainActivity, "Good Accuracy is $accuracy", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this@MainActivity, "Bad Accuracy is $accuracy", Toast.LENGTH_SHORT).show()
            }
        }
        //  textView.setText("Lat:" + latitude + "\n" + "Long:" + longitude + "\n" + "Accuracy:" + accuracy);


        //  textView.setText("Lat:" + latitude + "\n" + "Long:" + longitude + "\n" + "Accuracy:" + accuracy);
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = dateFormat.format(Date())
        tvDate.text = currentDate

    }


    private fun requestPermissions() {
        Dexter.withContext(this@MainActivity)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.e(TAG, "Granted")

                        openGpsLocationDialog()

                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        Log.e(TAG, "Denied---> ${multiplePermissionsReport.deniedPermissionResponses}")
                        showSettingsDialogAll(multiplePermissionsReport.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { dexterError: DexterError ->
                Log.e(TAG, "dexterError :" + dexterError.name)
            }
            .onSameThread()
            .check()
    }


    fun showSettingsDialogAll(deniedPermissionResponses: MutableList<PermissionDeniedResponse>) {
        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("Need Permissions")
        builder.setMessage(deniedPermissionResponses[0].permissionName)
        builder.setPositiveButton("GOTO SETTINGS") { dialog, _ ->
            dialog.cancel()
            openSettings()
        }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}