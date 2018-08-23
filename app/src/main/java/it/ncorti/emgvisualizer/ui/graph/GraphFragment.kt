package it.ncorti.emgvisualizer.ui.graph

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
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

        sensor_graph_view.channels = MYO_CHANNELS
        sensor_graph_view.maxValue = MYO_MAX_VALUE
        sensor_graph_view.minValue = MYO_MIN_VALUE

        repeat.setOnClickListener { graphPresenter.onRepeatPressed() }
        save.setOnClickListener { graphPresenter.onSavePressed() }
        start.setOnClickListener { graphPresenter.onStartPressed() }
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
        repeat.isEnabled = true
    }

    override fun disableRepeatButton() {
        repeat.isEnabled = false
    }

    override fun showData(data: FloatArray) {
        sensor_graph_view?.addPoint(data)
    }

    override fun startGraph(running: Boolean) {
        sensor_graph_view?.apply {
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
        points_count.text = "      " + ((8000 - totalPoints)/200).toString() + " seconds"
    }

    override fun setStatusText(text: String) {
        status_text.text = text
    }

    private fun writeToFile(content: String) {

        val storageDir =
                File("${Environment.getExternalStorageDirectory().absolutePath}/myo_emg")
        storageDir.mkdir()
        val outfile = File(storageDir, "myo_emg_export_${System.currentTimeMillis()}.csv")
        val fileOutputStream = FileOutputStream(outfile)
        fileOutputStream.write(content.toByteArray())
        fileOutputStream.close()
        Toast.makeText(activity, "Saved to: ${outfile.path}", Toast.LENGTH_LONG).show()
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