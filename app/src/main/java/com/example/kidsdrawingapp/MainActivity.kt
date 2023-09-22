package com.example.kidsdrawingapp

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
import android.provider.MediaStore.Images.Media
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.get
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    lateinit var drawingView:DrawingView
    lateinit var ibBrush:ImageButton
    lateinit var ibGallery:ImageButton
    lateinit var mImageButtonCurrentPaint:ImageButton
    lateinit var llPaintColors:LinearLayout
    lateinit var ivBackground:ImageView
    lateinit var ibUndo:ImageButton
    lateinit var ibSave:ImageButton
    lateinit var flDrawingViewContainer:FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        ibBrush = findViewById(R.id.ib_brush)
        llPaintColors = findViewById(R.id.ll_paint_colors)
        mImageButtonCurrentPaint = llPaintColors[1] as ImageButton
        mImageButtonCurrentPaint.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
        ibGallery = findViewById(R.id.ib_gallery)
        ivBackground = findViewById(R.id.iv_background)
        ibUndo = findViewById(R.id.ib_undo)
        ibSave = findViewById(R.id.ib_save)
        flDrawingViewContainer = findViewById(R.id.fl_drawing_view_container)

        drawingView.setSizeForBrush(20f)
        ibBrush.setOnClickListener {
            showBrushSizeChooserDialog()
        }
        ibGallery.setOnClickListener {
            if(isReadStorageAllowed()){
                val pickPhotoIntent = Intent(Intent.ACTION_PICK,Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(pickPhotoIntent, GALLERY)
            }else{
                requestStoragePermission()
            }
        }
        ibUndo.setOnClickListener {
            drawingView.onClickUndo()
        }
        ibSave.setOnClickListener {
            if(isReadStorageAllowed()){
                BitmapAsyncTask(getBitmapFromView(flDrawingViewContainer)).execute()
            }else{
                requestStoragePermission()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            try{
                if(data!!.data != null){
                    ivBackground.visibility = View.VISIBLE
                    ivBackground.setImageURI(data.data)
                }else{
                    Toast.makeText(this,"Error in parsing the image or its corrupted",Toast.LENGTH_SHORT).show()
                }

            }catch (e:java.lang.Exception){
                e.printStackTrace()
            }
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBrush = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        val mediumBrush = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        val largeBrush = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        smallBrush.setOnClickListener {
            drawingView.setSizeForBrush(10f)
            brushDialog.dismiss()
        }
        mediumBrush.setOnClickListener {
            drawingView.setSizeForBrush(20f)
            brushDialog.dismiss()
        }
        largeBrush.setOnClickListener {
            drawingView.setSizeForBrush(30f)
            brushDialog.dismiss()
        }
        brushDialog.show()

    }


    fun paintClicked(view: View){
        if(view != mImageButtonCurrentPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView.setColor(colorTag)
            imageButton.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_pressed))
            mImageButtonCurrentPaint.setImageDrawable(ContextCompat.getDrawable(this,R.drawable.pallet_normal))
            mImageButtonCurrentPaint = view
        }
    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE).toString())){
            Toast.makeText(this,"Need permission to add background",Toast.LENGTH_SHORT).show()

        }
        ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_CODE)

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == STORAGE_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this,"Permission Granted!, you can access the storage files",Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this,"Oops!, you just denied the permission",Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun isReadStorageAllowed():Boolean{
        val result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun getBitmapFromView(view: View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private inner class BitmapAsyncTask(val mBitmap:Bitmap):AsyncTask<Any,Void,String>() {
        private lateinit var mProgressDialog:Dialog

        override fun onPreExecute() {
            super.onPreExecute()
            showProgressDialog()

        }

        override fun doInBackground(vararg params: Any?): String {
            var result = ""
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(
                        externalCacheDir!!.absoluteFile.toString()
                                + File.separator
                                + "Kid sDrawingApp_"
                                + System.currentTimeMillis() / 1000
                                + ".png"
                    )
                    val fos = FileOutputStream(f)
                    fos.write(bytes.toByteArray())
                    fos.close()
                    result = f.absolutePath
                } catch (e: java.lang.Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            cancelProgressDialog()
        if (!result!!.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "File saved successfully: $result",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(this@MainActivity, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
            MediaScannerConnection.scanFile(this@MainActivity, arrayOf(result),null){
                path,uri -> val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
                shareIntent.type = "image/png"

                startActivity(Intent.createChooser(shareIntent,"Share"))
            }
        }
        private fun showProgressDialog(){
            mProgressDialog = Dialog(this@MainActivity)
            mProgressDialog.setContentView(R.layout.dialog_custom_progress)
            mProgressDialog.show()
        }
        private fun cancelProgressDialog(){
            mProgressDialog.dismiss()
        }
    }



    companion object{
        private const val STORAGE_PERMISSION_CODE = 1
        private const val GALLERY = 2
    }
}