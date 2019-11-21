package it.ncorti.emgvisualizer.ui.control

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import dagger.android.support.AndroidSupportInjection
import it.ncorti.emgvisualizer.BaseFragment
import it.ncorti.emgvisualizer.R

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_control_device.*
import javax.inject.Inject
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ValueEventListener
import it.ncorti.emgvisualizer.ui.MainActivity


class ControlDeviceFragment : BaseFragment<ControlDeviceContract.Presenter>(), ControlDeviceContract.View {
    data class User(
            val name: String = "",
            val comments: String = "",
            val handedness: String = "",
            val injury: String = "",
            val enrollment_date: String = "",
            val sessions: String = "0",
            var uuid: String = "")

    data class Task(
            val name: String = "",
            val description: String = "",
            val hand: String = "",
            val time: String = "",
            var uuid: String = "")

    companion object {
        fun newInstance() = ControlDeviceFragment()
    }

    @Inject
    lateinit var controlDevicePresenter: ControlDevicePresenter

    //private var selectedUser: Int? = 1

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        attachPresenter(controlDevicePresenter)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.layout_control_device, container, false)

        setHasOptionsMenu(true)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var myRef = FirebaseDatabase.getInstance().reference
        var selectedTask = 1
        var selectedUser = 1

        (activity as MainActivity).userCounter = selectedTask
        (activity as MainActivity).taskCounter = selectedUser

        loadUserSelection(myRef)
        loadTaskSelection(myRef)
        button_connect.setOnClickListener { controlDevicePresenter.onConnectionToggleClicked() }
        //lock and unlock the text, on save upload to firebase
        button_modify_subject.setOnClickListener {
            updateSubject(myRef, selectedUser)
            Toast.makeText(context, "Subject Updated", Toast.LENGTH_SHORT).show()
        }
        //lock and unlock the text, on save upload to firebase
        button_modify_task.setOnClickListener {
            updateTask(myRef, selectedTask)
            Toast.makeText(context, "Task Updated", Toast.LENGTH_SHORT).show()
        }
        button_new_subject.setOnClickListener {
            makeNewUser(myRef)
            Toast.makeText(context, "Subject Created", Toast.LENGTH_SHORT).show()
        }
        button_new_task.setOnClickListener {
            makeNewTask(myRef)
            Toast.makeText(context, "Blank task created", Toast.LENGTH_SHORT).show()
        }
        select_name.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(position != 0) {
                    selectedUser = position
                    loadSpecificUser(myRef, position)
                    (activity as MainActivity).userCounter = position
                }
                Log.d("MyoApp onItemSelected", "Selected user: " + position + "\nControlDeviceFrag Focus: " + view_pager?.currentItem)
            }
        }
        select_task.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTask = position
                loadSpecificTask(myRef, position)
                (activity as MainActivity).taskCounter = position

            }
        }

    }

    override fun showDeviceInformation(name: String?, address: String) {
        device_name.text = name ?: getString(R.string.unknown_device)
        device_address.text = address
    }


    override fun showConnected() {
        button_connect.text = getString(R.string.disconnect)
    }

    override fun showConnecting() {
        button_connect.text = "Connecting"
    }

    override fun showDisconnected() {
        button_connect.text = getString(R.string.connect)
    }

    override fun showConnectionError() {
        Toast.makeText(context, "Connection failed", Toast.LENGTH_SHORT).show()
        button_connect.text = getString(R.string.connect)
    }

    override fun enableConnectButton() {
        button_connect.isEnabled = true
    }

    override fun disableConnectButton() {
        button_connect.isEnabled = false
    }

    fun makeNewUser(firebaseData: DatabaseReference) {
        firebaseData.child("subjects").addListenerForSingleValueEvent(object :ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user_counter = dataSnapshot.childrenCount.toInt()
                val availableUsers: List<User> = mutableListOf(
                        User("Subject$user_counter")
                )
                availableUsers.forEach {
                    val key = firebaseData.child("subjects").push().key!!
                    it.uuid = key
                    firebaseData.child("subjects").child(key).setValue(it)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    fun makeNewTask(firebaseData: DatabaseReference) {
        firebaseData.child("tasks").addListenerForSingleValueEvent(object :ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val task_counter = dataSnapshot.childrenCount.toInt()
                val availableTasks: List<Task> = mutableListOf(
                        Task("Task$task_counter")
                )
                availableTasks.forEach {
                    val key = firebaseData.child("tasks").push().key!!
                    it.uuid = key
                    firebaseData.child("tasks").child(key).setValue(it)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }



    private fun loadUserSelection(firebaseData: DatabaseReference, counter: Int=0) {
        firebaseData.child("subjects").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Is better to use a List, because you don't know the size
                // of the iterator returned by dataSnapshot.getChildren() to
                // initialize the array
                val areas = ArrayList<String>()
                var counter = counter
                for (areaSnapshot in dataSnapshot.children) {
                    val areaName = areaSnapshot.child("name").getValue(String::class.java)
                    areas.add(areaName!!)
                    if(counter == 0) {
                        edit_date.setText(areaSnapshot.child("enrollment_date").getValue(String::class.java))
                        edit_handedness.setText(areaSnapshot.child("handedness").getValue(String::class.java))
                        edit_injury.setText(areaSnapshot.child("injury").getValue(String::class.java))
                        edit_comments.setText(areaSnapshot.child("comments").getValue(String::class.java))
                        sessions_uploaded_number.text = areaSnapshot.child("sessions").getValue(String::class.java)

                        counter++
                    }
                }

                areas.sortWith(compareBy { it.removePrefix("Subject").toInt() })
                val areaSpinner = select_name
                val areasAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, areas)
                areasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                areaSpinner.adapter = areasAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun loadTaskSelection(firebaseData: DatabaseReference, counter: Int=0) {
        firebaseData.child("tasks").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Is better to use a List, because you don't know the size
                // of the iterator returned by dataSnapshot.getChildren() to
                // initialize the array
                val areas = ArrayList<String>()
                var counter = counter
                for (areaSnapshot in dataSnapshot.children) {
                    val areaName = areaSnapshot.child("name").getValue(String::class.java)
                    areas.add(areaName!!)
                    if(counter == 0) {
                        edit_task_name.setText(areaSnapshot.child("name").getValue(String::class.java))
                        edit_task_hand.setText(areaSnapshot.child("hand").getValue(String::class.java))
                        edit_task_description.setText(areaSnapshot.child("description").getValue(String::class.java))
                        edit_time.setText(areaSnapshot.child("time").getValue(String::class.java))

                        counter++
                    }
                }

                val areaSpinner = select_task
                val areasAdapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, areas)
                areasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                areaSpinner.adapter = areasAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })
    }

    private fun loadSpecificTask(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("tasks").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        edit_task_name.setText(areaSnapshot.child("name").getValue(String::class.java))
                        edit_task_hand.setText(areaSnapshot.child("hand").getValue(String::class.java))
                        edit_task_description.setText(areaSnapshot.child("description").getValue(String::class.java))
                        edit_time.setText(areaSnapshot.child("time").getValue(String::class.java))
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun loadSpecificUser(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("subjects").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        edit_date.setText(areaSnapshot.child("enrollment_date").getValue(String::class.java))
                        edit_handedness.setText(areaSnapshot.child("handedness").getValue(String::class.java))
                        edit_injury.setText(areaSnapshot.child("injury").getValue(String::class.java))
                        edit_comments.setText(areaSnapshot.child("comments").getValue(String::class.java))
                        sessions_uploaded_number.text = areaSnapshot.child("sessions").getValue(String::class.java)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun updateSubject(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("subjects").addListenerForSingleValueEvent(object : ValueEventListener {
            var uuid = ""
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        uuid = areaSnapshot.child("uuid").getValue(String::class.java)!!
                    }
                }
                firebaseData.child("subjects").child(uuid).child("enrollment_date").setValue(edit_date.text.toString())
                firebaseData.child("subjects").child(uuid).child("handedness").setValue(edit_handedness.text.toString())
                firebaseData.child("subjects").child(uuid).child("injury").setValue(edit_injury.text.toString())
                firebaseData.child("subjects").child(uuid).child("comments").setValue(edit_comments.text.toString())

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

    private fun updateTask(firebaseData: DatabaseReference, index: Int) {
        firebaseData.child("tasks").addListenerForSingleValueEvent(object : ValueEventListener {
            var uuid = ""
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for ((counter, areaSnapshot) in dataSnapshot.children.withIndex()) {
                    if(counter == index) {
                        uuid = areaSnapshot.child("uuid").getValue(String::class.java)!!
                    }
                }
                firebaseData.child("tasks").child(uuid).child("name").setValue(edit_task_name.text.toString())
                firebaseData.child("tasks").child(uuid).child("hand").setValue(edit_task_hand.text.toString())
                firebaseData.child("tasks").child(uuid).child("description").setValue(edit_task_description.text.toString())
                firebaseData.child("tasks").child(uuid).child("time").setValue(edit_time.text.toString())

            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })

    }

}