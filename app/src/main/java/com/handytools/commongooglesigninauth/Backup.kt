package com.handytools.commongooglesigninauth

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.IOUtils
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class Backup : AppCompatActivity() {


    companion object {
        private val TAG = "BackupActivity"
        private val RC_SELECT_FILE = 9111
    }

    lateinit var mDrive: Drive


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)


        mDrive = getDriveService(this)
        var addAttachment = findViewById<Button>(R.id.upload_button)
        addAttachment.setOnClickListener {
            GlobalScope.async(Dispatchers.IO) {
                val intent = Intent().setType("*/*").setAction(Intent.ACTION_GET_CONTENT)
                startActivityForResult(Intent.createChooser(intent, "Select a file"), RC_SELECT_FILE)
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SELECT_FILE && resultCode == RESULT_OK) {
            val selectedFile = data!!.data //The uri with the location of the file

            val  fileName = getFileName(applicationContext.contentResolver, selectedFile!!)
            uploadFileToLocalAppDir(selectedFile!!, fileName)
            uploadFileToGoogleDrive(application, fileName)

            Toast.makeText(this, "Selected file uploaded to Google Drive: $selectedFile",Toast.LENGTH_LONG).show()
        }
    }

    private fun getFileName(contentResolver: ContentResolver, fileUri: Uri): String {

        var name = ""
        val returnCursor = contentResolver.query(fileUri, null, null, null, null)

        if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            returnCursor.moveToFirst()
            name = returnCursor.getString(nameIndex)
            returnCursor.close()
        }

        return name
    }

    private fun uploadFileToLocalAppDir(fileUri: Uri, fileName: String) {
        val parcelFileDescriptor = applicationContext.contentResolver.openFileDescriptor(fileUri, "r", null)
        val inputStream = FileInputStream(parcelFileDescriptor!!.fileDescriptor)
        val file = File(applicationContext.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        IOUtils.copy(inputStream, outputStream)
    }

    private fun getDriveService(context: Context): Drive {
        GoogleSignIn.getLastSignedInAccount(context).let { googleAccount ->
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = googleAccount!!.account!!
            return Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                JacksonFactory.getDefaultInstance(),
                credential
            )
                .setApplicationName(getString(R.string.app_name))
                .build()
        }
        var tempDrive: Drive
        return tempDrive
    }

    fun uploadFileToGoogleDrive(context: Context, fileName: String) {
        mDrive.let { googleDriveService ->
            lifecycleScope.launch {
                try {

                    val absolutePath = context!!.getFileStreamPath(fileName).absolutePath
                    val jpegFile = File(absolutePath)
                    val gfile = com.google.api.services.drive.model.File()
                    gfile.name = "examplepic"
                    val mimetype = "image/jpeg"
                    val fileContent = FileContent(mimetype, jpegFile)
                    ///var fileid = ""

                    withContext(Dispatchers.Main) {
                        withContext(Dispatchers.IO) {
                            launch {
                                var mFile = googleDriveService.Files().create(gfile, fileContent).execute()
                            }
                        }
                    }

                } catch (userAuthEx: UserRecoverableAuthIOException) {
                    startActivity(
                        userAuthEx.intent
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.d(TAG, "Error by file(s) upload. ", e)
                    Toast.makeText(context, "Error by file(s) upload. $e",Toast.LENGTH_LONG).show()
                }
            }
        }

    }


}