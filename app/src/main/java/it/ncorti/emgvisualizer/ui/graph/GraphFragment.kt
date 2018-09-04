package it.ncorti.emgvisualizer.ui.graph

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.ncorti.myonnaise.MYO_CHANNELS
import com.ncorti.myonnaise.MYO_MAX_VALUE
import com.ncorti.myonnaise.MYO_MIN_VALUE
import dagger.android.support.AndroidSupportInjection
import it.ncorti.emgvisualizer.BaseFragment
import it.ncorti.emgvisualizer.R

import kotlinx.android.synthetic.main.layout_graph.*
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.storage.UploadTask
import com.google.android.gms.tasks.OnSuccessListener
import it.ncorti.emgvisualizer.R.id.status_text
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

import it.ncorti.emgvisualizer.ui.MainActivity

import kotlinx.android.synthetic.main.layout_control_device.*
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


private const val REQUEST_WRITE_EXTERNAL_CODE = 2

class GraphFragment : BaseFragment<GraphContract.Presenter>(), GraphContract.View {



    companion object {
        fun newInstance() = GraphFragment()
    }

    @Inject
    lateinit var graphPresenter: GraphPresenter


    private var fileContentToSave: String? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        attachPresenter(graphPresenter)
        super.onAttach(context)

    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.layout_graph, container, false)
        setHasOptionsMenu(true)

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var myRef = FirebaseDatabase.getInstance().reference
        sensor_graph_view1.channels = MYO_CHANNELS
        sensor_graph_view1.maxValue = MYO_MAX_VALUE*8
        sensor_graph_view1.minValue = MYO_MIN_VALUE*8
        var selectedTask = (activity as MainActivity).taskCounter
        var selectedUser = (activity as MainActivity).userCounter
        stopButton.setOnClickListener { graphPresenter.onStopPressed() }
        save.setOnClickListener { graphPresenter.onSavePressed() }
        start.setOnClickListener { graphPresenter.onStartPressed()
            getTimeFromFirebaseIndex(myRef, (activity as MainActivity).taskCounter)
            getUserFromFirebaseIndex(myRef, (activity as MainActivity).userCounter)}


        val mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            //first use
            mAuth.signInAnonymously().addOnCompleteListener {
            }
        }
        getTimeFromFirebaseIndex(myRef, selectedTask)
        getUserFromFirebaseIndex(myRef, selectedUser)

    }

    private fun  getTimeFromFirebaseIndex(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("tasks").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        timeFirebase.text= (areaSnapshot.child("time").getValue(String::class.java))
                        graphPresenter.setTrialTime(timeFirebase.text.toString().toInt()*200)
                        taskNameFirebase.text = (areaSnapshot.child("name").getValue(String::class.java))
                        taskHandFirebase.text = (areaSnapshot.child("hand").getValue(String::class.java))
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun  getUserFromFirebaseIndex(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("subjects").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        userNameFirebase.text= (areaSnapshot.child("name").getValue(String::class.java))
                        uuidFirebase.text= (areaSnapshot.child("uuid").getValue(String::class.java))
                        sessionsFirebase.text= (areaSnapshot.child("sessions").getValue(String::class.java))
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }
    override fun enableStartButton() {
        start.isEnabled = true
    }

    override fun disableStartButton() {
        start.isEnabled = false
    }

    override fun enableSaveButton() {
        save.isEnabled = true
    }

    override fun disableSaveButton() {
        save.isEnabled = false
    }

    override fun enableRepeatButton() {
        stopButton.isEnabled = true
    }

    override fun disableRepeatButton() {
        stopButton.isEnabled = false
    }

    override fun showData(data: FloatArray) {
        sensor_graph_view1?.addPoint(data)
    }

    override fun startGraph(running: Boolean) {
        sensor_graph_view1?.apply {
            this.running = running
        }
    }

    override fun showNoStreamingMessage() {
        text_empty_graph.visibility = View.VISIBLE
    }

    override fun hideNoStreamingMessage() {
        text_empty_graph.visibility = View.INVISIBLE
    }

    override fun saveCsvFile(content: String) {
        context?.apply {
            val hasPermission = (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
            if (hasPermission) {
                writeToFile(content)
            } else {
                fileContentToSave = content
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        REQUEST_WRITE_EXTERNAL_CODE)
            }
        }
    }

    override fun showCollectedPoints(totalPoints: Int) {
        when {
            totalPoints < 600 -> points_count.text = "      " +
                    ((graphPresenter.getTrialTime()) / 200).toString() + " seconds"
            totalPoints < 600+graphPresenter.getTrialTime() ->
                points_count.text = "      " + ((graphPresenter.getTrialTime()+600 - totalPoints) / 200).toString() + " seconds"
            else -> points_count.text = ""
        }
    }

    override fun setStatusText(text: String) {
        status_text.text = text
    }

    private fun writeToFile(content: String) {
        val storageDir =
                File("${Environment.getExternalStorageDirectory().absolutePath}/myo_emg")
        storageDir.mkdir()


        val df = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SS")
        val date = df.format(Calendar.getInstance().time)
        val fileName = userNameFirebase.text.toString() + "_" + taskNameFirebase.text + "_" + taskHandFirebase.text + date + ".csv"
        val outfile = File(storageDir, fileName)

        val fileOutputStream = FileOutputStream(outfile)
        fileOutputStream.write(content.toByteArray())
        fileOutputStream.close()

        val mStorage: StorageReference = FirebaseStorage.getInstance().getReference("Uploads")
        val file = Uri.fromFile(outfile)
        val csvRef = mStorage.child(fileName)

        csvRef.putFile(file)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Saved to: ${outfile.path} and uploaded", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener {

                    Toast.makeText(activity, "Saved to: ${outfile.path}, but not uploaded", Toast.LENGTH_LONG).show()
                }
        var myRef = FirebaseDatabase.getInstance().reference

        myRef.child("subjects").child(uuidFirebase.text.toString()).child("sessions").setValue(
                (sessionsFirebase.text.toString().toInt() + 1).toString())

    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_WRITE_EXTERNAL_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fileContentToSave?.apply { writeToFile(this) }
                } else {
                    Toast.makeText(activity, getString(R.string.write_permission_denied_message), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}