package it.ncorti.emgvisualizer.ui.graph

import it.ncorti.emgvisualizer.BasePresenter
import it.ncorti.emgvisualizer.BaseView


interface GraphContract {

    interface View : BaseView {

        fun showData(data: FloatArray)

        fun startGraph(running: Boolean)

        fun hideNoStreamingMessage()

        fun showCollectedPoints(totalPoints: Int)

        fun saveCsvFile(content: String)

        fun showNoStreamingMessage()

        fun enableStartButton()

        fun disableStartButton()

        fun enableSaveButton()

        fun disableSaveButton()

        fun enableRepeatButton()

        fun disableRepeatButton()

        fun setStatusText(text: String)
    }

    abstract class Presenter(override val view: BaseView) : BasePresenter<BaseView>(view)
}