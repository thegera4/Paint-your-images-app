package com.app.kot_drawing_app

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import com.app.kot_drawing_app.databinding.ActivityMainBinding
import com.app.kot_drawing_app.databinding.DialogBrushSizeBinding
import com.app.kot_drawing_app.databinding.DialogColorPickerBinding
import com.google.android.gms.ads.interstitial.InterstitialAd
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import androidx.annotation.NonNull
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback





class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var bindingDialogBrushSize: DialogBrushSizeBinding
    private lateinit var brushDialog: Dialog
    private lateinit var bindingDialogColorPicker: DialogColorPickerBinding
    private lateinit var colorDialog: Dialog

    private var mImageButtonCurrentPaint: ImageButton? = null

    //interstitial ad
    private var mInterstitialAd: InterstitialAd? = null
    private var TAG = "MainActivity"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        var view = binding.root
        setContentView(view)

        //ads initialization
        MobileAds.initialize(this@MainActivity)

        //Banner ad
        val adReq = AdRequest.Builder().build()
        binding.adView.loadAd(adReq)

        //interstitial ad
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(this,"\n" +
                "ca-app-pub-4193581455913262/4868971382",
            adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.d(TAG, adError.message)
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "Ad was loaded.")
                mInterstitialAd = interstitialAd
            }
        })

        mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad was dismissed.")
            }

            override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                Log.d(TAG, "Ad failed to show.")
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                mInterstitialAd = null
            }
        }


        supportActionBar?.hide()

        mImageButtonCurrentPaint = binding.llPaintColors[3] as ImageButton
        mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable
            (this, R.drawable.pallet_pressed))

        bindingDialogBrushSize = DialogBrushSizeBinding.inflate(layoutInflater)
        brushDialog = Dialog(this)
        view = bindingDialogBrushSize.root
        brushDialog.setContentView(view)
        brushDialog.setTitle("Brush size: ")

        binding.drawingView.setSizeForBrush(10.toFloat())

        bindingDialogColorPicker = DialogColorPickerBinding.inflate(layoutInflater)
        colorDialog = Dialog(this)
        view = bindingDialogColorPicker.root
        colorDialog.setContentView(view)
        colorDialog.setTitle("Select color: ")

        binding.ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }

        binding.ibColors.setOnClickListener {
            showColorPickerDialog()
        }

        binding.ibGallery.setOnClickListener {
            if (isReadStorageAllowed()){
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            } else {
                requestStoragePermission()
            }
        }

        binding.ibUNDO.setOnClickListener {
            binding.drawingView.onClickUndo()
        }

        binding.ibStorage.setOnClickListener {
            //Show the Ad
            if (mInterstitialAd != null) {
                mInterstitialAd?.show(this)
            } else {
                Log.d("TAG", "The interstitial ad wasn't ready yet.")
            }
            if (isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(binding.flDrawingViewContainer)).execute()
            } else {
                requestStoragePermission()
            }
        }




    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == GALLERY){
                try {
                    if (data!!.data != null){
                        binding.ivBackground.visibility = View.VISIBLE
                        binding.ivBackground.setImageURI(data.data)
                    } else {
                        Toast.makeText(this, "Error in file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun showBrushSizeChooserDialog(){

        bindingDialogBrushSize.ibSmallBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(10F)
            brushDialog.dismiss()

        }
        bindingDialogBrushSize.ibMediumBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(20F)
            brushDialog.dismiss()

        }
        bindingDialogBrushSize.ibLargeBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(30F)
            brushDialog.dismiss()

        }
        brushDialog.show()
    }

    private fun showColorPickerDialog() {
            colorDialog.show()
        }

    fun paintClicked(view: View){
        if (view !== mImageButtonCurrentPaint){
            val imageButton = view as ImageButton

            val colorTag = imageButton.tag.toString()
            binding.drawingView.setColor(colorTag)
            //imageButton.setImageDrawable(ContextCompat.getDrawable
                //(this, R.drawable.pallet_pressed))
            //mImageButtonCurrentPaint!!.setImageDrawable(ContextCompat.getDrawable
                //(this, R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view
        }
        colorDialog.dismiss()
    }

    private fun requestStoragePermission(){
        /*if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this, "The app needs permission to access device images",
                Toast.LENGTH_SHORT).show()
        }*/
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission granted. Now you can access the files",
                    Toast.LENGTH_SHORT).show()
            } else{
                Toast.makeText(this, "You denied the permission",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isReadStorageAllowed(): Boolean{
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View) : Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null){
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        view.draw(canvas)

        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap: Bitmap) : AsyncTask<Any, Void, String>() {

        private lateinit var mProgressDialog: Dialog

        @Deprecated("Deprecated in Java")
        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()
        }

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg params: Any?): String {

            var result = ""
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir!!.absoluteFile.toString() +
                                File.separator + "Drawing App" +
                                System.currentTimeMillis() / 1000 + ".png"
                    )
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }

           MediaScannerConnection.scanFile(
                this@MainActivity,
                arrayOf(result), null
            ) { path, uri ->
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"
                startActivity(Intent.createChooser(shareIntent, "Share"))
            }

            return result
        }


        @Deprecated("Deprecated in Java")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
            if (result!!.isNotEmpty()) {
                Toast.makeText(
                    this@MainActivity, "Files saved successfully: " +
                            "$result", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Something went wrong while saving the file. Please try again",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }



        private fun showProgressDialog() {
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
            //buscar como agregar timer para darle algo de delay
        }

        private fun cancelProgressDialog() {
            mProgressDialog.dismiss()
        }
    }


    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}
