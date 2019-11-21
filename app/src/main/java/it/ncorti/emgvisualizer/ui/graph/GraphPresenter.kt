package it.ncorti.emgvisualizer.ui.graph

import android.content.ContentValues.TAG
import java.util.Calendar
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.ncorti.myonnaise.CommandList
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import it.ncorti.emgvisualizer.dagger.DeviceManager
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicInteger
import android.media.AudioManager
import android.media.ToneGenerator
import java.lang.System.nanoTime


val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
var timeTrial = 3000

class GraphPresenter(
        override val view: GraphContract.View,
        private val deviceManager: DeviceManager
) : GraphContract.Presenter(view) {

    private var dataSubscription: Disposable? = null
    private val counter: AtomicInteger = AtomicInteger()
    private val buffer: ArrayList<FloatArray> = arrayListOf()
    private val timeBuffer: ArrayList<Long> = arrayListOf()


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

        view.saveCsvFile(createCsv(buffer, timeBuffer))
        buffer.clear()
        timeBuffer.clear()
    }

    /*
    Clear buffer
    Clear counter
    Call Start
     */
    fun onStopPressed() {
        view.disableRepeatButton()
        view.enableStartButton()
        view.enableSaveButton()
        counter.set(0)
        view.showCollectedPoints(0)
        dataSubscription?.dispose()
        deviceManager.myo?.apply {
            this.sendCommand(CommandList.stopStreaming())
        }
    }

    /*
    Start streaming myo data
    Start recording in buffer for csv
    Start counter, and be ready to stop counter when it hits time
     */
    fun onStartPressed() {
        var counterVal = 1
        var startTime = System.currentTimeMillis()
        buffer.clear() //clears the csv buffer
        timeBuffer.clear()
        view.disableStartButton()
        view.enableRepeatButton()
        view.disableSaveButton()

        deviceManager.myo?.apply {
            if (!this.isStreaming()) {
                this.sendCommand(CommandList.emgFilteredOnly())
            } /**else {
                this.sendCommand(CommandList.stopStreaming())
            }*/

            view.hideNoStreamingMessage()

            dataSubscription?.apply {
                if (!this.isDisposed) this.dispose()
            }

            dataSubscription = this.dataFlowable()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe {
                        view.startGraph(true)
                        startTime = System.currentTimeMillis()
                    }

                    .doOnComplete {
                        view.setStatusText("Done, press Save to submit trial, press Start to redo.")
                        view.disableRepeatButton()
                        view.enableStartButton()
                        onStopPressed()
                    }
                    .subscribe {
                        view.showData(it)
                        buffer.add(it) //[1, 2, 3, 4, 5, 6, 7, 8] need to add a 103 to the start
                        timeBuffer.add( System.currentTimeMillis() - startTime)
                        view.showCollectedPoints(counter.incrementAndGet())
                        view.setStatusText("")
                        counterVal = counter.get()
                        when {
                            counterVal < 200 -> view.setStatusText("Starting task in 3")
                            counterVal < 400 -> view.setStatusText("Starting task in 2")
                            counterVal < 599 -> view.setStatusText("Starting task in 1")

                            counterVal < 600 -> tg.startTone(ToneGenerator.TONE_PROP_BEEP)
                            counterVal > 1199+(timeTrial) -> onStopPressed()
                            counterVal > 600+(timeTrial) -> view.setStatusText("Task over, remain still for 3 seconds")
                            counterVal > 598+(timeTrial) -> tg.startTone(ToneGenerator.TONE_PROP_BEEP2)
                            else -> view.setStatusText("Collecting Data")
                        }
                    }
        }

    }


    @VisibleForTesting
    internal fun createCsv(buffer: ArrayList<FloatArray>, timeBuffer: ArrayList<Long>): String {
        val stringBuilder = StringBuilder()
        buffer.forEachIndexed { i, it ->
            stringBuilder.append(timeBuffer[i])
            stringBuilder.append(",")
            it.forEach {
                stringBuilder.append(it)
                stringBuilder.append(",")
            }
            stringBuilder.append("\n")
        }
        return stringBuilder.toString()
    }

    fun getTrialTime(): Int {
        return timeTrial
    }

    fun setTrialTime(seconds: Int) {
        timeTrial = seconds
    }

}