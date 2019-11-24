/*
 This is free and unencumbered software released into the public domain.
 See LICENSE file for details.
 */

package re.indigo.epistolaire

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/*
Róbert Kiszeli https://www.youtube.com/watch?v=ZALMdNgx9bw
https://stackoverflow.com/questions/3012287/how-to-read-mms-data-in-android
 */

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val wtf = 2
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), wtf)
        }

        saveMessages()
    }

    fun saveMessages() {
        //val dumper = SmsDumper(contentResolver)
        val dumper = MmsDumper(contentResolver)
        val jobj = dumper.getJson()

        val myExternalFile = File(getExternalFilesDir("."), "backup.json")
        Log.e("TAG", "plop ${myExternalFile}")
        try {
            val fileOutPutStream = FileOutputStream(myExternalFile)

            try {
                fileOutPutStream.write(jobj.toString().toByteArray())
            } finally {
                fileOutPutStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        Toast.makeText(applicationContext,"Backup done to ${myExternalFile}", Toast.LENGTH_LONG).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
