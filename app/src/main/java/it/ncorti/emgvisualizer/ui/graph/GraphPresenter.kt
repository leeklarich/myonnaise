package it.ncorti.emgvisualizer.ui.graph

import android.content.ContentValues.TAG
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ncorti.myonnaise.CommandList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.ncorti.emgvisualizer.MyoApplication
import it.ncorti.emgvisualizer.MyoApplication_MembersInjector
import it.ncorti.emgvisualizer.dagger.DeviceManager
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger


class GraphPresenter(
        override val view: GraphContract.View,
        private val deviceManager: DeviceManager
) : GraphContract.Presenter(view) {

    private var dataSubscription: Disposable? = null
    private val counter: AtomicInteger = AtomicInteger()
    private val buffer: ArrayList<FloatArray> = arrayListOf()


    override fun create() {

    }

    override fun start() {
        view.showCollectedPoints(counter.get())

    }

    override fun stop() {
        view.startGraph(false)
        dataSubscription?.dispose()
    }

    /*
    Upload CSV to firestore
    Upload a session for this user to firebase
    Increase session count by 1
    Clear buffer
    Clear counter
     */
    fun onSavePressed() {
        Log.i(TAG, "save pressed")
        view.saveCsvFile(createCsv(buffer))
    }

    /*
    Clear buffer
    Clear counter
    Call Start
     */
    fun onRepeatPressed() {
        view.disableRepeatButton()
        view.enableStartButton()
        view.enableSaveButton()
        Log.i(TAG, "repeat pressed")
        counter.set(0)
        buffer.clear()
        view.showCollectedPoints(0)
        dataSubscription?.dispose()
        deviceManager.myo?.apply {
            this.sendCommand(CommandList.stopStreaming())
        }
        Log.i(TAG, "about to start")
        Log.i(TAG, "started")

    }

    /*
    Start streaming myo data
    Start recording in buffer for csv
    Start counter, and be ready to stop counter when it hits time
     */
    fun onStartPressed() {
        var counterVal = 1
        var takeCount = 8000
        Log.i(TAG, "start pressed")
        view.disableStartButton()
        view.enableRepeatButton()
        view.disableSaveButton()
        deviceManager.myo?.apply {
            if (!this.isStreaming()) {
                Log.i(TAG, "start streaming")
                this.sendCommand(CommandList.emgFilteredOnly())
            } else {
                this.sendCommand(CommandList.stopStreaming())
            }
            view.hideNoStreamingMessage()
            Log.i(TAG, "1")
            dataSubscription?.apply {
                if (!this.isDisposed) this.dispose()
            }
            Log.i(TAG, "2")
            dataSubscription = this.dataFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        view.startGraph(true)
                        Log.i(TAG, "3")
                    }

                    .take(takeCount.toLong())
                    .doOnComplete {
                        view.setStatusText("Done, press Save to submit trial, press Start to redo.")
                        view.disableRepeatButton()
                        view.enableStartButton()
                        onRepeatPressed()
                    }
                    .subscribe {
                        view.showData(it)
                        buffer.add(it)
                        view.showCollectedPoints(counter.incrementAndGet())
                        view.setStatusText("gucci")
                        counterVal = counter.get()
                        when {
                            counterVal < 200 -> view.setStatusText("Starting task in 5")
                            counterVal < 400 -> view.setStatusText("Starting task in 4")
                            counterVal < 600 -> view.setStatusText("Starting task in 3")
                            counterVal < 800 -> view.setStatusText("Starting task in 2")
                            counterVal < 1000 -> view.setStatusText("Starting task in 1")
                            counterVal >7000 -> view.setStatusText("Task over, remain still for 5 seconds")
                            else -> view.setStatusText("Collecting Data")
                        }
                        Log.i(TAG, "4")
                    }
        }

    }


    @VisibleForTesting
    internal fun createCsv(buffer: ArrayList<FloatArray>): String {
        val stringBuilder = StringBuilder()
        buffer.forEach {
            it.forEach {
                stringBuilder.append(it)
                stringBuilder.append(",")
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }
}